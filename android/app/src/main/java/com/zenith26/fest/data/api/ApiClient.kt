package com.zenith26.fest.data.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "zenith_settings"
    private const val KEY_BASE_URL = "base_url"

    // Public live tunnel URL for cloud emulators (Appetize.io) and remote devices
    const val DEFAULT_BASE_URL = "https://mailman-enclosed-makes-suggested.trycloudflare.com/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    private val gson = Gson()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentBaseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        rebuildRetrofit()
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun updateBaseUrl(context: Context, newUrl: String) {
        var formatted = newUrl.trim()
        if (!formatted.endsWith("/")) {
            formatted += "/"
        }
        currentBaseUrl = formatted
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        rebuildRetrofit()
    }

    private fun rebuildRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit?.create(ApiService::class.java)
    }

    fun getService(): ApiService {
        if (apiService == null) {
            rebuildRetrofit()
        }
        return apiService!!
    }

    /**
     * Parses the backend error JSON e.g. {"detail": "Already registered for this event with this email"}
     */
    fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val jsonObject = gson.fromJson(errorBody, JsonObject::class.java)
                if (jsonObject.has("detail")) {
                    val detailElem = jsonObject.get("detail")
                    if (detailElem.isJsonPrimitive) {
                        return detailElem.asString
                    } else if (detailElem.isJsonArray) {
                        // For validation error array from FastAPI
                        return detailElem.asJsonArray.joinToString("\n") { it.asJsonObject.get("msg").asString }
                    }
                }
            }
            "Server returned error HTTP ${response.code()}"
        } catch (e: Exception) {
            "Server returned error HTTP ${response.code()}"
        }
    }
}
