package com.programmerr47.phroom.sample

import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    //TODO remove default value by analyzing screen size
    @GET(".")
    suspend fun getUsers(@Query("results") count: Int = 20): UsersResponse
}