package com.warrior.shows_notifier

/**
 * Created by warrior on 11/26/16.
 */
data class ShowEpisode(val showTitle: String, val season: Int, val episodeNumber: Int) {
    override fun toString(): String = "$showTitle S${"%02d".format(season)}E${"%02d".format(episodeNumber)}"
}
