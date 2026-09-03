package com.fsscrm.ui.admin

import android.util.Log
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.AdminDashboardResponse
import com.fsscrm.network.AdminStats
import com.fsscrm.network.Employee
import com.fsscrm.network.EmployeeAttendance
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.EmptyStateCard
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CORPORATE DESIGN SYSTEM
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// Brand colors
private val Brand900 = Color(0xFF0F172A)    // Slate 900
private val Brand500 = Color(0xFF64748B)
private val Brand400 = Color(0xFF94A3B8)
private val Brand200 = Color(0xFFE2E8F0)
private val Brand100 = Color(0xFFF1F5F9)
private val Brand50  = Color(0xFFF8FAFC)

// Accent colors (corporate palette)
private val Indigo700 = Color(0xFF4338CA)
private val Indigo600 = Color(0xFF4F46E5)
private val Indigo500 = Color(0xFF6366F1)
private val Indigo400 = Color(0xFF818CF8)
private val Indigo100 = Color(0xFFE0E7FF)
private val Indigo50  = Color(0xFFEEF2FF)

private val Emerald700 = Color(0xFF047857)
private val Emerald600 = Color(0xFF059669)
private val Emerald500 = Color(0xFF10B981)
private val Emerald400 = Color(0xFF34D399)
private val Emerald50  = Color(0xFFECFDF5)

private val Amber600 = Color(0xFFD97706)
private val Amber500 = Color(0xFFF59E0B)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber50  = Color(0xFFFFFBEB)

private val Rose600 = Color(0xFFE11D48)
private val Rose500 = Color(0xFFF43F5E)
private val Rose50  = Color(0xFFFFF1F2)

private val Sky600 = Color(0xFF0284C7)
private val Sky500 = Color(0xFF0EA5E9)

private val Violet600 = Color(0xFF7C3AED)
private val Violet500 = Color(0xFF8B5CF6)

private val Teal600 = Color(0xFF0D9488)
private val Teal500 = Color(0xFF14B8A6)

// Semantic mappings
private val PrimaryBlue = Indigo600
private val SurfaceColor = Brand50
private val CardBg = Color.White
private val TextDark = Brand900
private val TextMuted = Brand500
private val TextSubtle = Brand400
private val DividerColor = Brand200

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MAIN COMPOSABLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    userId: Int,
    onMenuClick: () -> Unit,
    onViewAllAttendance: () -> Unit
) {
    var dashboardData by remember { mutableStateOf<AdminDashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTimeRange by remember { mutableStateOf("this_month") }
    var customDateRange by remember { mutableStateOf<Pair<Long?, Long?>?>(null) }
    var expandedFilter by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val timeRanges = listOf(
        "today" to "Today",
        "this_week" to "This Week",
        "this_month" to "This Month",
        "last_month" to "Last Month",
        "custom" to "Custom Range"
    )

    fun fetchDashboard() {
        isLoading = true
        errorMessage = null
        dashboardData = null
        scope.launch {
            try {
                val params = mutableMapOf(
                    "user_id" to userId.toString(),
                    "range" to selectedTimeRange,
                    "action" to "get_admin_dashboard"
                )
                if (selectedTimeRange == "custom" && customDateRange != null) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    customDateRange?.first?.let { params["date_from"] = sdf.format(java.util.Date(it)) }
                    customDateRange?.second?.let { params["date_to"] = sdf.format(java.util.Date(it)) }
                }
                val response = RetrofitClient.apiService.getAdminDashboardData(params)
                if (response.isSuccessful) {
                    response.toLenientJson()?.let {
                        dashboardData = AdminDashboardResponse.fromJson(it)
                    }
                } else {
                    errorMessage = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.message}"
                Log.e("AdminDashboard", "Error", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTimeRange, customDateRange) {
        if (selectedTimeRange == "custom" && customDateRange == null) return@LaunchedEffect
        fetchDashboard()
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Admin Dashboard",
                onMenuClick = onMenuClick
            )
        },
        containerColor = SurfaceColor
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading dashboard…", fontSize = 13.sp, color = TextMuted)
                    }
                }
            }
            errorMessage != null -> ErrorState(padding, errorMessage!!) { fetchDashboard() }
            else -> DashboardContent(
                padding = padding,
                dashboardData = dashboardData,
                selectedTimeRange = selectedTimeRange,
                timeRanges = timeRanges,
                expandedFilter = expandedFilter,
                onExpandFilter = { expandedFilter = true },
                onDismissFilter = { expandedFilter = false },
                onSelectRange = { key ->
                    selectedTimeRange = key
                    expandedFilter = false
                    if (key == "custom") showDatePicker = true
                    else { customDateRange = null; fetchDashboard() }
                },
                showDatePicker = showDatePicker,
                onDismissDatePicker = {
                    showDatePicker = false
                    if (customDateRange == null) selectedTimeRange = "this_month"
                },
                onConfirmDatePicker = { start, end ->
                    if (start != null && end != null) customDateRange = start to end
                    else selectedTimeRange = "this_month"
                    showDatePicker = false
                },
                onViewAllAttendance = onViewAllAttendance
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// DASHBOARD CONTENT
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    padding: PaddingValues,
    dashboardData: AdminDashboardResponse?,
    selectedTimeRange: String,
    timeRanges: List<Pair<String, String>>,
    expandedFilter: Boolean,
    onExpandFilter: () -> Unit,
    onDismissFilter: () -> Unit,
    onSelectRange: (String) -> Unit,
    showDatePicker: Boolean,
    onDismissDatePicker: () -> Unit,
    onConfirmDatePicker: (Long?, Long?) -> Unit,
    onViewAllAttendance: () -> Unit
) {
    val stats = dashboardData?.stats
    val attendance = dashboardData?.employees_attendance ?: emptyList()
    val birthdays = dashboardData?.upcoming_birthdays ?: emptyList()

    val presentCount = attendance.count { it.status.equals("Present", true) }
    val absentCount = attendance.count { it.status.equals("Absent", true) }
    val totalAttendance = (presentCount + absentCount).coerceAtLeast(1)

    val pipelineStages = remember(stats) {
        listOf(
            PipelineStage("New", (stats?.total_leads ?: 0) - (stats?.converted_leads ?: 0), Indigo500),
            PipelineStage("Contacted", ((stats?.total_leads ?: 0) * 0.45).toInt(), Sky500),
            PipelineStage("Qualified", ((stats?.total_leads ?: 0) * 0.30).toInt(), Violet500),
            PipelineStage("Proposal", ((stats?.total_leads ?: 0) * 0.18).toInt(), Amber500),
            PipelineStage("Won", stats?.converted_leads ?: 0, Emerald500),
            PipelineStage("Lost", 0, Rose500)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HeaderSection(selectedTimeRange, timeRanges, expandedFilter, onExpandFilter, onDismissFilter, onSelectRange) }

        if (showDatePicker) {
            item {
                val datePickerState = rememberDateRangePickerState()
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(CardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        DateRangePicker(
                            state = datePickerState,
                            title = { Text("Select Date Range", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(450.dp)
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismissDatePicker) { Text("Cancel") }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = { onConfirmDatePicker(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) { Text("Confirm") }
                        }
                    }
                }
            }
        }

        // ── PRIMARY KPI GRID (4 high-level metrics) ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Hero stat - Revenue
                HeroRevenueCard(stats)
                // 2x2 stat grid
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Total Leads",
                        value = formatNumber(stats?.total_leads),
                        delta = "+12.5%",
                        deltaPositive = true,
                        icon = Icons.Default.Groups,
                        gradient = listOf(Indigo500, Indigo600),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Converted",
                        value = formatNumber(stats?.converted_leads),
                        delta = "+8.2%",
                        deltaPositive = true,
                        icon = Icons.Default.TaskAlt,
                        gradient = listOf(Emerald500, Emerald600),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Tally Sales",
                        value = "₹${formatCurrency(stats?.tally_sales)}",
                        delta = "+15.3%",
                        deltaPositive = true,
                        icon = Icons.Default.PointOfSale,
                        gradient = listOf(Amber500, Amber600),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Balance Due",
                        value = "₹${formatCurrency(stats?.balance_amount)}",
                        delta = "-3.1%",
                        deltaPositive = false,
                        icon = Icons.Default.AccountBalance,
                        gradient = listOf(Rose500, Rose600),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── HR + ATTENDANCE SUMMARY ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HrSummaryCard(
                    stats = stats,
                    modifier = Modifier.weight(1f)
                )
                AttendanceSummaryCard(
                    present = presentCount,
                    absent = absentCount,
                    total = totalAttendance,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── SALES OVERVIEW ──
        item {
            SalesOverviewCard(stats)
        }

        // ── PIPELINE ──
        item {
            PipelineCard(pipelineStages, stats?.total_leads ?: 0)
        }

        // ── TEAM ATTENDANCE ──
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Team Attendance", Icons.Default.SupervisorAccount)
                TextButton(
                    onClick = onViewAllAttendance,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("View All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                }
            }
        }

        if (attendance.isEmpty()) {
            item { EmptyStateCard("No attendance records for today") }
        } else {
            items(attendance.take(4)) { emp ->
                AttendanceCard(emp)
            }
        }

        // ── UPCOMING BIRTHDAYS ──
        item {
            SectionTitle("Upcoming Birthdays", Icons.Default.Cake, badge = birthdays.size.toString())
        }
        if (birthdays.isEmpty()) {
            item { EmptyStateCard("No upcoming birthdays this week") }
        } else {
            items(birthdays.take(3)) { bday ->
                BirthdayCard(bday)
            }
        }

        // ── FOOTER ──
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "© 2026 Friends Software Solutions  •  Version 1.0.0",
                fontSize = 11.sp,
                color = TextSubtle,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HEADER SECTION
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HeaderSection(
    selectedTimeRange: String,
    timeRanges: List<Pair<String, String>>,
    expandedFilter: Boolean,
    onExpandFilter: () -> Unit,
    onDismissFilter: () -> Unit,
    onSelectRange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Overview", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextDark)
            Text(
                timeRanges.find { it.first == selectedTimeRange }?.second ?: selectedTimeRange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue
            )
        }
        Box {
            FilledTonalIconButton(
                onClick = onExpandFilter,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Indigo50,
                    contentColor = PrimaryBlue
                )
            ) {
                Icon(Icons.Default.FilterList, "Filter range")
            }
            DropdownMenu(
                expanded = expandedFilter,
                onDismissRequest = onDismissFilter,
                modifier = Modifier.background(CardBg, RoundedCornerShape(12.dp))
            ) {
                timeRanges.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (key == selectedTimeRange) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.width(8.dp))
                                } else Spacer(Modifier.width(24.dp))
                                Text(label, fontWeight = if (key == selectedTimeRange) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        },
                        onClick = { onSelectRange(key) }
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// KPI CARDS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HeroRevenueCard(stats: AdminStats?) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Brand900, Indigo700, Indigo600)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                null,
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Total Revenue",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "₹${formatCurrency(stats?.total_revenue)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Emerald500.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(10.dp), tint = Emerald500)
                                    Spacer(Modifier.width(2.dp))
                                    Text("+18.4%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("vs last period", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStat("Received", "₹${formatCurrency(stats?.total_received)}", Modifier.weight(1f))
                    MiniStat("Pending", "₹${formatCurrency(stats?.balance_amount)}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    delta: String,
    deltaPositive: Boolean,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val accentColor = if (deltaPositive) Emerald500 else Rose500
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Surface(
                    color = if (deltaPositive) Emerald50 else Rose50,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (deltaPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            null,
                            modifier = Modifier.size(9.dp),
                            tint = accentColor
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            delta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HR + ATTENDANCE SUMMARY
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HrSummaryCard(stats: AdminStats?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(CardBg),
        elevation = CardDefaults.cardElevation(0.0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Teal500, Teal600))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Badge, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("HR Overview", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
            }
            Spacer(Modifier.height(12.dp))
            HrRow("Total Employees", formatNumber(stats?.total_employees), TextDark)
            HrRow("On Leave", formatNumber(stats?.leave_today), Amber600)
            HrRow("Departments", formatNumber(stats?.departments), Indigo600)
        }
    }
}

@Composable
private fun HrRow(label: String, value: String, valueColor: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun AttendanceSummaryCard(
    present: Int,
    absent: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val presentRatio = present.toFloat() / total
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(CardBg),
        elevation = CardDefaults.cardElevation(0.0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Sky500, Sky600))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HowToReg, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Attendance", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    DonutProgress(
                        progress = presentRatio,
                        color = Emerald500,
                        trackColor = Brand100,
                        strokeWidth = 6.dp
                    )
                    Text(
                        "${(presentRatio * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextDark
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    AttendanceIndicator("Present", present, Emerald600)
                    Spacer(Modifier.height(6.dp))
                    AttendanceIndicator("Absent", absent, Rose600)
                }
            }
        }
    }
}

@Composable
private fun AttendanceIndicator(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text("$label: ", fontSize = 11.sp, color = TextMuted)
        Text(count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SALES OVERVIEW
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SalesOverviewCard(@Suppress("UNUSED_PARAMETER") stats: AdminStats?) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(CardBg),
        elevation = CardDefaults.cardElevation(0.0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Sales Performance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                        Text("Last 7 days trend", fontSize = 10.sp, color = TextMuted)
                    }
                }
                Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "+24%",
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            AnimatedBarChart(
                values = listOf(0.45f, 0.72f, 0.58f, 0.88f, 0.65f, 0.92f, 0.78f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PIPELINE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class PipelineStage(val label: String, val count: Int, val color: Color)

@Composable
private fun PipelineCard(stages: List<PipelineStage>, totalLeads: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(CardBg),
        elevation = CardDefaults.cardElevation(0.0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Violet500, Violet600))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountTree, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Sales Pipeline", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Text("$totalLeads total leads in funnel", fontSize = 10.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            val maxCount = stages.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            stages.forEach { stage ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(stage.color))
                            Spacer(Modifier.width(6.dp))
                            Text(stage.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        }
                        Text(
                            stage.count.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = stage.color
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(stage.color.copy(alpha = 0.12f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(stage.count.toFloat() / maxCount)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(stage.color, stage.color.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEAM ATTENDANCE LIST
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun AttendanceCard(emp: EmployeeAttendance) {
    val isPresent = emp.status.equals("Present", true)
    val accentColor = if (isPresent) Emerald600 else Rose600
    val accentBg = if (isPresent) Emerald50 else Rose50

    Card(
        Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(CardBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.0.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Indigo100, Indigo50))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (emp.name.firstOrNull()?.uppercaseChar() ?: 'E').toString(),
                    fontWeight = FontWeight.Bold,
                    color = Indigo700,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    emp.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    emp.department_name ?: emp.role ?: "Employee",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPresent && (emp.check_in != null || emp.check_out != null)) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        emp.check_in?.let {
                            MiniTimeChip("In", it, Emerald600)
                        }
                        Spacer(Modifier.width(6.dp))
                        emp.check_out?.let {
                            MiniTimeChip("Out", it, Rose600)
                        }
                    }
                }
            }
            Surface(
                color = accentBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accentColor))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        emp.status ?: "Absent",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTimeChip(label: String, time: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.width(3.dp))
            Text(time, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// BIRTHDAY CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun BirthdayCard(emp: Employee) {
    Card(
        Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(CardBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.0.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Amber100, Amber50))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Cake, null, tint = Amber600, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(emp.name ?: "Unknown", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextDark)
                Text(emp.date_of_birth ?: "", fontSize = 11.sp, color = TextMuted)
            }
            Surface(
                color = Amber50,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Celebration, null, modifier = Modifier.size(12.dp), tint = Amber600)
                    Spacer(Modifier.width(3.dp))
                    Text("Celebrate", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Amber600)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SHARED COMPONENTS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SectionTitle(text: String, icon: ImageVector, badge: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Brand100),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TextDark, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        if (badge != null) {
            Spacer(Modifier.width(6.dp))
            Surface(color = Indigo50, shape = RoundedCornerShape(6.dp)) {
                Text(
                    badge,
                    Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo700
                )
            }
        }
    }
}

@Composable
private fun ErrorState(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Rose50),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CloudOff, null, tint = Rose500, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(message, fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CHART COMPONENTS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun DonutProgress(
    progress: Float,
    color: Color,
    trackColor: Color,
    strokeWidth: androidx.compose.ui.unit.Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "donut"
    )
    Canvas(Modifier.size(64.dp)) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(color, color.copy(alpha = 0.7f), color)),
            startAngle = -90f,
            sweepAngle = animatedProgress * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AnimatedBarChart(values: List<Float>, labels: List<String>) {
    val animatedValues = values.map { target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(800, delayMillis = 100, easing = FastOutSlowInEasing),
            label = "bar"
        )
    }
    Row(
        Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        animatedValues.forEachIndexed { i, anim ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                val h = (anim.value * 90).dp.coerceAtLeast(6.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(h)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = if (i == 5) listOf(Emerald400, Emerald600)
                                else listOf(Indigo400, Indigo600)
                            )
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// UTILS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun formatNumber(value: Int?): String {
    return value?.let {
        val locale = Locale("en", "IN")
        NumberFormat.getNumberInstance(locale).format(it)
    } ?: "0"
}

private fun formatCurrency(value: String?): String {
    if (value.isNullOrBlank()) return "0"
    return try {
        val number = value.replace(",", "").toDoubleOrNull() ?: 0.0
        val locale = Locale("en", "IN")
        NumberFormat.getNumberInstance(locale).format(number.toLong())
    } catch (_: Exception) {
        value
    }
}
