package com.warrior.shows_notifier

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by warrior on 11/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Episode @JsonCreator constructor(
        @param:JsonProperty("season") @get:JsonProperty("season") val season: Int,
        @param:JsonProperty("episode") @get:JsonProperty("episode") val episodeNumber: Int
) : Comparable<Episode> {
    override operator fun compareTo(other: Episode): Int {
        val res = season.compareTo(other.season)
        return if (res != 0) res else episodeNumber.compareTo(other.episodeNumber)
    }
}
