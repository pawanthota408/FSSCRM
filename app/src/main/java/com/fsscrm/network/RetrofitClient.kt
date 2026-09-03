package com.fsscrm.network

import com.google.gson.GsonBuilder
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private const val BASE_URL = "https://crm.friendssoftwaresolutions.in/api/"

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Trust Manager that accepts all certificates (For troubleshooting SSL library issues)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val spec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
            .allEnabledCipherSuites()
            .build()

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(loggingInterceptor)
            .connectionSpecs(listOf(spec, ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val gson = GsonBuilder()
        .setLenient()
        // --- Int / Integer ---
        .registerTypeAdapter(Int::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) 0 
            else try { json.asInt } catch (e: Exception) { 0 }
        })
        .registerTypeAdapter(java.lang.Integer::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) null 
            else try { json.asInt } catch (e: Exception) { null }
        })
        // --- Double ---
        .registerTypeAdapter(Double::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) 0.0
            else try { json.asDouble } catch (e: Exception) { 0.0 }
        })
        .registerTypeAdapter(java.lang.Double::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) null
            else try { json.asDouble } catch (e: Exception) { null }
        })
        // --- Long ---
        .registerTypeAdapter(Long::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) 0L
            else try { json.asLong } catch (e: Exception) { 0L }
        })
        .registerTypeAdapter(java.lang.Long::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) null
            else try { json.asLong } catch (e: Exception) { null }
        })
        // --- Float ---
        .registerTypeAdapter(Float::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) 0.0f
            else try { json.asFloat } catch (e: Exception) { 0.0f }
        })
        .registerTypeAdapter(java.lang.Float::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) null
            else try { json.asFloat } catch (e: Exception) { null }
        })
        .create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
