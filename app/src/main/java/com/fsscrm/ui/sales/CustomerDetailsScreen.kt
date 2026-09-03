package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fsscrm.network.Customer
import com.fsscrm.network.CustomerLicense
import com.fsscrm.network.Product
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

fun isMainTallyName(name: String?): Boolean {
    val n = name?.trim()?.lowercase() ?: return false
    if (n.contains("cloud")) return false
    if (Regex("""\b(amc|tss|tdl|whatsapp|upgrade|service|addon|support|biz\s*analyst)\b""")
            .containsMatchIn(n)
    ) return false
    val hasTally = Regex("""\btally\b""").containsMatchIn(n)
    val hasTier = Regex("""(gold|silver|server|prime|erp\s*9|tallyprime)""").containsMatchIn(n)
    return hasTally && hasTier
}

fun tallyTierRank(name: String?): Int {
    val n = name?.trim()?.lowercase() ?: return 0
    return when {
        n.contains("server") || n.contains("prime") -> 3
        n.contains("gold") -> 2
        n.contains("silver") -> 1
        else -> 0
    }
}

fun filterUpgradeOptions(currentMainName: String?, allProducts: List<Product>): List<Product> {
    val currentRank = tallyTierRank(currentMainName)
    if (currentRank <= 0) return emptyList()
    return allProducts.filter { prod ->
        isMainTallyName(prod.name) && tallyTierRank(prod.name) > currentRank
    }
}

fun filterServiceOptions(allProducts: List<Product>): List<Product> {
    return allProducts.filter { !isMainTallyName(it.name) }
}

fun groupLicensesByKey(licenses: List<CustomerLicense>): Map<String, List<CustomerLicense>> {
    return licenses.groupBy { lic ->
        lic.license_key?.trim().orEmpty().ifEmpty { "(no key)" }
    }
}

fun JsonObject.arrayOrEmpty(vararg keys: String): List<JsonObject> {
    fun from(obj: JsonObject): List<JsonObject>? {
        for (k in keys) {
            val el = obj.get(k) ?: continue
            if (el.isJsonArray) {
                return el.asJsonArray.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }
            }
        }
        return null
    }
    from(this)?.let { return it }
    if (has("data") && get("data").isJsonObject) {
        from(getAsJsonObject("data"))?.let { return it }
    }
    return emptyList()
}

fun jsonStr(o: JsonObject, vararg keys: String): String {
    for (k in keys) {
        val e = o.get(k) ?: continue
        if (!e.isJsonNull) {
            return try {
                e.asString.trim()
            } catch (_: Exception) {
                try {
                    e.asInt.toString()
                } catch (_: Exception) {
                    ""
                }
            }
        }
    }
    return ""
}

fun jsonMoney(o: JsonObject, vararg keys: String): String {
    for (k in keys) {
        val e = o.get(k) ?: continue
        if (e.isJsonNull) continue
        return try {
            "₹${"%.2f".format(e.asDouble)}"
        } catch (_: Exception) {
            try {
                e.asString
            } catch (_: Exception) {
                ""
            }
        }
    }
    return ""
}

fun matchesLicenceKey(o: JsonObject, licenseKey: String): Boolean {
    if (licenseKey.isBlank()) return false
    val keys = listOf("license_key", "licence_key", "serial_number", "serial", "license_number")
    return keys.any { jsonStr(o, it).equals(licenseKey, ignoreCase = true) }
}

fun matchesServiceName(o: JsonObject, serviceName: String): Boolean {
    if (serviceName.isBlank()) return true
    val hay = listOf(
        "service", "item_name", "product_name", "work_name",
        "requirement", "description", "name", "lead_service", "lead_name"
    ).joinToString(" ") { jsonStr(o, it) }.lowercase()
    return hay.contains(serviceName.trim().lowercase())
}

data class LicenseHistoryItem(
    val id: Int = 0,
    val title: String,
    val subtitle: String = "",
    val date: String = "",
    val amount: String = "",
    val status: String = ""
)

/** Fallback parser for customer list API */
data class CustomersListResponse(val data: List<Customer>? = null)

fun parseCustomersListResponse(body: JsonElement): CustomersListResponse? {
    return try {
        val gson = Gson()
        when {
            body.isJsonObject -> {
                val obj = body.asJsonObject
                val arr = when {
                    obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                    obj.has("customers") && obj.get("customers").isJsonArray ->
                        obj.getAsJsonArray("customers")
                    else -> null
                }
                if (arr != null) {
                    val type = object : TypeToken<List<Customer>>() {}.type
                    CustomersListResponse(gson.fromJson(arr, type))
                } else null
            }
            body.isJsonArray -> {
                val type = object : TypeToken<List<Customer>>() {}.type
                CustomersListResponse(gson.fromJson(body, type))
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    userId: Int,
    customerId: Int,
    navController: NavController,
    onBack: () -> Unit
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    var licenses by remember { mutableStateOf<List<CustomerLicense>>(emptyList()) }
    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedLicenseForDetail by remember { mutableStateOf<CustomerLicense?>(null) }
    var selectedService by remember { mutableStateOf<CustomerLicense?>(null) }
    var isCreatingLead by remember { mutableStateOf(false) }
    var opportunityMode by remember { mutableStateOf("Service") }

    var licenceTab by remember { mutableIntStateOf(0) }
    var serviceTab by remember { mutableIntStateOf(0) }

    var licenceLeads by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var licenceProformas by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var licencePayments by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var licenceActivity by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var serviceProformas by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var servicePayments by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var serviceActivity by remember { mutableStateOf<List<LicenseHistoryItem>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }

    var allWorks by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var allPayments by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var allLeads by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var allProformas by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var allLeadHistory by remember { mutableStateOf<List<JsonObject>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val gson = remember { Gson() }

    fun parseCustomerFromJson(root: JsonObject): Customer? {
        return try {
            val dataObj = when {
                root.has("data") && root.get("data").isJsonObject -> root.getAsJsonObject("data")
                else -> root
            }
            val customerJson = when {
                dataObj.has("customer") && dataObj.get("customer").isJsonObject ->
                    dataObj.getAsJsonObject("customer")
                dataObj.has("id") -> dataObj
                else -> dataObj
            }
            gson.fromJson(customerJson, Customer::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun parseLicensesFromAny(root: JsonElement): List<CustomerLicense> {
        return try {
            val type = object : TypeToken<List<CustomerLicense>>() {}.type
            when {
                root.isJsonArray -> gson.fromJson(root, type)
                root.isJsonObject -> {
                    val obj = root.asJsonObject
                    val data = if (obj.has("data") && obj.get("data").isJsonObject)
                        obj.getAsJsonObject("data") else obj
                    val arr = when {
                        data.has("licenses") && data.get("licenses").isJsonArray ->
                            data.getAsJsonArray("licenses")
                        data.has("customer_licenses") && data.get("customer_licenses").isJsonArray ->
                            data.getAsJsonArray("customer_licenses")
                        data.has("active_licenses") && data.get("active_licenses").isJsonArray ->
                            data.getAsJsonArray("active_licenses")
                        obj.has("licenses") && obj.get("licenses").isJsonArray ->
                            obj.getAsJsonArray("licenses")
                        data.has("customer") && data.get("customer").isJsonObject -> {
                            val c = data.getAsJsonObject("customer")
                            when {
                                c.has("licenses") && c.get("licenses").isJsonArray ->
                                    c.getAsJsonArray("licenses")
                                c.has("customer_licenses") && c.get("customer_licenses").isJsonArray ->
                                    c.getAsJsonArray("customer_licenses")
                                else -> null
                            }
                        }
                        else -> null
                    }
                    if (arr != null) gson.fromJson(arr, type) else emptyList()
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseProducts(body: JsonElement): List<Product> {
        return try {
            val type = object : TypeToken<List<Product>>() {}.type
            when {
                body.isJsonArray -> gson.fromJson(body, type)
                body.isJsonObject -> {
                    val obj = body.asJsonObject
                    when {
                        obj.has("products") && obj.get("products").isJsonArray ->
                            gson.fromJson(obj.getAsJsonArray("products"), type)
                        obj.has("data") && obj.get("data").isJsonArray ->
                            gson.fromJson(obj.getAsJsonArray("data"), type)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun errorBodyMessage(response: retrofit2.Response<*>): String {
        return try {
            val raw = response.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return "HTTP ${response.code()}"
            val obj = gson.fromJson(raw, JsonObject::class.java)
            when {
                obj.has("message") && !obj.get("message").isJsonNull ->
                    obj.get("message").asString
                else -> raw.take(200)
            }
        } catch (_: Exception) {
            "HTTP ${response.code()}"
        }
    }

    fun storeRelatedFromCustomerJson(obj: JsonObject) {
        val data = when {
            obj.has("data") && obj.get("data").isJsonObject -> obj.getAsJsonObject("data")
            else -> obj
        }
        allWorks = data.arrayOrEmpty("works", "customer_works")
        allPayments = data.arrayOrEmpty("payments", "customer_payments")
        allLeads = data.arrayOrEmpty("leads", "related_leads")
        allProformas = data.arrayOrEmpty("proformas", "proforma_invoices", "invoices")
        allLeadHistory = data.arrayOrEmpty("history", "lead_history", "activity", "activities")

        android.util.Log.d(
            "CustomerDetails",
            "RAW works=${allWorks.size} pay=${allPayments.size} leads=${allLeads.size} " +
                    "pf=${allProformas.size} hist=${allLeadHistory.size}"
        )
        allPayments.take(3).forEachIndexed { idx, p ->
            val keys = p.entrySet().joinToString(", ") { (k, v) ->
                "$k=${v.toString().take(40)}"
            }
            android.util.Log.d("CustomerDetails", "PAY[$idx] $keys")
        }
    }

    fun loadLicenceHistory(licenseKey: String, serviceName: String? = null) {
        android.util.Log.d("CustomerDetails", "Loading history key=$licenseKey service=$serviceName")
        historyLoading = true
        scope.launch {
            try {
                try {
                    val detailBody = mapOf(
                        "user_id" to userId,
                        "customer_id" to customerId,
                        "action" to "get_customer",
                        "include_licenses" to true,
                        "include_works" to true,
                        "include_payments" to true
                    )
                    val resp = RetrofitClient.apiService.getCustomerDetails(detailBody)
                    if (resp.isSuccessful) {
                        resp.toLenientJson()?.let { json ->
                            if (json.isJsonObject) {
                                storeRelatedFromCustomerJson(json.asJsonObject)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerDetails", "detail history fetch", e)
                }

                val filterSvc = !serviceName.isNullOrBlank()
                val svc = serviceName?.trim().orEmpty()
                val cid = customerId.toString()

                fun underCustomer(o: JsonObject): Boolean {
                    val c = jsonStr(o, "customer_id")
                    return c.isEmpty() || c == cid || c == "0"
                }

                val worksForService = if (filterSvc) {
                    allWorks.filter { underCustomer(it) && matchesServiceName(it, svc) }
                } else {
                    allWorks.filter { underCustomer(it) }
                }

                val workIds = worksForService.mapNotNull {
                    it.get("id")?.takeIf { x -> !x.isJsonNull }?.asInt
                }.toSet()

                val leadIds = (
                        worksForService.mapNotNull {
                            it.get("lead_id")?.takeIf { x -> !x.isJsonNull }?.asInt
                        } + allLeads.filter {
                            underCustomer(it) && (!filterSvc || matchesServiceName(it, svc))
                        }.mapNotNull {
                            it.get("id")?.takeIf { x -> !x.isJsonNull }?.asInt
                        }
                        ).toSet()

                val paymentsFiltered = allPayments.filter { p ->
                    if (!underCustomer(p) && !matchesLicenceKey(p, licenseKey)) return@filter false
                    if (!filterSvc) return@filter true
                    val wid = p.get("work_id")?.takeIf { !it.isJsonNull }?.asInt
                    val lid = p.get("lead_id")?.takeIf { !it.isJsonNull }?.asInt
                    when {
                        wid != null && workIds.contains(wid) -> true
                        lid != null && leadIds.contains(lid) -> true
                        wid == null && lid == null -> true
                        matchesServiceName(p, svc) -> true
                        else -> false
                    }
                }

                val payMapped = paymentsFiltered.mapIndexed { i, o ->
                    LicenseHistoryItem(
                        id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: (2000 + i),
                        title = jsonStr(o, "payment_reference", "reference_no", "transaction_id")
                            .ifBlank { "Payment" },
                        subtitle = listOf(
                            jsonStr(o, "payment_method", "method"),
                            jsonStr(o, "status")
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        date = jsonStr(o, "payment_date", "created_at"),
                        amount = jsonMoney(o, "amount"),
                        status = jsonStr(o, "status").ifBlank { "completed" }
                    )
                }

                val pfFiltered = allProformas.filter { pf ->
                    if (!underCustomer(pf) && !matchesLicenceKey(pf, licenseKey)) return@filter false
                    if (!filterSvc) return@filter true
                    val lid = pf.get("lead_id")?.takeIf { !it.isJsonNull }?.asInt
                    when {
                        lid != null && leadIds.contains(lid) -> true
                        matchesServiceName(pf, svc) -> true
                        jsonStr(pf, "service", "item_name", "description").isBlank() -> true
                        else -> false
                    }
                }

                val pfMapped = pfFiltered.mapIndexed { i, o ->
                    LicenseHistoryItem(
                        id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: (1000 + i),
                        title = jsonStr(o, "proforma_no", "invoice_no", "quote_no")
                            .ifBlank { "Proforma #${o.get("id")?.asInt ?: i}" },
                        subtitle = jsonStr(o, "status"),
                        date = jsonStr(o, "created_at", "proforma_date", "date"),
                        amount = jsonMoney(o, "total", "amount"),
                        status = jsonStr(o, "status")
                    )
                }

                val leadsMapped = allLeads
                    .filter { matchesLicenceKey(it, licenseKey) || underCustomer(it) }
                    .filter { if (filterSvc) matchesServiceName(it, svc) else true }
                    .mapIndexed { i, o ->
                        val lic = jsonStr(o, "serial_number", "license_key")
                        val rawSvc = jsonStr(o, "service")
                        val formattedSvc = if (lic.isNotBlank() && rawSvc.isNotBlank() && !rawSvc.contains(lic, ignoreCase = true)) {
                            "$lic : $rawSvc"
                        } else {
                            rawSvc
                        }
                        LicenseHistoryItem(
                            id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: i,
                            title = jsonStr(o, "name").ifBlank {
                                "Lead #${o.get("id")?.asInt ?: i}"
                            },
                            subtitle = listOf(formattedSvc, jsonStr(o, "status"))
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            date = jsonStr(o, "created_at", "date"),
                            status = jsonStr(o, "status")
                        )
                    }

                val worksActivity = worksForService.mapIndexed { i, o ->
                    LicenseHistoryItem(
                        id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: (3000 + i),
                        title = jsonStr(o, "work_name", "name", "lead_name").ifBlank { "Work" },
                        subtitle = listOf(
                            jsonStr(o, "lead_service", "service"),
                            jsonStr(o, "status")
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        date = jsonStr(o, "created_at", "actual_completion", "start_date"),
                        amount = jsonMoney(o, "total_amount", "amount_received"),
                        status = jsonStr(o, "status")
                    )
                }

                val histActivity = allLeadHistory.filter { h ->
                    if (!underCustomer(h) && !matchesLicenceKey(h, licenseKey)) return@filter false
                    if (!filterSvc) return@filter true
                    val lid = h.get("lead_id")?.takeIf { !it.isJsonNull }?.asInt
                    lid == null || leadIds.contains(lid) || matchesServiceName(h, svc)
                }.mapIndexed { i, o ->
                    LicenseHistoryItem(
                        id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: (4000 + i),
                        title = jsonStr(o, "action", "status_to", "remarks").ifBlank { "Activity" },
                        subtitle = jsonStr(o, "remarks", "status_from"),
                        date = jsonStr(o, "created_at"),
                        status = jsonStr(o, "status_to", "action")
                    )
                }

                val activityMapped = (histActivity + worksActivity)
                    .sortedByDescending { it.date }

                android.util.Log.d(
                    "CustomerDetails",
                    "MAPPED svc=$svc leads=${leadsMapped.size} pf=${pfMapped.size} " +
                            "pay=${payMapped.size} act=${activityMapped.size} worksSvc=${worksForService.size}"
                )

                if (filterSvc) {
                    serviceProformas = pfMapped
                    servicePayments = payMapped
                    serviceActivity = activityMapped
                } else {
                    licenceLeads = leadsMapped
                    licenceProformas = pfMapped
                    licencePayments = payMapped
                    licenceActivity = activityMapped
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerDetails", "loadLicenceHistory", e)
                try {
                    snackbarHostState.showSnackbar("History error: ${e.message}")
                } catch (_: Exception) { }
            } finally {
                historyLoading = false
            }
        }
    }

    fun createLeadForOpportunity(product: Product, licenseKey: String, mode: String) {
        val cust = customer ?: return
        if (licenseKey.isBlank() || licenseKey == "(no key)") {
            scope.launch {
                snackbarHostState.showSnackbar("No valid licence number to bind this product")
            }
            return
        }
        if (isCreatingLead) return

        val connectionType = if (mode == "Upgrade") "Upgrade" else "Service"
        val currentProduct = selectedLicenseForDetail?.item_name.orEmpty()

        isCreatingLead = true
        scope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "user_id" to userId,
                    "customer_id" to customerId,
                    "name" to cust.name,
                    "email" to (cust.email ?: ""),
                    "phone" to (cust.phone ?: ""),
                    "company" to (cust.company ?: ""),
                    "service" to product.name,
                    "serial_number" to licenseKey,
                    "license_key" to licenseKey,
                    "connection_type" to connectionType,
                    "current_product" to currentProduct,
                    "status" to "New",
                    "source" to "App Opportunity",
                    "message" to "$connectionType under licence $licenseKey: ${product.name}"
                )
                if (product.id > 0) body["product_id"] = product.id

                val response = RetrofitClient.apiService.createLead(
                    body.mapValues { it.value.toString() }
                )

                if (response.isSuccessful && response.body() != null) {
                    val root = response.body()!!
                    if (root.isJsonObject) {
                        val obj = root.asJsonObject
                        val status = obj.get("status")?.asString
                        if (status == "success" || status == "ok" || obj.has("lead_id") || obj.has("id")) {
                            val newLeadId = when {
                                obj.has("lead_id") && !obj.get("lead_id").isJsonNull ->
                                    obj.get("lead_id").asInt
                                obj.has("id") && !obj.get("id").isJsonNull ->
                                    obj.get("id").asInt
                                obj.has("data") && obj.get("data").isJsonObject -> {
                                    val d = obj.getAsJsonObject("data")
                                    when {
                                        d.has("lead_id") -> d.get("lead_id").asInt
                                        d.has("id") -> d.get("id").asInt
                                        else -> 0
                                    }
                                }
                                else -> 0
                            }
                            snackbarHostState.showSnackbar(
                                if (newLeadId > 0)
                                    "Lead #$newLeadId created ($connectionType) for ${product.name}"
                                else
                                    "Lead created ($connectionType) for ${product.name}"
                            )
                            if (newLeadId > 0) {
                                navController.navigate("lead_details/$newLeadId")
                            }
                        } else {
                            snackbarHostState.showSnackbar(
                                obj.get("message")?.asString ?: "Failed to create lead"
                            )
                        }
                    } else {
                        snackbarHostState.showSnackbar("Lead created")
                    }
                } else {
                    snackbarHostState.showSnackbar(errorBodyMessage(response))
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message ?: "Could not create lead"}")
            } finally {
                isCreatingLead = false
            }
        }
    }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                var loadedLicenses = false
                val detailBody = mapOf(
                    "user_id" to userId,
                    "customer_id" to customerId,
                    "action" to "get_customer",
                    "include_licenses" to true,
                    "include_works" to true,
                    "include_payments" to true
                )
                val custResponse = RetrofitClient.apiService.getCustomerDetails(detailBody)
                if (custResponse.isSuccessful) {
                    custResponse.toLenientJson()?.let { root ->
                        if (root.isJsonObject) {
                            val obj = root.asJsonObject
                            if (obj.get("status")?.asString == "error") {
                                errorMessage = obj.get("message")?.asString ?: "API error"
                            } else {
                                customer = parseCustomerFromJson(obj)
                                val embedded = parseLicensesFromAny(obj)
                                if (embedded.isNotEmpty()) {
                                    licenses = embedded
                                    loadedLicenses = true
                                }
                                storeRelatedFromCustomerJson(obj)
                            }
                        }
                    }
                } else {
                    errorMessage = errorBodyMessage(custResponse)
                }

                if (customer == null) {
                    val listBody = mapOf(
                        "user_id" to userId,
                        "action" to "list",
                        "search" to customerId.toString(),
                        "limit" to 50,
                        "page" to 1
                    )
                    val listResp = RetrofitClient.apiService.getCustomers(
                        listBody.mapValues { it.value.toString() }
                    )
                    if (listResp.isSuccessful) {
                        listResp.toLenientJson()?.let { json ->
                            val parsed = parseCustomersListResponse(json)
                            val match = parsed?.data?.firstOrNull { it.id == customerId }
                            if (match != null) {
                                customer = match
                                errorMessage = null
                            }
                        }
                    }
                }

                if (!loadedLicenses) {
                    val licParams = mutableMapOf<String, Any>(
                        "user_id" to userId,
                        "customer_id" to customerId,
                        "include_expired" to true,
                        "include_inactive" to true,
                        "include_works" to true
                    )
                    val lid = customer?.lead_id ?: 0
                    if (lid > 0) licParams["lead_id"] = lid
                    val licResponse = RetrofitClient.apiService.getExistingLicenses(licParams)
                    if (licResponse.isSuccessful) {
                        licResponse.toLenientJson()?.let {
                            licenses = parseLicensesFromAny(it)
                        }
                    }
                }

                val prodResp = RetrofitClient.apiService.getProducts(
                    mapOf("user_id" to userId, "limit" to 500)
                )
                if (prodResp.isSuccessful) {
                    prodResp.toLenientJson()?.let {
                        allProducts = parseProducts(it)
                    }
                }

                if (customer == null && errorMessage == null) {
                    errorMessage = "Customer not found"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                try {
                    snackbarHostState.showSnackbar("Error: ${e.message}")
                } catch (_: Exception) { }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(customerId, userId) { loadData() }

    LaunchedEffect(selectedLicenseForDetail?.id) {
        opportunityMode = "Service"
        licenceTab = 0
        selectedService = null
        selectedLicenseForDetail?.license_key?.trim()?.let { key ->
            if (key.isNotEmpty()) loadLicenceHistory(key)
        }
    }

    LaunchedEffect(selectedService?.id) {
        serviceTab = 0
        val key = selectedLicenseForDetail?.license_key?.trim().orEmpty()
        val svc = selectedService?.item_name
        if (key.isNotEmpty() && !svc.isNullOrBlank()) {
            loadLicenceHistory(key, svc)
        }
    }

    val licensesByKey = remember(licenses) { groupLicensesByKey(licenses) }
    val mainLicenseRows = remember(licensesByKey) {
        licensesByKey.mapNotNull { (key, items) ->
            if (key == "(no key)" && items.none { isMainTallyName(it.item_name) }) {
                return@mapNotNull null
            }
            val main = items.firstOrNull { isMainTallyName(it.item_name) } ?: items.firstOrNull()
            main?.let { key to it }
        }.sortedBy { it.first }
    }

    val topBarTitle = when {
        selectedService != null -> selectedService?.item_name ?: "Service"
        selectedLicenseForDetail != null -> "License Details"
        else -> "Customer Details"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(topBarTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedService != null -> selectedService = null
                            selectedLicenseForDetail != null -> selectedLicenseForDetail = null
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            }

            customer == null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "Customer not found",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { loadData() }) { Text("Retry") }
                    }
                }
            }

            selectedService != null && selectedLicenseForDetail != null -> {
                BackHandler { selectedService = null }
                val key = selectedLicenseForDetail!!.license_key?.trim().orEmpty()
                val svc = selectedService!!

                Column(Modifier.fillMaxSize().padding(padding)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                svc.item_name ?: "Service",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Under licence $key",
                                fontSize = 13.sp,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Medium
                            )
                            val exp =
                                if (!svc.expiry_date.isNullOrBlank() && svc.expiry_date != "0000-00-00")
                                    "Exp: ${svc.expiry_date}" else "No expiry set"
                            Text(exp, fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    SecondaryTabRow(
                        selectedTabIndex = serviceTab,
                        containerColor = Color.White,
                        contentColor = PrimaryIndigo
                    ) {
                        Tab(
                            selected = serviceTab == 0,
                            onClick = { serviceTab = 0 },
                            text = { Text("Proforma") },
                            icon = { Icon(Icons.Default.Receipt, null, Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = serviceTab == 1,
                            onClick = { serviceTab = 1 },
                            text = { Text("Payments") },
                            icon = { Icon(Icons.Default.Receipt, null, Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = serviceTab == 2,
                            onClick = { serviceTab = 2 },
                            text = { Text("Activity") },
                            icon = { Icon(Icons.Default.History, null, Modifier.size(18.dp)) }
                        )
                    }

                    val list = when (serviceTab) {
                        0 -> serviceProformas
                        1 -> servicePayments
                        else -> serviceActivity
                    }
                    val emptyMsg = when (serviceTab) {
                        0 -> "No proforma invoices for this service yet"
                        1 -> "No payments for this service yet"
                        else -> "No activity recorded for this service yet"
                    }

                    if (historyLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryIndigo)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (list.isEmpty()) {
                                item {
                                    Text(
                                        emptyMsg,
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            } else {
                                items(list, key = { it.id }) { row ->
                                    HistoryRowCard(row)
                                }
                            }
                        }
                    }
                }
            }

            selectedLicenseForDetail != null -> {
                BackHandler { selectedLicenseForDetail = null }

                val key = selectedLicenseForDetail!!.license_key?.trim().orEmpty()
                val currentMainName = selectedLicenseForDetail!!.item_name
                val attached = licenses.filter {
                    it.license_key?.trim() == key && !isMainTallyName(it.item_name)
                }
                val ownedUnderKey = remember(licenses, key) {
                    licenses.filter { it.license_key?.trim() == key }
                        .mapNotNull { it.item_name?.trim()?.lowercase() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                }
                val upgradeOptions = remember(allProducts, currentMainName, ownedUnderKey) {
                    filterUpgradeOptions(currentMainName, allProducts)
                        .filter { !ownedUnderKey.contains(it.name.trim().lowercase()) }
                }
                val serviceOptions = remember(allProducts, ownedUnderKey) {
                    filterServiceOptions(allProducts)
                        .filter { !ownedUnderKey.contains(it.name.trim().lowercase()) }
                }
                val displayedOpportunities =
                    if (opportunityMode == "Upgrade") upgradeOptions else serviceOptions

                Column(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        LicenseCard(selectedLicenseForDetail!!, isDetail = true) {}
                    }

                    SecondaryTabRow(
                        selectedTabIndex = licenceTab,
                        containerColor = Color.White,
                        contentColor = PrimaryIndigo
                    ) {
                        Tab(
                            selected = licenceTab == 0,
                            onClick = { licenceTab = 0 },
                            text = { Text("Attached") }
                        )
                        Tab(
                            selected = licenceTab == 1,
                            onClick = { licenceTab = 1 },
                            text = { Text("Opportunities") }
                        )
                        Tab(
                            selected = licenceTab == 2,
                            onClick = { licenceTab = 2 },
                            text = { Text("History") }
                        )
                    }

                    when (licenceTab) {
                        0 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        "Already bound under this licence (${attached.size})",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (attached.isEmpty()) {
                                    item {
                                        Text(
                                            "No additional services bound to this licence yet",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                } else {
                                    items(
                                        attached,
                                        key = { "${it.id}_${it.item_name}" }
                                    ) { addon ->
                                        AddonDetailCard(addon) {
                                            selectedService = addon
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text(
                                        "Upgrade = higher Tally only · Service = missing addons",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FilterChip(
                                            selected = opportunityMode == "Upgrade",
                                            onClick = { opportunityMode = "Upgrade" },
                                            label = { Text("Upgrade") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryIndigo,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        FilterChip(
                                            selected = opportunityMode == "Service",
                                            onClick = { opportunityMode = "Service" },
                                            label = { Text("Service") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryIndigo,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                                if (displayedOpportunities.isEmpty()) {
                                    item {
                                        Text(
                                            if (opportunityMode == "Upgrade")
                                                "No upgrade options (already highest tier or none available)."
                                            else
                                                "No service / addon opportunities found.",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    items(displayedOpportunities, key = { it.id }) { product ->
                                        OpportunityCard(
                                            product = product,
                                            isLoading = isCreatingLead,
                                            modeLabel = opportunityMode,
                                            onAddClick = {
                                                createLeadForOpportunity(product, key, opportunityMode)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text(
                                        "Full trail for licence $key",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (historyLoading) {
                                    item {
                                        Box(
                                            Modifier.fillMaxWidth().padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = PrimaryIndigo)
                                        }
                                    }
                                } else {
                                    item { SectionHeader("Leads") }
                                    if (licenceLeads.isEmpty()) {
                                        item { EmptyHint("No leads linked to this licence yet") }
                                    } else {
                                        items(licenceLeads, key = { "L${it.id}" }) {
                                            HistoryRowCard(it)
                                        }
                                    }
                                    item { SectionHeader("Proforma invoices") }
                                    if (licenceProformas.isEmpty()) {
                                        item { EmptyHint("No proformas for this licence yet") }
                                    } else {
                                        items(licenceProformas, key = { "P${it.id}" }) {
                                            HistoryRowCard(it)
                                        }
                                    }
                                    item { SectionHeader("Payments") }
                                    if (licencePayments.isEmpty()) {
                                        item { EmptyHint("No payments for this licence yet") }
                                    } else {
                                        items(licencePayments, key = { "Pay${it.id}" }) {
                                            HistoryRowCard(it)
                                        }
                                    }
                                    item { SectionHeader("Activity") }
                                    if (licenceActivity.isEmpty()) {
                                        item { EmptyHint("No activity recorded yet") }
                                    } else {
                                        items(licenceActivity, key = { "A${it.id}" }) {
                                            HistoryRowCard(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryIndigo.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            customer!!.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryIndigo,
                                            fontSize = 24.sp
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            customer!!.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            customer!!.company ?: "No Company",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                                DetailItem(Icons.Default.Phone, "Phone", customer!!.phone ?: "N/A")
                                DetailItem(Icons.Default.Email, "Email", customer!!.email ?: "N/A")
                                DetailItem(
                                    Icons.Default.Business,
                                    "Company",
                                    customer!!.company ?: "N/A"
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            "Licences already available",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Open a licence → Attached · Opportunities · History",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (mainLicenseRows.isEmpty()) {
                        item {
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "No licences found for this customer.",
                                    Modifier.padding(24.dp),
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(mainLicenseRows, key = { (_, lic) -> "${lic.id}" }) { (_, mainLic) ->
                            LicenseCard(mainLic) { selectedLicenseForDetail = mainLic }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = Color.Gray, fontSize = 13.sp)
}

@Composable
fun HistoryRowCard(item: LicenseHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (item.subtitle.isNotBlank()) {
                    Text(item.subtitle, fontSize = 12.sp, color = Color.Gray)
                }
                if (item.date.isNotBlank()) {
                    Text(item.date, fontSize = 11.sp, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item.amount.isNotBlank()) {
                    Text(item.amount, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (item.status.isNotBlank()) {
                    Text(item.status, fontSize = 11.sp, color = PrimaryIndigo)
                }
            }
        }
    }
}

@Composable
fun AddonDetailCard(license: CustomerLicense, onClick: (() -> Unit)? = null) {
    val isActive = license.status?.lowercase() in listOf("active", null, "")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Color(0xFFFCE7F3), shape = RoundedCornerShape(6.dp)) {
                Text(
                    "Service / Addon",
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9D174D)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(license.item_name ?: "Item", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val expiry =
                    if (!license.expiry_date.isNullOrBlank() && license.expiry_date != "0000-00-00")
                        "Exp: ${license.expiry_date}" else "No expiry set"
                Text(expiry, fontSize = 11.sp, color = Color.Gray)
                if (onClick != null) {
                    Text(
                        "Tap for proforma · payments · activity",
                        fontSize = 11.sp,
                        color = PrimaryIndigo
                    )
                }
            }
            Text(
                (license.status ?: "active").uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isActive) Color(0xFF166534) else Color(0xFF991B1B)
            )
        }
    }
}

@Composable
fun LicenseCard(
    license: CustomerLicense,
    isDetail: Boolean = false,
    onClick: () -> Unit
) {
    val isActive = license.status?.lowercase() in listOf("active", null, "")
    val licenseKey = license.license_key?.trim().orEmpty().ifEmpty { "(no key)" }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDetail, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VpnKey, null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    license.item_name ?: "Tally License",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "License: $licenseKey",
                    fontSize = 13.sp,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Medium
                )
                Text("Expires: Lifetime (main)", fontSize = 12.sp, color = Color.Gray)
            }
            Surface(
                color = if (isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    (license.status ?: "ACTIVE").uppercase(),
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (isActive) Color(0xFF166534) else Color(0xFF991B1B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OpportunityCard(
    product: Product,
    isLoading: Boolean = false,
    modeLabel: String = "Service",
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    if (modeLabel == "Upgrade") "Tally upgrade" else product.category,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onAddClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("ADD", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}