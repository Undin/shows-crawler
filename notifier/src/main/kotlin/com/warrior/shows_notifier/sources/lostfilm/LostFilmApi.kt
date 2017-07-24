package com.warrior.shows_notifier.sources.lostfilm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.warrior.shows_notifier.entities.Show
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

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class LostFilmResponse(
        @JsonProperty("data") val data: List<LostFilmShow>
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class LostFilmShow(
        @JsonProperty("id") val rawId: String,
        @JsonProperty("title_orig") val title: String,
        @JsonProperty("title") val localTitle: String,
        @JsonProperty("link") val showUrl: String
) {
    fun toShow(sourceName: String, baseUrl: String): Show
            = Show(sourceName, rawId.toLong(), title, localTitle, baseUrl + showUrl)
}
