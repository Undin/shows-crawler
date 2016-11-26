package com.warrior.shows_notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.warrior.shows_notifier.crawlers.LostFilmCrawler
import com.warrior.shows_notifier.crawlers.NewStudioCrawler
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by warrior on 10/29/16.
 */
const val TERMINAL_NOTIFIER = "TERMINAL_NOTIFIER"

fun main(args: Array<String>) {
    printCurrentTime()

    val options = Options()
    options.addOption("s", "settings", true, "path to settings file")
    options.addOption("r", "results", true, "path to file with previous notifications")
    options.addOption("l", "logs", false, "print logs")
    options.addOption("h", "help", false, "show this help")
    val requiredOptions = listOf("s", "r")

    val parser = DefaultParser()
    val line = parser.parse(options, args)
    if (line.hasOption("h")) {
        printHelp(options)
        return
    }
    options.options
            .filter { it.opt in requiredOptions }
            .forEach {
                if (!line.hasOption(it.opt)) {
                    println("Missing required option: ${it.opt}")
                    printHelp(options)
                    System.exit(1)
                }
            }
    val settingsFile = line.getOptionValue("s")
    val resultsPath = line.getOptionValue("r")
    val printLogs = line.hasOption("l")

    val terminalNotifier = System.getenv(TERMINAL_NOTIFIER)
    if (terminalNotifier == null) {
        System.out.println("$TERMINAL_NOTIFIER variable is not set")
        System.exit(1)
    }

    val mapper = ObjectMapper().registerKotlinModule()
    val settings: Map<String, List<String>> = mapper.readValue(File(settingsFile))
    val resultsFile = File(resultsPath)
    val resultsMap: Map<String, Map<String, Episode>> = if (resultsFile.exists()) {
        mapper.readValue(resultsFile)
    } else {
        emptyMap()
    }

    val newResultsMap = HashMap<String, Map<String, Episode>>()
    for ((k, v) in settings) {
        val results = resultsMap[k] ?: emptyMap()
        val newResults = when (k) {
            "lostfilm" -> checkSeries(LostFilmCrawler(printLogs), v, results, terminalNotifier, "http://www.lostfilm.tv/")
            "newstudio" -> checkSeries(NewStudioCrawler(printLogs), v, results, terminalNotifier, "http://newstudio.tv/")
            else -> emptyMap()
        }
        newResultsMap[k] = newResults
    }
    mapper.writeValue(resultsFile, newResultsMap)
}

private fun printCurrentTime() {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS")
    println(dateFormat.format(Date()))
}

private fun printHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.printHelp("java -jar show-notifier-jarfile.jar [options...]", options)
}

private fun checkSeries(crawler: Crawler, shows: List<String>, results: Map<String, Episode>,
                        terminalNotifier: String, url: String): Map<String, Episode> {
    val newResults = HashMap<String, Episode>(results)
    val episodes = crawler.episodes()
    for ((showTitle, season, episodeNumber) in episodes) {
        if (showTitle in shows) {
            val episode = Episode(season, episodeNumber)
            val lastShowEpisode = results[showTitle]
            if (lastShowEpisode == null || lastShowEpisode < episode) {
                val lastNewEpisode = newResults[showTitle]
                if (lastNewEpisode == null || lastNewEpisode < episode) {
                    newResults[showTitle] = episode
                }
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
    return newResults
}
