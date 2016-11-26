package com.warrior.shows_notifier

/**
 * Created by warrior on 11/26/16.
 */
interface Notifier {
    fun notify(episode: ShowEpisode, url: String)
}
