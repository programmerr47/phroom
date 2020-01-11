package com.programmerr47.phroom.sample

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.programmerr47.phroom.Phroom
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class Locator(val appContext: Context) {
    val phroom: Phroom by lazy { Phroom(appContext.applicationContext) }

    val api: Api by lazy { retrofit.create<Api>() }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val loggingInterceptor: Interceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val gson: Gson by lazy { Gson() }
}
