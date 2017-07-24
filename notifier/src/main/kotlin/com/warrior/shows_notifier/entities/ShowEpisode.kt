package com.warrior.shows_notifier.entities

/**
 * Created by warrior on 11/26/16.
 */
data class ShowEpisode(
        val season: Int,
        val episodeNumber: Int,
        val showTitle: String,
        val episodeUrl: String?
) : Comparable<ShowEpisode> {

    override fun compareTo(other: ShowEpisode): Int {
        val res = season.compareTo(other.season)
        return if (res != 0) res else episodeNumber.compareTo(other.episodeNumber)
    }

    override fun toString(): String = "%s S%02dE%02d".format(showTitle, season, episodeNumber)
}
