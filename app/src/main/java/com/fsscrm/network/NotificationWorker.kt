package com.fsscrm.network

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fsscrm.ui.common.showLocalNotification
import com.fsscrm.ui.common.toLenientJson
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        Log.d("NotificationWorker", "Work started at ${Date()}")
        val sessionManager = com.fsscrm.network.SessionManager(ctx)
        val userId = sessionManager.getUserId()
        
        if (userId == 0) return Result.success()

        val prefs = ctx.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        try {
            // 1. Fetch Dashboard Data for Stats and Profile
            val dashboardResp = RetrofitClient.apiService.getDashboardData(mapOf("user_id" to userId))
            val dashboardData = dashboardResp.toLenientJson()?.let { DashboardResponse.fromJson(it) }
            if (dashboardResp.isSuccessful && dashboardData != null) {
                val userName = dashboardData.profile?.name ?: dashboardData.rootName ?: "User"
                val stats = dashboardData.getEffectiveStats()

                // Check for New Leads/Tasks
                val lastLeadCount = prefs.getInt("last_lead_count", -1)
                val lastTaskCount = prefs.getInt("last_task_count", -1)

                val currentLeadsCount = stats.totalLeads
                if (lastLeadCount != -1 && currentLeadsCount > lastLeadCount) {
                    val newLeads = currentLeadsCount - lastLeadCount
                    val latestLead = dashboardData.recentLeads.firstOrNull()
                    val msg = if (latestLead != null) {
                        "New Lead: ${latestLead.name} from ${latestLead.company_name ?: "Unknown"}. Plus ${newLeads-1} more."
                    } else "You have $newLeads new lead(s) assigned."
                    showLocalNotification(ctx, "New Lead Assigned", msg, "leads")
                }
                
                val currentTasksCount = stats.totalTasks
                if (lastTaskCount != -1 && currentTasksCount > lastTaskCount) {
                    val latestTask = dashboardData.tasks.firstOrNull()
                    val msg = if (latestTask != null) "Task: ${latestTask.name}. Due: ${latestTask.dueDate}" else "A new task has been assigned to you."
                    showLocalNotification(ctx, "New Task Assigned", msg, "tasks")
                }

                prefs.edit().putInt("last_lead_count", currentLeadsCount).putInt("last_task_count", currentTasksCount).apply()


                // 2. Attendance Nudges
                val currentAttendance = dashboardData.getEffectiveAttendance()
                
                // 9:30 AM Check-in
                if (hour == 9 && minute >= 30 && prefs.getString("last_checkin_nudge", "") != todayStr) {
                    if (currentAttendance?.isCheckedIn != true) {
                        showLocalNotification(ctx, "Good Morning $userName", "It's 9:30 AM. Don't forget to Check-in for today!", "attendance")
                        prefs.edit().putString("last_checkin_nudge", todayStr).apply()
                    }
                }
                // 6:30 PM Check-out
                if (hour == 18 && minute >= 30 && prefs.getString("last_checkout_nudge", "") != todayStr) {
                    if (currentAttendance?.isCheckedIn == true && currentAttendance.isCheckedOut != true) {
                        showLocalNotification(ctx, "Work Day Ending", "Good evening $userName! Remember to Check-out before leaving.", "attendance")
                        prefs.edit().putString("last_checkout_nudge", todayStr).apply()
                    }
                }
            }

            // 3. Morning Summary & Reminders (8:00 AM)
            if (hour == 8 && prefs.getString("last_morning_greet", "") != todayStr) {
                val followUpResp = RetrofitClient.apiService.getTodayFollowUps(mapOf("user_id" to userId))
                val followUpData = followUpResp.toLenientJson()?.let { FollowUpResponse.fromJson(it) }
                if (followUpResp.isSuccessful && followUpData != null) {
                    val todayFollows = followUpData.followups
                    
                    if (todayFollows.isNotEmpty()) {
                        val first = todayFollows.first()
                        val msg = "Good Morning! You have ${todayFollows.size} follow-ups today. First one is at ${first.follow_up_time} with ${first.lead_name}."
                        showLocalNotification(ctx, "Today's Schedule", msg, "followups")
                    } else {
                        showLocalNotification(ctx, "Good Morning!", "Have a productive day! You have no follow-ups scheduled for today.", "home")
                    }
                    prefs.edit().putString("last_morning_greet", todayStr).apply()
                }
            }

            // 4. Pre-Follow-up Reminders (30 mins before)
            val followUpResp = RetrofitClient.apiService.getTodayFollowUps(mapOf("user_id" to userId))
            val followUpResponse = followUpResp.toLenientJson()?.let { FollowUpResponse.fromJson(it) }
            if (followUpResp.isSuccessful && followUpResponse != null) {
                val now = calendar.timeInMillis
                
                followUpResponse.followups.forEach { follow ->
                    val followTime = parseFollowUpTime(follow.follow_up_date, follow.follow_up_time)
                    if (followTime != null) {
                        val diffMins = (followTime - now) / (1000 * 60)
                        val reminderId = "reminder_${follow.id}"
                        
                        // Remind if follow-up is in 25-35 minutes and not already reminded
                        if (diffMins in 25..35 && !prefs.getBoolean(reminderId, false)) {
                            showLocalNotification(
                                ctx, 
                                "Upcoming Follow-up", 
                                "Reminder: Follow-up with ${follow.lead_name} at ${follow.follow_up_time} in about 30 minutes.", 
                                "lead_details", 
                                follow.lead_id.toString()
                            )
                            prefs.edit().putBoolean(reminderId, true).apply()
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("NotificationWorker", "Main loop error", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun parseFollowUpTime(dateStr: String, timeStr: String?): Long? {
        if (timeStr == null) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            format.parse("$dateStr $timeStr")?.time
        } catch (e: Exception) {
            try {
                val format2 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                format2.parse("$dateStr $timeStr")?.time
            } catch (e2: Exception) {
                Log.w("NotificationWorker", "Failed to parse follow-up time: $dateStr $timeStr")
                null
            }
        }
    }
}
