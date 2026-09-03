package com.fsscrm.ui.common

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.StringReader

fun Response<ResponseBody>.toLenientJson(): JsonElement? {
    val raw = try { this.body()?.string() } catch (e: Exception) { null } ?: return null
    return try {
        val reader = JsonReader(StringReader(raw))
        reader.isLenient = true
        JsonParser.parseReader(reader)
    } catch (e: Exception) {
        android.util.Log.e("JSON_PARSE", "Failed to parse: $raw", e)
        null
    }
}

suspend fun handleMapResponse(
    response: Response<out Map<String, *>>,
    onSuccess: suspend () -> Unit,
    onError: suspend (String) -> Unit
) {
    if (response.isSuccessful && response.body() != null) {
        val body = response.body()!!
        if (body["status"]?.toString() == "success") {
            onSuccess()
        } else {
            onError(body["message"]?.toString() ?: "Operation failed")
        }
    } else {
        val errorBody = response.errorBody()?.string()
        onError(if (!errorBody.isNullOrBlank()) errorBody else "Server error: ${response.code()}")
    }
}

suspend fun handleJsonResponse(
    response: Response<JsonElement>,
    onSuccess: suspend () -> Unit,
    onError: suspend (String) -> Unit
) {
    if (response.isSuccessful && response.body() != null) {
        val body = response.body()!!
        if (body.isJsonObject) {
            val obj = body.asJsonObject
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                onSuccess()
            } else {
                onError(obj.get("message")?.asString ?: "Operation failed")
            }
        } else {
            onSuccess() // Fallback for plain success
        }
    } else {
        val errorBody = response.errorBody()?.string()
        onError(if (!errorBody.isNullOrBlank()) errorBody else "Server error: ${response.code()}")
    }
}
