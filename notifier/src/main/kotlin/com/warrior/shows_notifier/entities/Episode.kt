package com.warrior.shows_notifier.entities

/**
 * Created by warrior on 11/12/16.
 */
data class Episode(
        val season: Int,
        val episodeNumber: Int,
        val showUrl: String
) : Comparable<ShowEpisode> {
    override operator fun compareTo(other: ShowEpisode): Int {
        val res = season.compareTo(other.season)
        return if (res != 0) res else episodeNumber.compareTo(other.episodeNumber)
    }
}
