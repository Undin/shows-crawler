package com.warrior.shows_collector

/**
 * Created by warrior on 2/19/17.
 */
data class Show(
        val sourceName: String,
        val rawId: Int,
        val title: String,
        val localTitle: String,
        val season: Int? = null,
        val episodeNumber: Int? = null
) {
    override fun toString(): String {
        return "{ source_name: $sourceName, raw_id: $rawId, title: $title, local_title: $localTitle, season: $season, episode: $episodeNumber }"
    }
}
