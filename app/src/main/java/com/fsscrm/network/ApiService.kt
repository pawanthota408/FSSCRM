package com.fsscrm.network

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query
import retrofit2.http.QueryMap

// ==================== MODELS ====================

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val token: String? = null,
    val user_id: Int? = null,
    val name: String? = null,
    val role: String? = null,
    val error: String? = null,
)

// ✅ NEW: Added for Work Details
data class WorkDetailsRequest(
    val user_id: Int,
    val work_id: Int
)

data class CompleteWorkRequest(
    val user_id: Int,
    val work_id: Int,
    val serial_number: String,
    val company: String,
    val completion_date: String,
    val verification_notes: String,
    val expiry_dates: String, // JSON string or Map
    val customer_email: String,
    val customer_password: String
)

data class CustomerDetailsRequest(
    val user_id: Int,
    val customer_id: Int,
    val include_works: Boolean = true,
    val include_licenses: Boolean = true,
    val include_payments: Boolean = true,
    val include_history: Boolean = true
)

data class UpdateResponse(
    val status: String? = null,
    val latest_version: String? = null,
    val version_code: Int? = null,
    val update_url: String? = null,
    val release_notes: String? = null,
    val force_update: Boolean? = false
)

// ==================== API SERVICE ====================

interface ApiService {

    // ----- AUTH -----
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<JsonElement>

    @GET("get_profile.php")
    suspend fun getProfile(@Query("user_id") userId: Int): Response<ProfileResponse>

    @POST("update_profile.php")
    suspend fun updateProfile(@Body request: Map<String, String>): Response<JsonElement>

    @POST("update_fcm_token.php")
    suspend fun updateFcmToken(@Body request: Map<String, String>): Response<JsonElement>

    // ----- DASHBOARD -----
    @POST("get_dashboard.php")
    suspend fun getDashboardData(@Body request: Map<String, @JvmSuppressWildcards Any>): Response<okhttp3.ResponseBody>

    // ----- LEADS -----
    @POST("get_leads.php")
    suspend fun getLeads(@Body request: Map<String, @JvmSuppressWildcards Any>): Response<okhttp3.ResponseBody>

    @GET("admin_leads.php")
    suspend fun getAdminLeads(
        @Query("user_id") userId: Int,
        @Query("status") status: String = "All",
        @Query("search") search: String = "",
        @Query("range") range: String = "all"
    ): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("admin_leads.php")
    suspend fun getAdminLeadsPost(@FieldMap params: Map<String, String>): Response<okhttp3.ResponseBody>

    @POST("get_lead_details.php")
    suspend fun getLeadDetails(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("admin_get_lead_details.php")
    suspend fun getAdminLeadDetails(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("get_lead_licenses.php")
    suspend fun getExistingLicenses(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<okhttp3.ResponseBody>

    @POST("get_license_details.php")
    suspend fun getLicenseDetails(@Body request: Map<String, String>): Response<okhttp3.ResponseBody>

    @POST("get_today_followups.php")
    suspend fun getTodayFollowUps(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("create_lead.php")
    suspend fun createLead(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("create_lead.php")
    suspend fun addAdminLead(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("update_lead_status.php")
    suspend fun updateLeadStatus(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("assign_lead.php")
    suspend fun assignLead(@FieldMap request: Map<String, String>): Response<JsonElement>

    // ----- FOLLOW UPS -----
    @FormUrlEncoded
    @POST("add_follow_up.php")
    suspend fun addFollowUp(@FieldMap request: Map<String, String>): Response<JsonElement>

    @POST("get_followups.php")
    suspend fun getFollowUps(@Body request: Map<String, Int>): Response<JsonElement>

    @FormUrlEncoded
    @POST("update_followup_status.php")
    suspend fun updateFollowUpStatus(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("reschedule_follow_up.php")
    suspend fun rescheduleFollowUp(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("complete_follow_up.php")
    suspend fun completeFollowUp(@FieldMap request: Map<String, String>): Response<JsonElement>

    // ----- QUOTATIONS -----
    @FormUrlEncoded
    @POST("create_quotation.php")
    suspend fun createQuotation(@FieldMap request: Map<String, String>): Response<JsonElement>

    @POST("get_quotations.php")
    suspend fun getQuotations(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("get_quote_details.php")
    suspend fun getQuoteDetails(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("update_quote_status.php")
    suspend fun updateQuoteStatus(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("delete_quote.php")
    suspend fun deleteQuote(@FieldMap request: Map<String, String>): Response<JsonElement>

    // ----- PROFORMA INVOICES -----
    @FormUrlEncoded
    @POST("create_proforma.php")
    suspend fun createProforma(@FieldMap request: Map<String, String>): Response<JsonElement>

    @POST("get_proformas.php")
    suspend fun getProformas(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("get_proforma_details.php")
    suspend fun getProformaDetails(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("update_proforma_status.php")
    suspend fun updateProformaStatus(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("delete_proforma.php")
    suspend fun deleteProforma(@FieldMap request: Map<String, String>): Response<JsonElement>

    // ----- WORKS -----
    @POST("get_works.php")
    suspend fun getWorks(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("get_work_details.php")
    suspend fun getWorkDetails(@Body request: WorkDetailsRequest): Response<JsonElement>

    @FormUrlEncoded
    @POST("create_work.php")
    suspend fun createWork(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("update_work_status.php")
    suspend fun updateWorkStatus(@FieldMap request: Map<String, String>): Response<JsonElement>

    @POST("complete_work_to_customer.php")
    suspend fun completeWork(@Body request: CompleteWorkRequest): Response<JsonElement>

    @POST("add_work_payment.php")
    suspend fun addWorkPayment(@Body request: Map<String, String>): Response<JsonElement>

    @POST("get_work_payments.php")
    suspend fun getWorkPayments(@Body request: Map<String, Int>): Response<JsonElement>

    @FormUrlEncoded
    @POST("delete_work.php")
    suspend fun deleteWork(@FieldMap request: Map<String, String>): Response<JsonElement>

    // ----- INVOICES -----
    @POST("get_invoices.php")
    suspend fun getInvoices(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    // ----- RAZORPAY -----
    @FormUrlEncoded
    @POST("create_razorpay_link.php")
    suspend fun createRazorpayLink(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @POST("razorpay_webhook.php")
    suspend fun razorpayWebhook(@Body request: Map<String, String>): Response<Map<String, String>>

    // ----- PRODUCTS -----
    @POST("get_products.php")
    suspend fun getProducts(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("add_product.php")
    suspend fun addProduct(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("update_product.php")
    suspend fun updateProduct(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("delete_product.php")
    suspend fun deleteProduct(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    // ==================== CUSTOMERS — EMPLOYEE (get_customers.php) ====================

    @POST("get_customers.php")
    suspend fun getCustomers(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<okhttp3.ResponseBody>

    @POST("get_customer_details.php")
    suspend fun getCustomerDetails(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<okhttp3.ResponseBody>

    @POST("get_customer_licenses.php")
    suspend fun getCustomerLicenses(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("add_customer_license.php")
    suspend fun addCustomerLicense(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("update_customer_license.php")
    suspend fun updateCustomerLicense(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    // ==================== CUSTOMERS — ADMIN (admin_customers.php) ====================

    /** GET list of all admin customers */
    @GET("admin_customers.php")
    suspend fun getAdminCustomers(
        @Query("user_id") userId: Int,
        @Query("search") search: String = ""
    ): Response<okhttp3.ResponseBody>

    /** POST actions (add_customer_with_license, add_license, edit_license, delete_license) */
    @FormUrlEncoded
    @POST("admin_customers.php")
    suspend fun adminCustomersPost(@FieldMap params: Map<String, String>): Response<okhttp3.ResponseBody>

    /** GET ajax=get_customer + ajax=get_license_detail */
    @GET("admin_customers.php")
    suspend fun adminCustomersGet(@QueryMap params: Map<String, String>): Response<okhttp3.ResponseBody>

    /** Multipart CSV import */
    @Multipart
    @POST("admin_customers.php")
    suspend fun adminCustomersUpload(
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Response<okhttp3.ResponseBody>

    @GET("admin_quotes_api.php")
    suspend fun getAdminQuotes(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @FormUrlEncoded
    @POST("admin_quotes_api.php")
    suspend fun adminQuotesPost(@FieldMap params: Map<String, String>): Response<ResponseBody>

    @GET("admin_quotes_api.php")
    suspend fun adminQuotesGet(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @GET("admin_payroll_api.php")
    suspend fun getAdminPayroll(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @FormUrlEncoded
    @POST("admin_payroll_api.php")
    suspend fun adminPayrollPost(@FieldMap params: Map<String, String>): Response<ResponseBody>

    @GET("admin_leave_api.php")
    suspend fun getAdminLeaves(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @FormUrlEncoded
    @POST("admin_leave_api.php")
    suspend fun adminLeavePost(@FieldMap params: Map<String, String>): Response<ResponseBody>

    @GET("admin_invoices_api.php")
    suspend fun getAdminInvoices(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @FormUrlEncoded
    @POST("admin_invoices_api.php")
    suspend fun adminInvoicesPost(@FieldMap params: Map<String, String>): Response<ResponseBody>

    // ----- PAYMENTS -----
    @POST("get_payments.php")
    suspend fun getPayments(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("get_payment_details.php")
    suspend fun getPaymentDetails(@Body request: Map<String, Int>): Response<JsonElement>

    @FormUrlEncoded
    @POST("delete_payment.php")
    suspend fun deletePayment(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    // ----- ACTIVITIES & HISTORY -----
    @POST("get_activities.php")
    suspend fun getActivities(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("update_activity.php")
    suspend fun updateActivity(@Body request: Map<String, String>): Response<JsonElement>

    @POST("ask_ai.php")
    suspend fun askAi(@Body request: Map<String, String>): Response<JsonElement>

    @POST("get_lead_history.php")
    suspend fun getLeadHistory(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    // ----- TASKS -----
    @POST("get_tasks.php")
    suspend fun getTasks(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("add_task.php")
    suspend fun addTask(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("update_task_status.php")
    suspend fun updateTaskStatus(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("delete_task.php")
    suspend fun deleteTask(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    // ----- LEAVES -----
    @POST("get_leaves.php")
    suspend fun getLeaves(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @POST("apply_leave.php")
    suspend fun applyLeave(@Body request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("update_leave_status.php")
    suspend fun updateLeaveStatus(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("delete_leave.php")
    suspend fun deleteLeave(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    // ----- ATTENDANCE -----
    @FormUrlEncoded
    @POST("mark_attendance.php")
    suspend fun markAttendance(@FieldMap request: Map<String, String>): Response<JsonElement>

    @POST("get_attendance.php")
    suspend fun getAttendanceHistory(@Body request: Map<String, Int>): Response<JsonElement>

    @POST("get_attendance_summary.php")
    suspend fun getAttendanceSummary(@Body request: Map<String, Int>): Response<JsonElement>

    // ----- PAYROLL -----
    @POST("get_payroll.php")
    suspend fun getPayroll(@Body request: Map<String, Int>): Response<JsonElement>

    @GET("get_payroll.php")
    suspend fun getPayrollGet(@QueryMap params: Map<String, String>): Response<JsonElement>

    @POST("get_employee_payroll.php")
    suspend fun getEmployeePayroll(@Body request: Map<String, Int>): Response<JsonElement>

    @GET("get_employee_payroll.php")
    suspend fun getEmployeePayrollGet(@QueryMap params: Map<String, String>): Response<JsonElement>

    @POST("get_payroll_details.php")
    suspend fun getPayrollDetails(@Body request: Map<String, Int>): Response<JsonElement>

    // ----- NOTIFICATIONS -----
    @POST("get_notifications.php")
    suspend fun getNotifications(@Body request: Map<String, Int>): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("mark_notification_read.php")
    suspend fun markNotificationRead(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    @FormUrlEncoded
    @POST("delete_notification.php")
    suspend fun deleteNotification(@FieldMap request: Map<String, String>): Response<Map<String, String>>

    // ----- CALL LOGS -----
    @POST("sync_call_log.php")
    suspend fun syncCallLog(@Body request: Map<String, String>): Response<Map<String, String>>

    @POST("get_call_logs.php")
    suspend fun getCallLogs(@Body request: Map<String, Int>): Response<JsonElement>

    @FormUrlEncoded
    @POST("log_call_history.php")
    suspend fun logCallHistory(@FieldMap request: Map<String, String>): Response<JsonElement>

    @FormUrlEncoded
    @POST("update_call_notes.php")
    suspend fun updateCallNotes(@FieldMap request: Map<String, String>): Response<JsonElement>

    @GET("check_update.php")
    suspend fun checkUpdate(): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("admin-dashboard.php")
    suspend fun getAdminDashboardData(@FieldMap request: Map<String, String>): Response<okhttp3.ResponseBody>

    @POST("get_employees.php")
    suspend fun getEmployees(@Body request: Map<String, Int>): Response<EmployeesResponse>

    // ----- ACTIVITY WITH PHOTO -----
    @Multipart
    @POST("update_activity.php")
    suspend fun updateActivityWithPhoto(
        @Part("user_id") userId: RequestBody,
        @Part("activity_id") activityId: RequestBody,
        @Part("action") action: RequestBody,
        @Part photo: MultipartBody.Part? = null,
        @Part("photo_verified") photoVerified: RequestBody? = null
    ): Response<JsonElement>

    fun downloadPayslipPdf(payrollId: Int)
}
