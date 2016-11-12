package com.warrior.shows_notifier

/**
 * Created by warrior on 10/29/16.
 */
interface Crawler {
    fun episodes(): List<ShowEpisode>

    data class ShowEpisode(val showTitle: String, val season: Int, val episodeNumber: Int) {
        override fun toString(): String = "$showTitle S${season}E${episodeNumber}"
    }
}
