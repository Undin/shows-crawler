package com.warrior.shows_collector.lostfilm

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by warrior on 2/19/17.
 */
internal interface LostFilmApi {

    @GET("/ajaxik.php?act=serial&type=search")
    fun shows(@Query("o") offset: Int): Call<LostFilmResponse>
}