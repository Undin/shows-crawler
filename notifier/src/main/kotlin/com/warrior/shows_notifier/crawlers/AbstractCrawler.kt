package com.warrior.shows_notifier.crawlers

import com.warrior.shows_notifier.Crawler

/**
 * Created by warrior on 11/12/16.
 */
abstract class AbstractCrawler(private val printLogs: Boolean) : Crawler {

    protected fun log(message: String) {
        if (printLogs) {
            println(message)
        }
    }
}
