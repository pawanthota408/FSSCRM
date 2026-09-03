package com.fsscrm.network

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fss_crm_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val USER_ID = "user_id"
        private const val USER_NAME = "user_name"
        private const val USER_ROLE = "user_role"
        private const val USER_POSITION = "user_position"
        private const val USER_DEPT = "user_dept"
        private const val USER_DEPT_NAME = "user_dept_name"
        private const val AUTH_TOKEN = "auth_token"
        private const val IS_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(userId: Int, userName: String, role: String? = null, deptId: Int? = null, token: String? = null, position: String? = null, deptName: String? = null) {
        val editor = prefs.edit()
        editor.putInt(USER_ID, userId)
        editor.putString(USER_NAME, userName)
        if (role != null) editor.putString(USER_ROLE, role)
        if (position != null) editor.putString(USER_POSITION, position)
        if (deptId != null) editor.putInt(USER_DEPT, deptId)
        if (deptName != null) editor.putString(USER_DEPT_NAME, deptName)
        if (token != null) editor.putString(AUTH_TOKEN, token)
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getUserId(): Int = prefs.getInt(USER_ID, 0)

    fun getUserName(): String = prefs.getString(USER_NAME, "User") ?: "User"

    fun getUserRole(): String = prefs.getString(USER_ROLE, "employee") ?: "employee"

    fun getUserPosition(): String = prefs.getString(USER_POSITION, "") ?: ""

    fun getUserDept(): Int = prefs.getInt(USER_DEPT, 0)

    fun getUserDeptName(): String = prefs.getString(USER_DEPT_NAME, "") ?: ""

    fun getAuthToken(): String? = prefs.getString(AUTH_TOKEN, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN, false)

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
