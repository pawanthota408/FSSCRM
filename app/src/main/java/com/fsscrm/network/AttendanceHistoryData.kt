package com.fsscrm.network

import com.google.gson.annotations.SerializedName

data class AttendanceHistoryResponse(
    val status: String,
    val summary: AttendanceSummary,
    val history: List<AttendanceHistoryItem>
)

data class AttendanceSummary(
    @SerializedName("present", alternate = ["present_count"]) val present: Int,
    @SerializedName("absent", alternate = ["absent_count"]) val absent: Int,
    @SerializedName("leaves", alternate = ["leave_count", "total_leaves", "leaves_count"]) val leaves: Int
)

data class AttendanceHistoryItem(
    val date: String,
    val day: String,
    val status: String,
    val check_in: String,
    val check_out: String,
    val working_hours: String,
    val is_verified: Boolean
)
