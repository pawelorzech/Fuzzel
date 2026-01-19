package com.fizzy.android.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val instanceManager: InstanceManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = instanceManager.getToken()

        val request = if (!token.isNullOrBlank()) {
            // Try token without Bearer prefix first (some APIs like 37signals use this)
            val authHeader = if (token.startsWith("Bearer ")) {
                token
            } else {
                "Bearer $token"
            }

            Log.d("AuthInterceptor", "Request URL: ${originalRequest.url}")
            Log.d("AuthInterceptor", "Token length: ${token.length}")

            originalRequest.newBuilder()
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
        } else {
            Log.d("AuthInterceptor", "No token available for: ${originalRequest.url}")
            originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
        }

        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            Log.e("AuthInterceptor", "Request failed: ${response.code} - ${response.message}")
        }

        return response
    }
}
