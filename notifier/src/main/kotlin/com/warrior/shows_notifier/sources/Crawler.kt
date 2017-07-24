package com.warrior.shows_notifier.sources

import com.warrior.shows_notifier.entities.ShowEpisode

/**
 * Created by warrior on 10/29/16.
 */
interface Crawler {
    fun episodes(): List<ShowEpisode>
}
