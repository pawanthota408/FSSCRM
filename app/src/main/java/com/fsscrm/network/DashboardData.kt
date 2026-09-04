package com.fsscrm.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class Employee(
    val id: Int? = null,
    val employee_id: String? = null,
    val employee_code: String? = null,
    val name: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val position: String? = null,
    val department_id: Int? = null,
    val department_name: String? = null,
    val status: String? = null,
    @SerializedName("profile_image", alternate = ["profile_pic"]) val profile_image: String? = null,
    val joining_date: String? = null,
    val date_of_birth: String? = null,
    @SerializedName("created_at", alternate = ["createdAt"]) val created_at: String? = null
)

data class AttendanceStatus(
    @SerializedName("status", alternate = ["todayStatus", "current_status", "today_status"]) val status: String? = null,
    @SerializedName("check_in_time", alternate = ["checkInTime", "check_in", "login_time", "checkin_time"]) val checkInTime: String? = null,
    @SerializedName("check_out_time", alternate = ["checkOutTime", "check_out", "logout_time", "checkout_time"]) val checkOutTime: String? = null,
    @SerializedName("working_hours", alternate = ["duration", "hours_worked", "workingHours"]) val workingHours: String? = null,
    @SerializedName("isCheckedIn", alternate = ["is_checked_in"]) val isCheckedIn: Boolean? = null,
    @SerializedName("isCheckedOut", alternate = ["is_checked_out"]) val isCheckedOut: Boolean? = null
) {
    val isActuallyCheckedIn: Boolean get() = isCheckedIn ?: (status == "Present" || status == "active" || !checkInTime.isNullOrEmpty())
    val isActuallyCheckedOut: Boolean get() = isCheckedOut ?: (!checkOutTime.isNullOrEmpty() && checkOutTime != "00:00:00")
}

data class DashboardStats(
    val totalLeads: Int = 0,
    val wonDeals: Int = 0,
    val revenue: Double = 0.0,
    val pendingTasks: Int = 0,
    val totalTasks: Int = 0,
    val presentDays: Int = 0
)

data class RawDashboardStats(
    @SerializedName("total_tasks", alternate = ["totalTasks"]) val totalTasks: Any? = null,
    @SerializedName("pending_tasks", alternate = ["pendingTasks", "tasks_pending"]) val pendingTasks: Any? = null,
    @SerializedName("present_days", alternate = ["presentDays", "present_count"]) val presentDays: Any? = null
)

data class LeadDashboardStats(
    @SerializedName("total_leads", alternate = ["totalLeads", "leads_count"]) val totalLeads: Any? = null,
    @SerializedName("converted_leads", alternate = ["convertedLeads", "converted_count", "deals_won", "won_deals"]) val convertedLeads: Any? = null,
    @SerializedName("revenue", alternate = ["total_revenue", "sales", "total_paid", "amount_received", "paid_amount", "total_paid_amount", "total_amount_received", "won_amount", "deals_amount"]) val revenue: Any? = null
)

data class StatusBadge(
    val label: String? = null,
    val color: String? = null,
    val bg: String? = null
)

data class Lead(
    @SerializedName("id") val id: Int,
    @SerializedName("customer_id") val customer_id: Int? = null,
    @SerializedName("license_key") val license_key: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("company", alternate = ["company_name"]) val company: String? = null,
    @SerializedName("connection_type") val connection_type: String? = null,
    @SerializedName("serial_number") val serial_number: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = "New",
    @SerializedName("source") val source: String? = null,
    @SerializedName("lead_type") val lead_type: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("preferred_contact") val preferred_contact: String? = null,
    @SerializedName("ip_address") val ip_address: String? = null,
    @SerializedName("assigned_to") val assigned_to: Int? = null,
    @SerializedName("created_by") val created_by: Int? = null,
    @SerializedName("created_at", alternate = ["createdAt"]) val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null,
    @SerializedName("employee_name") val employee_name: String? = null,
    @SerializedName("assigned_name") val assigned_name: String? = null,
    @SerializedName("creator_name") val creator_name: String? = null,
    @SerializedName("date") val date_flat: String? = null,
    @SerializedName("has_licences") val has_licences: Int? = 0,
    @SerializedName("is_existing_customer") val is_existing_customer: Boolean? = false,
    @SerializedName("status_badge") val status_badge: StatusBadge? = null,
    @SerializedName("date_time") val date_time: String? = null,
    @SerializedName("safeName") val safeNameField: String? = null
) {
    val company_name: String? get() = company
    val createdAt: String? get() = created_at
    val safeName: String get() = name ?: safeNameField ?: company ?: "Unnamed"
    val effectiveLicenseNumber: String?
        get() = serial_number?.takeIf { it.isNotBlank() } ?: license_key?.takeIf { it.isNotBlank() }

    fun formattedRequirements(): List<String> {
        val rawService = service ?: return emptyList()
        val lic = effectiveLicenseNumber
        return rawService.split(",").map { item ->
            val reqText = item.trim()
            if (!lic.isNullOrBlank() && !reqText.contains(lic, ignoreCase = true)) {
                "$lic : $reqText"
            } else {
                reqText
            }
        }.filter { it.isNotBlank() }
    }

    val formattedRequirementSummary: String?
        get() {
            val reqs = formattedRequirements()
            if (reqs.isEmpty()) return null
            return reqs.joinToString(", ")
        }

    val date: String get() {
        return try {
            val inputStr = created_at ?: date_flat ?: ""
            if (inputStr.isEmpty()) return "N/A"
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val d = inputFormat.parse(inputStr)
            outputFormat.format(d ?: java.util.Date())
        } catch (e: Exception) { (created_at ?: date_flat ?: "").take(10) }
    }
}

data class ActivityLog(
    val id: Int,
    @SerializedName("employee_id") val employee_id: Int? = null,
    @SerializedName("action") val action: String,
    @SerializedName("activity_type") val activity_type_raw: String? = null,
    @SerializedName("details", alternate = ["description"]) val details: String?,
    @SerializedName("date", alternate = ["created_at"]) val date: String,
    val status: String? = null,
    val lead_id: Int? = null,
    val client_name: String? = null,
    @SerializedName("company_name", alternate = ["company"]) val company_name: String? = null,
    val client_contact_person: String? = null,
    val client_designation: String? = null,
    val client_mobile: String? = null,
    val client_email: String? = null,
    val meeting_purpose: String? = null,
    val meeting_notes: String? = null,
    val location_address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_link: String? = null,
    val visit_start_time: String? = null,
    val visit_end_time: String? = null,
    val employee_checkin_photo: String? = null,
    val employee_checkout_photo: String? = null,
    val employee_signature: String? = null,
    val client_signature: String? = null,
    val client_feedback: String? = null,
    val rating: Int? = null,
    val next_followup_date: String? = null,
    val next_followup_notes: String? = null,
    val attendees: List<Attendee>? = null,
    val assignedUsers: List<Employee>? = null,
    val email: String? = null,
    val phone: String? = null
) {
    val activity_type: String get() = activity_type_raw ?: action ?: "Activity"
    val created_at: String get() = date
    val description: String? get() = details ?: meeting_notes ?: meeting_purpose
}

data class Attendee(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val position: String? = null,
    val initial: String? = null
)

data class ProfileResponse(
    val status: String,
    @SerializedName("employee", alternate = ["profile"]) val employee: Employee? = null,
    val message: String? = null,
    val error: String? = null
)

data class DashboardResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("profile") val profile: Employee? = null,
    @SerializedName("attendance") val attendance: AttendanceStatus? = null,
    @SerializedName("stats") val stats: RawDashboardStats? = null,
    @SerializedName("leads") val leads: LeadDashboardStats? = null,
    @SerializedName("recentLeads") val recentLeads: List<Lead> = emptyList(),
    @SerializedName("tasks") val tasks: List<Task> = emptyList(),
    @SerializedName("activities") val activities: List<ActivityLog> = emptyList(),
    @SerializedName("announcements") val announcements: List<Announcement> = emptyList(),
    @SerializedName("expiring_licenses") val expiringLicenses: List<CustomerLicense> = emptyList(),
    @SerializedName("name") val rootName: String? = null
) {
    private fun toInt(v: Any?): Int = when(v) { is Number -> v.toInt(); is String -> v.toIntOrNull() ?: 0; else -> 0 }
    private fun toDouble(v: Any?): Double = when(v) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0 }
    fun getEffectiveStats() = DashboardStats(toInt(leads?.totalLeads), toInt(leads?.convertedLeads), toDouble(leads?.revenue), toInt(stats?.pendingTasks), toInt(stats?.totalTasks), toInt(stats?.presentDays))
    fun getEffectiveAttendance() = attendance
    companion object {
        private val lenientGson = RetrofitClient.gson

        fun fromJson(json: JsonElement): DashboardResponse {
            return try {
                val dataPart = if (json.isJsonObject && json.asJsonObject.has("data") && json.asJsonObject["data"].isJsonObject) json.asJsonObject["data"] else json
                lenientGson.fromJson(dataPart, DashboardResponse::class.java)
            } catch (e: Exception) { DashboardResponse() }
        }
    }
}

data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("licence_type") val licence_type: String? = null,
    @SerializedName("category") val category: String,
    @SerializedName("icon_link") val icon_link: String? = null,
    @SerializedName("serial_at") val serial_at: String? = null,
    @SerializedName("status") val status: String = "active"
) {
    companion object {
        fun fromJson(json: JsonElement): Product = RetrofitClient.gson.fromJson(json, Product::class.java)
    }
}

private fun parsePrice(value: String?): Double {
    if (value == null) return 0.0
    return value.replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
}

data class QuoteRequirement(
    @SerializedName("description", alternate = ["requirement", "item", "product_name"]) val requirement: String = "",
    @SerializedName("rate", alternate = ["cost", "unit_price", "price", "item_price", "item_rate", "item_cost"]) val cost: String = "0.00",
    @SerializedName("qty", alternate = ["quantity"]) val quantity: String = "1",
    @SerializedName("amount", alternate = ["total", "total_price", "item_total", "total_amount"]) val amount: String = "0.00",
    @SerializedName("details", alternate = ["specifications", "remarks"]) val details: String = "",
    val product_id: Int? = null,
    val serial_number: String? = null,
    val licence_type: String? = null,
    val serial_at: String? = null,
    val expiry_date: String? = null
) {
    val quantityInt: Int get() = quantity.toDoubleOrNull()?.toInt() ?: 1

    val effectivePrice: Double
        get() {
            val c = parsePrice(cost)
            if (c > 0.0) return c
            val a = parsePrice(amount)
            if (a > 0.0) {
                val q = quantityInt.coerceAtLeast(1)
                return a / q
            }
            return 0.0
        }

    val effectiveAmount: Double
        get() {
            val a = parsePrice(amount)
            if (a > 0.0) return a
            return effectivePrice * quantityInt
        }
}

data class Work(
    @SerializedName("id") val id: Int,
    @SerializedName("lead_id", alternate = ["lead_id_raw"]) val lead_id: Int = 0,
    @SerializedName("customer_id", alternate = ["customer_id_raw"]) val customer_id: Int? = null,
    @SerializedName("work_name", alternate = ["name", "title", "project_name", "service", "work"]) val work_name: String = "",
    @SerializedName("description", alternate = ["notes", "details", "remarks"]) val description: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("start_date") val start_date: String? = null,
    @SerializedName("expected_completion") val expected_completion: String? = null,
    @SerializedName("actual_completion") val actual_completion: String? = null,
    @SerializedName("handover_date") val handover_date: String? = null,
    @SerializedName("total_amount", alternate = ["amount", "total", "deal_total"]) val total_amount: String = "0.00",
    @SerializedName("advance_received", alternate = ["advance", "paid_amount"]) val advance_received: String = "0.00",
    @SerializedName("balance_amount", alternate = ["balance", "due_amount"]) val balance_amount: String = "0.00",
    @SerializedName("amount_received") val amount_received: String = "0.00",
    @SerializedName("payment_type") val payment_type: String? = null,
    @SerializedName("payment_mode") val payment_mode: String? = null,
    @SerializedName("emi_count") val emi_count: Int? = null,
    @SerializedName("emi_amount") val emi_amount: String? = null,
    @SerializedName("created_by") val created_by: Int? = null,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("updated_at") val updated_at: String? = null,
    @SerializedName("lead_name") val lead_name: String? = null,
    @SerializedName("lead_email") val lead_email: String? = null,
    @SerializedName("lead_phone") val lead_phone: String? = null,
    @SerializedName("lead_company") val lead_company: String? = null,
    @SerializedName("customer_name") val customer_name: String? = null,
    @SerializedName("customer_email") val customer_email: String? = null,
    @SerializedName("customer_phone") val customer_phone: String? = null,
    @SerializedName("customer_company") val customer_company: String? = null,
    @SerializedName("license_key") val license_key: String? = null,
    @SerializedName("serial_number") val serial_number: String? = null,
    @SerializedName("razorpay_link_id") val razorpay_link_id: String? = null,
    @SerializedName("razorpay_short_url") val razorpay_short_url: String? = null
)

data class WorkDetailsResponse(
    val work: Work? = null,
    val payments: List<Payment>? = emptyList(),
    val history: List<ActivityTimelineItem>? = emptyList(),
    val proformas: List<ProformaInvoice> = emptyList(),
    val proforma: ProformaInvoice? = null
) {
    companion object {
        private val lenientGson = RetrofitClient.gson

        fun fromJson(json: JsonElement): WorkDetailsResponse {
            return try {
                val dataPart = if (json.isJsonObject && json.asJsonObject.has("data") && json.asJsonObject["data"].isJsonObject) json.asJsonObject["data"] else json
                val parsed = lenientGson.fromJson(dataPart, WorkDetailsResponse::class.java)
                if (parsed.work == null && dataPart.isJsonObject && (dataPart.asJsonObject.has("work_name") || dataPart.asJsonObject.has("id"))) {
                    return parsed.copy(work = lenientGson.fromJson(dataPart, Work::class.java))
                }
                parsed
            } catch (e: Exception) { WorkDetailsResponse() }
        }
    }
}

data class WorkResponse(
    val status: String? = null,
    @SerializedName("works", alternate = ["data", "work_list", "active_works", "projects"]) val works: List<Work> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): WorkResponse {
            return try {
                if (json.isJsonArray) {
                    val list = lenientGson.fromJson<List<Work>>(json, object : TypeToken<List<Work>>() {}.type) ?: emptyList()
                    WorkResponse(status = "success", works = list)
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val targetArray = if (obj.has("works") && obj.get("works").isJsonArray) obj.getAsJsonArray("works")
                        else if (obj.has("data") && obj.get("data").isJsonArray) obj.getAsJsonArray("data")
                        else if (obj.has("work_list") && obj.get("work_list").isJsonArray) obj.getAsJsonArray("work_list")
                        else if (obj.has("active_works") && obj.get("active_works").isJsonArray) obj.getAsJsonArray("active_works")
                        else if (obj.has("projects") && obj.get("projects").isJsonArray) obj.getAsJsonArray("projects")
                        else null

                    if (targetArray != null) {
                        val list = lenientGson.fromJson<List<Work>>(targetArray, object : TypeToken<List<Work>>() {}.type) ?: emptyList()
                        WorkResponse(status = obj.get("status")?.asString ?: "success", works = list)
                    } else {
                        lenientGson.fromJson(json, WorkResponse::class.java)
                    }
                } else {
                    WorkResponse(status = "error", works = emptyList())
                }
            } catch (e: Exception) { WorkResponse(status = "error", works = emptyList()) }
        }
    }
}

data class Payment(
    @SerializedName("id") val id: Int,
    @SerializedName("payment_reference") val payment_reference: String? = null,
    @SerializedName("customer_id") val customer_id: Int? = null,
    @SerializedName("customer_name") val customer_name: String? = null,
    @SerializedName("customer_email") val customer_email: String? = null,
    @SerializedName("customer_phone") val customer_phone: String? = null,
    @SerializedName("lead_id") val lead_id: Int? = null,
    @SerializedName("work_id") val work_id: Int? = null,
    @SerializedName("invoice_id") val invoice_id: Int? = null,
    @SerializedName("amount") val amount: String,
    @SerializedName("payment_date") val payment_date: String,
    @SerializedName("payment_method") val payment_method: String? = null,
    @SerializedName("transaction_id") val transaction_id: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("payment_type") val payment_type: String? = null,
    @SerializedName("created_by") val created_by: Int? = null,
    @SerializedName("created_at", alternate = ["createdAt"]) val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("reference_no") val reference_no: String? = null,
    @SerializedName("proforma_id") val proforma_id: Int? = null,
    @SerializedName("quote_id") val quote_id: Int? = null
) {
    val createdAt: String? get() = created_at
    val mode: String? get() = payment_method
}

data class FollowUp(
    val id: Int,
    val lead_id: Int,
    val employee_id: Int? = null,
    @SerializedName("follow_up_date") val follow_up_date: String,
    @SerializedName("follow_up_time") val follow_up_time: String? = null,
    @SerializedName("follow_up_date_display") val follow_up_date_display: String? = null,
    val remarks: String? = null,
    val status: String = "pending",
    val created_at: String? = null,
    val completed_at: String? = null,
    @SerializedName("lead_name") val lead_name: String? = null
)

data class FollowUpResponse(
    val status: String? = null,
    val followups: List<FollowUp> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson

        fun fromJson(json: JsonElement): FollowUpResponse {
            return try {
                val dataPart = if (json.isJsonObject && json.asJsonObject.has("data") && json.asJsonObject["data"].isJsonObject) json.asJsonObject["data"] else json
                lenientGson.fromJson(dataPart, FollowUpResponse::class.java)
            } catch (e: Exception) { FollowUpResponse() }
        }
    }
}

data class ActivityTimelineItem(
    val action: String? = null,
    @SerializedName("status_from") val status_from: String? = null,
    @SerializedName("status_to") val status_to: String? = null,
    val remarks: String? = null,
    val date: String? = null,
    @SerializedName("employee_name") val employee_name: String? = null
)

data class ProformaInvoice(
    @SerializedName("id") val id: Int,
    @SerializedName("proforma_no", alternate = ["proforma_number", "invoice_no", "reference_no", "number"]) val proforma_no: String = "",
    @SerializedName("lead_id") val lead_id: Int = 0,
    @SerializedName("customer_name", alternate = ["name", "client_name"]) val customer_name: String = "",
    @SerializedName("customer_email") val customer_email: String? = null,
    @SerializedName("customer_phone") val customer_phone: String? = null,
    @SerializedName("customer_company") val customer_company: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("amount") val amount: String? = "0.00",
    @SerializedName("tax") val tax: String? = "0.00",
    @SerializedName("discount") val discount: String? = "0.00",
    @SerializedName("total") val total: String? = "0.00",
    @SerializedName("items", alternate = ["proforma_items", "requirements", "requirements_json", "line_items", "details"]) val items: String? = null,
    @SerializedName("status") val status: String? = "pending",
    @SerializedName("created_by") val created_by: Int? = null,
    @SerializedName("created_at", alternate = ["createdAt", "date", "proforma_date"]) val created_at: String? = null,
    @SerializedName("sent_at") val sent_at: String? = null,
    @SerializedName("quote_id") val quote_id: Int? = null,
    @SerializedName("approved_by") val approved_by: Int? = null,
    @SerializedName("approved_at") val approved_at: String? = null,
    @SerializedName("rejected_reason") val rejected_reason: String? = null,
    @SerializedName("sent_to_customer_at") val sent_to_customer_at: String? = null,
    @SerializedName("admin_notes") val admin_notes: String? = null,
    @SerializedName("sent_to_admin") val sent_to_admin: Int? = 0
) {
    val createdAt: String? get() = created_at
    val items_parsed: List<QuoteRequirement> get() {
        return try {
            if (items.isNullOrBlank()) emptyList()
            else RetrofitClient.gson.fromJson(items, object : com.google.gson.reflect.TypeToken<List<QuoteRequirement>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }
    companion object {
        fun fromJson(json: JsonElement): ProformaInvoice {
            return try {
                val element = if (json.isJsonArray && json.asJsonArray.size() > 0) json.asJsonArray[0] else json
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val target = if (obj.has("data") && obj.get("data").isJsonObject) obj.get("data").asJsonObject
                        else if (obj.has("proforma") && obj.get("proforma").isJsonObject) obj.get("proforma").asJsonObject
                        else if (obj.has("proforma_invoice") && obj.get("proforma_invoice").isJsonObject) obj.get("proforma_invoice").asJsonObject
                        else if (obj.has("details") && obj.get("details").isJsonObject) obj.get("details").asJsonObject
                        else obj
                    
                    val p = RetrofitClient.gson.fromJson(target, ProformaInvoice::class.java)
                    if (p.items.isNullOrBlank()) {
                        val arr = if (target.has("items") && target.get("items").isJsonArray) target.getAsJsonArray("items")
                            else if (target.has("proforma_items") && target.get("proforma_items").isJsonArray) target.getAsJsonArray("proforma_items")
                            else if (target.has("requirements") && target.get("requirements").isJsonArray) target.getAsJsonArray("requirements")
                            else null
                        if (arr != null) {
                            return p.copy(items = arr.toString())
                        }
                    }
                    p
                } else {
                    RetrofitClient.gson.fromJson(json, ProformaInvoice::class.java)
                }
            } catch (e: Exception) {
                RetrofitClient.gson.fromJson(json, ProformaInvoice::class.java)
            }
        }
    }
}

data class AppNotification(
    val id: Int,
    val title: String,
    val description: String,
    @SerializedName("is_read", alternate = ["isRead"]) val is_read: Boolean,
    @SerializedName("created_at", alternate = ["createdAt", "date"]) val created_at: String,
    val type: String? = null,
    val related_id: Int? = null
) {
    val date: String get() = created_at
}

data class Customer(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val lead_id: Int? = null,
    val status: String? = null,
    @SerializedName("created_at", alternate = ["createdAt"]) val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("contact_person") val contactPerson: String? = null,
    @SerializedName("contact_mobile") val contactMobile: String? = null,
    @SerializedName("contact_email") val contactEmail: String? = null,
    @SerializedName("emp_id") val emp_id: Int? = null,
    @SerializedName("serial_number") val serial_number: String? = null,
    @SerializedName("balance_amount") val balance_amount: Double? = null,
    @SerializedName("total_works") val total_works: Int? = null,
    @SerializedName("assigned_employee_name") val assignedEmployeeName: String? = null,
    @SerializedName("employee_code") val employeeCode: String? = null,
    @SerializedName("employee_position") val employeePosition: String? = null,
    @SerializedName("license_key") val licenseKey: String? = null,
    @SerializedName("license_count") val license_count: Int? = 0,
    @SerializedName("amount") val amount: String? = "0.00",

    // Extended fields for Admin view
    @SerializedName("main_licenses") val main_licenses: List<CustomerLicense>? = null,
    @SerializedName("licence_key_count") val licence_key_count: Int? = null,
    @SerializedName("customer_status") val customer_status: String? = null,
    @SerializedName("lead_service") val lead_service: String? = null
) {
    val createdAt: String? get() = created_at
}

data class LicenceGroup(
    @SerializedName("license_key") val license_key: String,
    @SerializedName("main") val main: CustomerLicense? = null,
    @SerializedName("addons") val addons: List<CustomerLicense> = emptyList()
)

data class CustomerLicense(
    val id: Int? = null,
    val customer_id: Int? = null,
    val lead_id: Int? = null,
    val parent_license_id: Int? = null,
    val item_name: String? = null,
    val license_key: String? = null,
    val expiry_date: String? = null,
    val status: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    @SerializedName("is_main") val is_main_raw: Any? = null,
    @SerializedName("days_until_expiry") val days_until_expiry: Int? = null,
    @SerializedName("expiry_date_formatted") val expiry_date_formatted: String? = null,

    // Extended fields for badges
    @SerializedName("licence_type") val licence_type: String? = null,
    @SerializedName("license_type") val license_type: String? = null,
    @SerializedName("serial_number") val serial_number: String? = null
) {
    val is_main: Boolean get() = when(is_main_raw) {
        is Boolean -> is_main_raw
        is Number -> is_main_raw.toInt() == 1
        is String -> is_main_raw == "1" || is_main_raw.lowercase() == "true"
        else -> parent_license_id == null || parent_license_id == 0
    }
    val effectiveStatus: String get() = status?.lowercase() ?: "active"
    val title: String get() = item_name ?: "Unknown Product"

    companion object {
        fun fromJson(json: JsonObject): CustomerLicense {
            return RetrofitClient.gson.fromJson(json, CustomerLicense::class.java)
        }
    }
}

data class CustomerLicensesResponse(
    val status: String? = null,
    val message: String? = null,
    val licenses: List<CustomerLicense>? = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): CustomerLicensesResponse {
            return try {
                if (json.isJsonArray) {
                    val list = lenientGson.fromJson<List<CustomerLicense>>(json, object : com.google.gson.reflect.TypeToken<List<CustomerLicense>>() {}.type)
                    CustomerLicensesResponse(status = "success", licenses = list)
                } else {
                    lenientGson.fromJson(json, CustomerLicensesResponse::class.java)
                }
            } catch (e: Exception) { CustomerLicensesResponse(status = "error") }
        }
    }
}

data class QuotationResponse(
    val status: String? = null,
    val quotations: List<QuoteDetails> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): QuotationResponse {
            return try {
                if (json.isJsonArray) {
                    val list = lenientGson.fromJson<List<QuoteDetails>>(json, object : com.google.gson.reflect.TypeToken<List<QuoteDetails>>() {}.type)
                    QuotationResponse(status = "success", quotations = list)
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val array = if (obj.has("quotations")) obj.get("quotations") else if (obj.has("data")) obj.get("data") else null
                    if (array != null && array.isJsonArray) {
                        val list = lenientGson.fromJson<List<QuoteDetails>>(array, object : com.google.gson.reflect.TypeToken<List<QuoteDetails>>() {}.type)
                        QuotationResponse(status = obj.get("status")?.asString ?: "success", quotations = list)
                    } else {
                        lenientGson.fromJson(json, QuotationResponse::class.java)
                    }
                } else {
                    QuotationResponse(status = "error", quotations = emptyList())
                }
            } catch (e: Exception) { QuotationResponse(status = "error", quotations = emptyList()) }
        }
    }
}

data class ProformaResponse(
    val status: String? = null,
    val proformas: List<ProformaInvoice> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): ProformaResponse {
            return try {
                if (json.isJsonArray) {
                    val list = lenientGson.fromJson<List<ProformaInvoice>>(json, object : com.google.gson.reflect.TypeToken<List<ProformaInvoice>>() {}.type)
                    ProformaResponse(status = "success", proformas = list)
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val array = if (obj.has("proformas")) obj.get("proformas") else if (obj.has("data")) obj.get("data") else null
                    if (array != null && array.isJsonArray) {
                        val list = lenientGson.fromJson<List<ProformaInvoice>>(array, object : com.google.gson.reflect.TypeToken<List<ProformaInvoice>>() {}.type)
                        ProformaResponse(status = obj.get("status")?.asString ?: "success", proformas = list)
                    } else {
                        lenientGson.fromJson(json, ProformaResponse::class.java)
                    }
                } else {
                    ProformaResponse(status = "error", proformas = emptyList())
                }
            } catch (e: Exception) { ProformaResponse(status = "error", proformas = emptyList()) }
        }
    }
}

data class ProductResponse(
    val status: String? = null,
    val products: List<Product> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): ProductResponse {
            return try {
                if (json.isJsonArray) {
                    val list = lenientGson.fromJson<List<Product>>(json, object : com.google.gson.reflect.TypeToken<List<Product>>() {}.type)
                    ProductResponse(status = "success", products = list)
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val array = when {
                        obj.has("products_services") -> obj.get("products_services")
                        obj.has("products") -> obj.get("products")
                        obj.has("data") -> obj.get("data")
                        else -> null
                    }
                    if (array != null && array.isJsonArray) {
                        val list = lenientGson.fromJson<List<Product>>(array, object : com.google.gson.reflect.TypeToken<List<Product>>() {}.type)
                        ProductResponse(status = obj.get("status")?.asString ?: "success", products = list)
                    } else {
                        lenientGson.fromJson(json, ProductResponse::class.java)
                    }
                } else {
                    ProductResponse(status = "error", products = emptyList())
                }
            } catch (e: Exception) { ProductResponse(status = "error", products = emptyList()) }
        }
    }
}

data class QuoteDetails(
    val id: Int,
    @SerializedName("quote_no", alternate = ["quotation_no", "quote_number", "number", "reference_no"]) val quote_no: String = "",
    @SerializedName("customer_name", alternate = ["name", "client_name"]) val customer_name: String = "",
    val customer_email: String? = null,
    val customer_phone: String? = null,
    val customer_company: String? = null,
    val amount: String? = "0.00",
    val tax: String? = "0.00",
    val total: String? = "0.00",
    val status: String? = "pending",
    val quote_date: String? = null,
    val valid_until: String? = null,
    val lead_id: Int? = null,
    val created_by: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val modified_by_admin: Int? = null,
    val modified_by: Int? = null,
    val modified_at: String? = null,
    val admin_notes: String? = null,
    val discount: String? = "0.00",
    @SerializedName("requirements", alternate = ["items", "quote_items", "details", "line_items", "products"]) val requirements: List<QuoteRequirement>? = null,
    @SerializedName("items_json", alternate = ["items_str", "requirements_json", "requirements_str", "items_text"]) val rawItemsJson: String? = null
) {
    val allRequirements: List<QuoteRequirement> get() {
        if (!requirements.isNullOrEmpty()) return requirements
        if (!rawItemsJson.isNullOrBlank()) {
            try {
                val list = RetrofitClient.gson.fromJson<List<QuoteRequirement>>(
                    rawItemsJson,
                    object : com.google.gson.reflect.TypeToken<List<QuoteRequirement>>() {}.type
                )
                if (!list.isNullOrEmpty()) return list
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    companion object {
        fun fromJson(json: JsonElement): QuoteDetails {
            return try {
                val element = if (json.isJsonArray && json.asJsonArray.size() > 0) json.asJsonArray[0] else json
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val target = if (obj.has("data") && obj.get("data").isJsonObject) obj.get("data").asJsonObject
                        else if (obj.has("quote") && obj.get("quote").isJsonObject) obj.get("quote").asJsonObject
                        else if (obj.has("quotation") && obj.get("quotation").isJsonObject) obj.get("quotation").asJsonObject
                        else if (obj.has("details") && obj.get("details").isJsonObject) obj.get("details").asJsonObject
                        else obj
                    
                    val q = RetrofitClient.gson.fromJson(target, QuoteDetails::class.java)
                    if (q.requirements == null) {
                        val arr = if (target.has("requirements") && target.get("requirements").isJsonArray) target.getAsJsonArray("requirements")
                            else if (target.has("items") && target.get("items").isJsonArray) target.getAsJsonArray("items")
                            else if (target.has("quote_items") && target.get("quote_items").isJsonArray) target.getAsJsonArray("quote_items")
                            else if (target.has("details") && target.get("details").isJsonArray) target.getAsJsonArray("details")
                            else null
                        
                        if (arr != null) {
                            val reqs = RetrofitClient.gson.fromJson<List<QuoteRequirement>>(
                                arr,
                                object : com.google.gson.reflect.TypeToken<List<QuoteRequirement>>() {}.type
                            )
                            return q.copy(requirements = reqs)
                        }
                    }
                    q
                } else {
                    RetrofitClient.gson.fromJson(json, QuoteDetails::class.java)
                }
            } catch (e: Exception) {
                RetrofitClient.gson.fromJson(json, QuoteDetails::class.java)
            }
        }
    }
}

data class LicenseDetailsResponse(
    val status: String? = null,
    val license_key: String? = null,
    val customer: Customer? = null,
    val linked_items: List<CustomerLicense> = emptyList(),
    val missing_products: List<Product> = emptyList(),
    val quotes: List<QuoteDetails> = emptyList(),
    val proformas: List<ProformaInvoice> = emptyList(),
    val payments: List<Payment> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): LicenseDetailsResponse {
            return try {
                lenientGson.fromJson(json, LicenseDetailsResponse::class.java)
            } catch (e: Exception) { LicenseDetailsResponse(status = "error") }
        }
    }
}

data class Announcement(val id: Int, val title: String, val content: String, val date: String) {
    val description: String get() = content
}

data class Task(
    val id: Int, 
    val name: String? = null, 
    val dueDate: String? = null, 
    val status: String? = null,
    val priority: String? = null,
    val description: String? = null
)

data class AppTask(
    val id: Int, 
    val task_name: String, 
    val status: String? = "pending", 
    val due_date: String? = null, 
    val client_name: String? = null,
    val priority: String? = null,
    val description: String? = null
)

data class LeaveRequest(
    val id: Int, 
    val leave_type: String, 
    val start_date: String, 
    val end_date: String, 
    val status: String,
    val days: Int = 0,
    val reason: String? = null
)

data class PayrollRecord(
    val id: Int = 0,
    @SerializedName("employee_id") val employee_id: Int? = null,
    @SerializedName("employee_name") val employee_name: String? = null,
    @SerializedName("payroll_month", alternate = ["month"]) val payroll_month_raw: Any? = null,
    @SerializedName("payroll_year", alternate = ["year"]) val payroll_year_raw: Any? = null,
    @SerializedName("basic_salary") val basic_salary: String? = "0",
    @SerializedName("house_rent_allowance") val house_rent_allowance: String? = "0",
    @SerializedName("dearness_allowance") val dearness_allowance: String? = "0",
    @SerializedName("transport_allowance") val transport_allowance: String? = "0",
    @SerializedName("medical_allowance") val medical_allowance: String? = "0",
    @SerializedName("special_allowance") val special_allowance: String? = "0",
    @SerializedName("total_allowances") val total_allowances: String? = "0",
    @SerializedName("gross_salary") val gross_salary: String? = "0",
    @SerializedName("pf_deduction") val pf_deduction: String? = "0",
    @SerializedName("professional_tax") val professional_tax: String? = "0",
    @SerializedName("income_tax") val income_tax: String? = "0",
    @SerializedName("other_deductions") val other_deductions: String? = "0",
    @SerializedName("total_deductions") val total_deductions: String? = "0",
    @SerializedName("net_salary", alternate = ["net", "salary", "amount"]) val net_salary_raw: String? = "0",
    @SerializedName("status") val status_raw: String? = "pending",
    @SerializedName("payment_date", alternate = ["paid_date", "paid_at"]) val payment_date: String? = null,
    @SerializedName("payment_method", alternate = ["method"]) val payment_method: String? = null,
    @SerializedName("transaction_id", alternate = ["txn_id", "utr"]) val transaction_id: String? = null
) {
    val payroll_month: String get() = monthName
    val payroll_year: String get() = yearStr

    val monthName: String
        get() {
            val m = payroll_month_raw?.toString() ?: ""
            val intM = m.toIntOrNull()
            return if (intM != null && intM in 1..12) {
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                months[intM - 1]
            } else if (m.isNotBlank()) {
                m
            } else "N/A"
        }

    val yearStr: String
        get() = payroll_year_raw?.toString() ?: ""

    val net_salary: String
        get() = net_salary_raw ?: "0"

    val status: String
        get() = status_raw ?: "pending"
}

data class LeaveResponse(
    val status: String? = null,
    val data: List<LeaveRequest> = emptyList(),
    val leaves: List<LeaveRequest>? = null // some APIs use 'leaves' key
) {
    val allLeaves: List<LeaveRequest> get() = leaves ?: data
}

data class PayrollResponse(
    val status: String? = null,
    val data: List<PayrollRecord> = emptyList(),
    val records: List<PayrollRecord>? = null
) {
    val allRecords: List<PayrollRecord> get() = records ?: data
}

data class EmployeesResponse(
    val status: String? = null,
    @SerializedName("employees", alternate = ["data"]) val employees: List<Employee> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): EmployeesResponse {
            return try {
                lenientGson.fromJson(json, EmployeesResponse::class.java)
            } catch (e: Exception) { EmployeesResponse(status = "error") }
        }
    }
}

data class LeadDetailsResponse(
    val status: String? = null,
    val message: String? = null,
    val lead: Lead? = null,
    val customer: Customer? = null,
    @SerializedName("history", alternate = ["lead_history"]) val history: List<ActivityTimelineItem> = emptyList(),
    @SerializedName("activities", alternate = ["activity_log"]) val activities: List<ActivityTimelineItem> = emptyList(),
    @SerializedName("follow_ups", alternate = ["followups"]) val follow_ups: List<FollowUp> = emptyList(),
    @SerializedName("customer_licenses") val customerLicenses: List<CustomerLicense> = emptyList(),
    @SerializedName("main_licenses") val mainLicenses: List<CustomerLicense> = emptyList(),
    @SerializedName("licences_grouped") val licencesGrouped: List<LicenceGroup> = emptyList(),
    @SerializedName("quotes", alternate = ["quotations", "quote_list"]) val quotes: List<QuoteDetails> = emptyList(),
    @SerializedName("proformas", alternate = ["proforma_invoices", "proforma", "proforma_list"]) val proformas: List<ProformaInvoice> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val payments: List<Payment> = emptyList(),
    @SerializedName("works", alternate = ["work_list", "active_works", "work", "projects", "lead_works"]) val works: List<Work> = emptyList(),
    val stats: Map<String, Any>? = null,
    val employees: List<Employee> = emptyList()
) {
    fun hasApprovedQuote(): Boolean {
        return quotes.any { q ->
            val st = q.status?.lowercase()?.trim() ?: ""
            st == "approved" || st == "accepted" || st == "won"
        }
    }

    fun hasApprovedProforma(): Boolean {
        return proformas.any { pf ->
            val st = pf.status?.lowercase()?.trim() ?: ""
            st == "approved" || st == "accepted" || st == "won"
        }
    }

    companion object {
        private val lenientGson = RetrofitClient.gson

        fun fromJson(json: JsonElement): LeadDetailsResponse {
            return try {
                // If it's an array with one element, unwrap it (some PHP proxies do this)
                val element = if (json.isJsonArray && json.asJsonArray.size() > 0) {
                    json.asJsonArray[0]
                } else {
                    json
                }

                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val dataPart = if (obj.has("data") && obj.get("data").isJsonObject) {
                        obj.get("data").asJsonObject
                    } else {
                        obj
                    }
                    
                    val parsed = lenientGson.fromJson(dataPart, LeadDetailsResponse::class.java)
                    
                    val rootStatus = obj.get("status")?.takeIf { it.isJsonPrimitive }?.asString
                    val rootMessage = obj.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                    
                    var finalProformas = parsed.proformas
                    if (finalProformas.isEmpty()) {
                        val pfArr = if (dataPart.has("proforma_invoices") && dataPart.get("proforma_invoices").isJsonArray) dataPart.getAsJsonArray("proforma_invoices")
                            else if (dataPart.has("proformas") && dataPart.get("proformas").isJsonArray) dataPart.getAsJsonArray("proformas")
                            else null
                        if (pfArr != null) {
                            try {
                                finalProformas = lenientGson.fromJson(pfArr, object : com.google.gson.reflect.TypeToken<List<ProformaInvoice>>() {}.type) ?: emptyList()
                            } catch (_: Exception) {}
                        }
                    }

                    var finalQuotes = parsed.quotes
                    if (finalQuotes.isEmpty()) {
                        val qArr = if (dataPart.has("quotations") && dataPart.get("quotations").isJsonArray) dataPart.getAsJsonArray("quotations")
                            else if (dataPart.has("quotes") && dataPart.get("quotes").isJsonArray) dataPart.getAsJsonArray("quotes")
                            else null
                        if (qArr != null) {
                            try {
                                finalQuotes = lenientGson.fromJson(qArr, object : com.google.gson.reflect.TypeToken<List<QuoteDetails>>() {}.type) ?: emptyList()
                            } catch (_: Exception) {}
                        }
                    }

                    var finalWorks = parsed.works
                    if (finalWorks.isEmpty()) {
                        val wArr = if (dataPart.has("works") && dataPart.get("works").isJsonArray) dataPart.getAsJsonArray("works")
                            else if (dataPart.has("active_works") && dataPart.get("active_works").isJsonArray) dataPart.getAsJsonArray("active_works")
                            else if (dataPart.has("work_list") && dataPart.get("work_list").isJsonArray) dataPart.getAsJsonArray("work_list")
                            else if (dataPart.has("projects") && dataPart.get("projects").isJsonArray) dataPart.getAsJsonArray("projects")
                            else null
                        if (wArr != null) {
                            try {
                                finalWorks = lenientGson.fromJson(wArr, object : TypeToken<List<Work>>() {}.type) ?: emptyList()
                            } catch (_: Exception) {}
                        }
                    }

                    parsed.copy(
                        status = if (parsed.status.isNullOrEmpty()) rootStatus else parsed.status,
                        message = if (parsed.message.isNullOrEmpty()) rootMessage else parsed.message,
                        proformas = finalProformas,
                        quotes = finalQuotes,
                        works = finalWorks
                    )
                } else {
                    LeadDetailsResponse(status = "error", message = "Not a JSON object")
                }
            } catch (e: Exception) {
                android.util.Log.e("GSON", "LeadDetailsResponse parse error: ${e.message}", e)
                LeadDetailsResponse(status = "error", message = "Parse error: ${e.message}")
            }
        }
    }
}

data class LeadCounts(
    val all: Int = 0,
    val new: Int = 0,
    val contacted: Int = 0,
    val qualified: Int = 0,
    val proposal: Int = 0,
    val won: Int = 0,
    val lost: Int = 0
)

data class LeadResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val total: Int? = null,
    val counts: LeadCounts? = null,
    @SerializedName("leads", alternate = ["data"]) val leads: List<Lead> = emptyList()
) {
    companion object {
        private val lenientGson = RetrofitClient.gson

        fun fromJson(json: JsonElement): LeadResponse {
            return try {
                if (json.isJsonArray) {
                    val list = lenientGson.fromJson<List<Lead>>(json, object : com.google.gson.reflect.TypeToken<List<Lead>>() {}.type)
                    LeadResponse(status = "success", leads = list)
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    // If the object itself is the wrapper (status, leads)
                    if (obj.has("leads") || obj.has("data") || obj.has("status")) {
                        lenientGson.fromJson(json, LeadResponse::class.java)
                    } else {
                        // Unwrapped object? Unlikely for leads but for safety
                        LeadResponse(status = "success", leads = emptyList())
                    }
                } else {
                    LeadResponse(status = "error", leads = emptyList())
                }
            } catch (e: Exception) { 
                android.util.Log.e("GSON", "Parse error", e)
                LeadResponse(status = "error", leads = emptyList()) 
            }
        }
    }
}

data class Invoice(
    val id: Int,
    val invoice_no: String,
    val lead_id: Int,
    val customer_name: String,
    val amount: String,
    val tax: String,
    val discount: String? = "0.00",
    val total: String,
    val status: String,
    val invoice_date: String,
    val due_date: String? = null,
    val created_at: String
)

data class AdminDashboardResponse(
    val success: Boolean = false,
    val status: String? = null,
    val stats: AdminStats? = null,
    val employees_attendance: List<EmployeeAttendance>? = emptyList(),
    val upcoming_birthdays: List<Employee>? = emptyList(),
    val departments: List<Department>? = emptyList(),
    val pipeline: PipelineData? = null
) {
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): AdminDashboardResponse {
            return try {
                lenientGson.fromJson(json, AdminDashboardResponse::class.java)
            } catch (e: Exception) { AdminDashboardResponse(status = "error") }
        }
    }
}

data class AdminCustomersResponse(
    @SerializedName("success") val success_raw: Any? = null,
    val status: String? = null,
    val error: String? = null,
    val customers: List<Customer>? = emptyList()
) {
    val success: Boolean get() = when(success_raw) {
        is Boolean -> success_raw
        is String -> success_raw == "true" || success_raw == "success"
        is Number -> success_raw.toInt() == 1
        else -> status == "success" || status == "true"
    }
    companion object {
        private val lenientGson = RetrofitClient.gson
        fun fromJson(json: JsonElement): AdminCustomersResponse {
            return try {
                lenientGson.fromJson(json, AdminCustomersResponse::class.java)
            } catch (e: Exception) { AdminCustomersResponse() }
        }
    }
}

data class PipelineData(
    val stages: List<PipelineStage>? = emptyList(),
    val conversion_rate: Double = 0.0
)

data class PipelineStage(
    val label: String,
    val count: Int,
    val pct: Double,
    val color: String
)

data class Department(
    val id: Int,
    val name: String? = null
)

data class AdminStats(
    val total_leads: Int = 0,
    val converted_leads: Int = 0,
    val total_revenue: String = "0.00",
    val total_received: String = "0.00",
    val balance_amount: String = "0.00",
    val tally_sales: String = "0.00",
    val total_quotes: String = "0.00",
    val total_employees: Int = 0,
    val present_today: Int = 0,
    val leave_today: Int = 0,
    val absent_today: Int = 0,
    val departments: Int = 0
)

data class EmployeeAttendance(
    val id: Int,
    val name: String,
    val role: String? = null,
    val status: String? = null,
    val check_in: String? = null,
    val check_out: String? = null,
    val profile_image: String? = null,
    val department_name: String? = null
)
