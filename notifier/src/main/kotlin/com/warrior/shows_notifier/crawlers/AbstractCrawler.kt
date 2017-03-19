package com.warrior.shows_notifier.crawlers

/**
 * Created by warrior on 11/12/16.
 */
abstract class AbstractCrawler(private val sourceId: Int) : Crawler {
    override fun sourceId(): Int = sourceId
}
