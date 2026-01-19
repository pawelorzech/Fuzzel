package com.fizzy.android.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstanceInterceptor @Inject constructor(
    private val instanceManager: InstanceManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val baseUrl = instanceManager.getBaseUrl()

        if (baseUrl == null) {
            return chain.proceed(originalRequest)
        }

        val accountSlug = instanceManager.getAccountSlug()
        val originalPath = originalRequest.url.encodedPath

        // Global paths that don't need account prefix
        val isGlobalPath = originalPath.startsWith("/my/") ||
                originalPath.startsWith("/magic_links") ||
                originalPath.startsWith("/sessions")

        val newUrl = baseUrl.toHttpUrlOrNull()?.let { newBaseUrl ->
            val urlBuilder = originalRequest.url.newBuilder()
                .scheme(newBaseUrl.scheme)
                .host(newBaseUrl.host)
                .port(newBaseUrl.port)

            // Add account slug prefix for non-global paths
            if (!isGlobalPath && accountSlug != null) {
                // Build new path with account slug prefix
                val newPath = "/$accountSlug$originalPath"
                urlBuilder.encodedPath(newPath)
            }

            urlBuilder.build()
        } ?: originalRequest.url

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
