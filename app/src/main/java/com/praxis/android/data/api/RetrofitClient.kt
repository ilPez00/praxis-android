package com.praxis.android.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://praxisweb.xyz/api/"

    private var authToken: String? = null
    private var connectivityObserver: com.praxis.android.util.NetworkConnectivityObserver? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("praxis_auth", Context.MODE_PRIVATE)
        authToken = prefs.getString("access_token", null)
        connectivityObserver = com.praxis.android.util.NetworkConnectivityObserver(context)
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private val logging = HttpLoggingInterceptor().apply {
        // BODY logged every request/response byte; BASIC keeps diagnostics
        // without the I/O tax on the API path.
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = if (!authToken.isNullOrEmpty()) {
            original.newBuilder()
                .header("Authorization", "Bearer $authToken")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val connectivityInterceptor = Interceptor { chain ->
        if (connectivityObserver?.checkCurrentConnection() == false) {
            throw java.io.IOException("No network connection")
        }
        chain.proceed(chain.request())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .addInterceptor(connectivityInterceptor)
        .build()

    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    val api: PraxisApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()
        .create(PraxisApi::class.java)
}
