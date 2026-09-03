package com.fsscrm.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.fsscrm.R
import com.google.gson.Gson

data class QuoteSettings(
    val companyName: String = "Friends Software Solutions",
    val companyEmail: String = "info@friendssoftwaresolutions.in",
    val companyPhone: String = "",
    val companyFax: String = "",
    val companyAddress: String = "Hyderabad, Telangana",
    val companyWebsite: String = "v.friendssoftwaresolutions.in",
    val logoUri: String = "",
    val startQuoteNumber: Int = 0,
    val validityDays: Int = 30,
    val currency: String = "Indian Rupee",
    val includeTax: Boolean = true,
    val includeTotalPrice: Boolean = true,
    val taxRate: Double = 18.0,
    val showCustomerDetails: Boolean = true,
    val footerText: String = ""
)

object QuoteSettingsManager {
    private const val PREFS_NAME = "quote_settings_prefs"
    private const val KEY_SETTINGS = "quote_settings"

    fun saveSettings(context: android.content.Context, settings: QuoteSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SETTINGS, Gson().toJson(settings)).apply()
    }

    fun loadSettings(context: android.content.Context): QuoteSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                Gson().fromJson(json, QuoteSettings::class.java)
            } catch (e: Exception) {
                QuoteSettings()
            }
        } else {
            QuoteSettings()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(QuoteSettingsManager.loadSettings(context)) }
    var currentSubScreen by remember { mutableStateOf("main") }

    if (currentSubScreen == "business") {
        BusinessDetailsScreen(
            settings = settings,
            onSettingsChange = { settings = it },
            onBack = { currentSubScreen = "main" }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            QuoteSettingsManager.saveSettings(context, settings)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF8F9FB)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(Color.White)
            ) {
                SettingsListItem(
                    title = "Logo & Business Details",
                    subtitle = "Include your logo & business details in quotes",
                    onClick = { currentSubScreen = "business" }
                )
                SettingsListItem(
                    title = "Start Quotes Number",
                    subtitle = settings.startQuoteNumber.toString(),
                    onClick = { /* Open picker */ }
                )
                SettingsListItem(
                    title = "Quotes Validity",
                    subtitle = "${settings.validityDays} days",
                    onClick = { /* Open picker */ }
                )
                SettingsListItem(
                    title = "Currency",
                    subtitle = settings.currency,
                    onClick = { /* Open picker */ }
                )

                Spacer(Modifier.height(16.dp))

                SettingsToggleItem(
                    title = "Include Tax",
                    checked = settings.includeTax,
                    onCheckedChange = { settings = settings.copy(includeTax = it) }
                )
                SettingsToggleItem(
                    title = "Include total price",
                    checked = settings.includeTotalPrice,
                    onCheckedChange = { settings = settings.copy(includeTotalPrice = it) }
                )
                SettingsListItem(
                    title = "Tax Rate",
                    subtitle = "${settings.taxRate}%",
                    onClick = { /* Open picker */ }
                )
                SettingsListItem(
                    title = "Show Customer Details",
                    subtitle = "Email, Fax, Address, Phone",
                    onClick = { /* Open picker */ }
                )
                SettingsListItem(
                    title = "Footer Text",
                    subtitle = settings.footerText.ifBlank { "None" },
                    onClick = { /* Open editor */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailsScreen(
    settings: QuoteSettings,
    onSettingsChange: (QuoteSettings) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Business Details",
                onBackClick = onBack
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    onSettingsChange(settings.copy(logoUri = uri.toString()))
                }
            }

            // Logo Row
            BusinessFieldRow(
                label = "Logo",
                value = if (settings.logoUri.isEmpty()) "" else "Logo Selected",
                onClear = { onSettingsChange(settings.copy(logoUri = "")) },
                leading = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings.logoUri.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(settings.logoUri),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.hand),
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            )

            BusinessTextFieldRow("Company Name", settings.companyName, { onSettingsChange(settings.copy(companyName = it)) })
            BusinessTextFieldRow("Description", "", { }) // Placeholder for description
            BusinessTextFieldRow("Phone", settings.companyPhone, { onSettingsChange(settings.copy(companyPhone = it)) })
            BusinessTextFieldRow("Fax", settings.companyFax, { onSettingsChange(settings.copy(companyFax = it)) })
            BusinessTextFieldRow("Email", settings.companyEmail, { onSettingsChange(settings.copy(companyEmail = it)) })
            BusinessTextFieldRow("Address", settings.companyAddress, { onSettingsChange(settings.copy(companyAddress = it)) })
            BusinessTextFieldRow("Website", settings.companyWebsite, { onSettingsChange(settings.copy(companyWebsite = it)) })

            Spacer(Modifier.height(16.dp))
            Text(
                "More details",
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsListItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}

@Composable
fun SettingsToggleItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF7C4DFF),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.7f)
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}

@Composable
fun BusinessFieldRow(
    label: String,
    value: String,
    onClear: () -> Unit,
    leading: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Text(
            label,
            modifier = Modifier.width(80.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = Color.Black
        )
        IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(18.dp))
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFEEEEEE))
}

@Composable
fun BusinessTextFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(80.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
        Spacer(Modifier.width(8.dp))
        VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFFEEEEEE))
        Spacer(Modifier.width(12.dp))
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(label, fontSize = 14.sp, color = Color.LightGray)
                }
                innerTextField()
            }
        )
        
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFEEEEEE))
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color = Color.LightGray) {
    Box(modifier.width(1.dp).background(color))
}
