package com.warrior.series_crawler.crawlers

import com.warrior.series_crawler.Crawler

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
