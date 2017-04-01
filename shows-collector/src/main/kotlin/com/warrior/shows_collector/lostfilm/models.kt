package com.warrior.shows_collector.lostfilm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.warrior.shows_collector.Show

/**
 * Created by warrior on 2/19/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class LostFilmShow(
        @JsonProperty("id") val rawId: String,
        @JsonProperty("title_orig") val title: String,
        @JsonProperty("title") val localTitle: String,
        @JsonProperty("link") val showUrl: String
) {
    fun toShow(sourceName: String, baseUrl: String): Show
            = Show(sourceName, rawId.toInt(), title, localTitle, baseUrl + showUrl)
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class LostFilmResponse(
        @JsonProperty("data") val data: List<LostFilmShow>
)
