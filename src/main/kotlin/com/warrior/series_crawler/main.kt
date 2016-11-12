package com.warrior.series_crawler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.warrior.series_crawler.crawlers.LostFilmCrawler
import com.warrior.series_crawler.crawlers.NewStudioCrawler
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File

/**
 * Created by warrior on 10/29/16.
 */
const val TERMINAL_NOTIFIER = "TERMINAL_NOTIFIER"

fun main(args: Array<String>) {
    val parser = DefaultParser()

    val options = Options()
    options.addOption("s", "settings", true, "path to settings file")
    options.addOption("h", "help", false, "show this help")

    val line = parser.parse(options, args)
    if (line.hasOption("h")) {
        val formatter = HelpFormatter()
        formatter.printHelp("java -jar series-crawler-jarfile.jar [options...]", options)
        return
    }
    val settingsFile = line.getOptionValue("s")

    val terminalNotifier = System.getenv(TERMINAL_NOTIFIER)
    if (terminalNotifier == null) {
        System.out.println("$TERMINAL_NOTIFIER variable is not set")
        System.exit(1)
    }

    val mapper = ObjectMapper()
    val settings: Map<String, List<String>> = mapper.readValue(File(settingsFile),
            object : TypeReference<Map<String, List<String>>>() {})

    for ((k, v) in settings) {
        when (k) {
            "lostfilm" -> checkSeries(LostFilmCrawler(), v, terminalNotifier, "http://www.lostfilm.tv/")
            "newstudio" -> checkSeries(NewStudioCrawler(), v, terminalNotifier, "http://newstudio.tv/")
        }
    }
}

private fun checkSeries(crawler: Crawler, shows: List<String>, terminalNotifier: String, url: String) {
    val episodes = crawler.episodes()
    for ((showTitle, season, episodeNumber) in episodes) {
        if (showTitle in shows) {
            val processBuilder = ProcessBuilder(terminalNotifier,
                    "-message", "S${season}E$episodeNumber",
                    "-title", "\"$showTitle\"",
                    "-sound", "default",
                    "-timeout", Int.MAX_VALUE.toString(),
                    "-open", url)
            processBuilder.start()
        }
    }
}
