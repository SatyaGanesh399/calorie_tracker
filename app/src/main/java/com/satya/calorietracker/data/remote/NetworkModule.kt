package com.satya.calorietracker.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.satya.calorietracker.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * One OkHttp client, one JSON parser, shared by every provider.
 * HTTP responses are cached on disk (10 MB) so repeating a search costs nothing.
 */
object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true              // some OFF fields arrive as quoted numbers
        coerceInputValues = true
        explicitNulls = false
    }

    @Volatile
    private var client: OkHttpClient? = null

    fun okHttp(context: Context): OkHttpClient = client ?: synchronized(this) {
        client ?: build(context.applicationContext).also { client = it }
    }

    private fun build(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, 10L * 1024 * 1024))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                // Open Food Facts asks every client to identify itself.
                val request = chain.request().newBuilder()
                    .header("User-Agent", BuildConfig.OFF_USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                    )
                }
            }
            .build()
    }

    fun retrofit(context: Context, baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp(context))
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

/** Cheap, synchronous "is there a usable network" check. No permission beyond ACCESS_NETWORK_STATE. */
class NetworkMonitor(private val context: Context) {

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
