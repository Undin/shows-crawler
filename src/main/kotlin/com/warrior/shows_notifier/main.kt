package com.warrior.shows_notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.warrior.shows_notifier.crawlers.LostFilmCrawler
import com.warrior.shows_notifier.crawlers.NewStudioCrawler
import com.warrior.shows_notifier.notifiers.MacOSNotifier
import com.warrior.shows_notifier.notifiers.TelegramNotifier
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by warrior on 10/29/16.
 */

fun main(args: Array<String>) {
    printCurrentTime()

    val options = Options()
    options.addOption("c", "config", true, "path to config.json file")
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
    val configFile = line.getOptionValue("c")
    val resultsPath = line.getOptionValue("r")
    val printLogs = line.hasOption("l")

    val mapper = ObjectMapper().registerKotlinModule()
    val config: Config = mapper.readValue(File(configFile))

    val notifiers = createNotifiers(config)
    if (notifiers.isEmpty()) {
        System.err.println("can't create any notifier")
        System.exit(1)
    }

    val resultsFile = File(resultsPath)
    val resultsMap: Map<String, Map<String, Episode>> = if (resultsFile.exists()) {
        mapper.readValue(resultsFile)
    } else {
        emptyMap()
    }

    val newResultsMap = HashMap<String, Map<String, Episode>>()
    for ((k, v) in config.shows) {
        val results = resultsMap[k] ?: emptyMap()
        val newResults = when (k) {
            "lostfilm" -> checkSeries(LostFilmCrawler(printLogs), notifiers, v, results, "http://www.lostfilm.tv/")
            "newstudio" -> checkSeries(NewStudioCrawler(printLogs), notifiers, v, results, "http://newstudio.tv/")
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

private fun createNotifiers(config: Config): ArrayList<Notifier> {
    val notifiers = ArrayList<Notifier>()
    if (config.terminalNotifier != null) {
        notifiers += MacOSNotifier(config.terminalNotifier)
    }
    if (config.telegramParams != null) {
        notifiers += TelegramNotifier(config.telegramParams.token, config.telegramParams.chatId)
    }
    return notifiers
}

private fun checkSeries(crawler: Crawler, notifiers: List<Notifier>, shows: List<String>,
                        results: Map<String, Episode>, url: String): Map<String, Episode> {
    val newResults = HashMap<String, Episode>(results)
    val episodes = crawler.episodes()
    for (showEpisode in episodes) {
        val (showTitle, season, episodeNumber) = showEpisode
        if (showTitle in shows) {
            val episode = Episode(season, episodeNumber)
            val lastShowEpisode = results[showTitle]
            if (lastShowEpisode == null || lastShowEpisode < episode) {
                val lastNewEpisode = newResults[showTitle]
                if (lastNewEpisode == null || lastNewEpisode < episode) {
                    newResults[showTitle] = episode
                }
                for (notifier in notifiers) {
                    notifier.notify(showEpisode, url)
                }
            }
        }
    }
    return newResults
}
