package com.warrior.series_crawler

/**
 * Created by warrior on 10/29/16.
 */
interface Crawler {
    fun episodes(): List<Episode>

    data class Episode(val showTitle: String, val season: Int, val episodeNumber: Int) {
        override fun toString(): String = "$showTitle S${season}E${episodeNumber}"
    }
}
