package com.warrior.shows_notifier.entities

/**
 * Created by warrior on 2/19/17.
 */
data class Show(
        val sourceName: String,
        val rawId: Long,
        val title: String,
        val localTitle: String,
        val showUrl: String
) {
    override fun toString(): String {
        return "{ source_name: $sourceName, " +
                "raw_id: $rawId, " +
                "title: $title, " +
                "local_title: $localTitle, " +
                "url: $showUrl }"
    }
}
