package com.warrior.shows_notifier.sources

import com.warrior.shows_notifier.entities.Show

/**
 * Created by warrior on 2/19/17.
 */
interface ShowCollector {

    /**
     * @param rawIds set of raw ids which are already collected
     */
    fun collect(rawIds: Set<Long> = emptySet()): List<Show>
}
