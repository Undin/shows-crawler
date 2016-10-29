package com.warrior.series_crawler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.warrior.series_crawler.crawlers.LostFilmCrawler
import com.warrior.series_crawler.crawlers.NewStudioCrawler
import java.io.File

/**
 * Created by warrior on 10/29/16.
 */
fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    val settings: Map<String, List<String>> = mapper.readValue(File("./settings.json"),
            object : TypeReference<Map<String, List<String>>>() {})

    for ((k, v) in settings) {
        when (k) {
            "lostfilm" -> checkSeries(LostFilmCrawler(), v, "http://www.lostfilm.tv/")
            "newstudio" -> checkSeries(NewStudioCrawler(), v, "http://newstudio.tv/")
        }
    }
}

private fun checkSeries(crawler: Crawler, shows: List<String>, url: String) {
    val episodes = crawler.episodes()
    for ((showTitle, season, episodeNumber) in episodes) {
        if (showTitle in shows) {
            val processBuilder = ProcessBuilder("terminal-notifier",
                    "-message", "S${season}E$episodeNumber",
                    "-title", "\"$showTitle\"",
                    "-sound", "default",
                    "-timeout", Int.MAX_VALUE.toString(),
                    "-open", url)
            processBuilder.start()
        }
    }
}
