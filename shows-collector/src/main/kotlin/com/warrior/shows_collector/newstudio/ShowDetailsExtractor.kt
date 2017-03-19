package com.warrior.shows_collector.newstudio

import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Pattern

/**
 * Created by warrior on 2/19/17.
 */
internal object ShowDetailsExtractor {

    private const val ELEMENT_PATTERN = "(.*) \\(Сезон (\\d+), Серия (\\d+)\\) / (.*) \\(\\d{4}\\)"
    private val PATTERN = Pattern.compile(ELEMENT_PATTERN)

    private val LOGGER = LogManager.getLogger(javaClass)

    fun getShowDetails(url: String): ShowDetails? {
        for (i in 0..5) {
            try {
                val document = Jsoup.connect(url).get()
                val element = document.select("a.torTopic.tt-text > b:matchesOwn($ELEMENT_PATTERN)").first()
                return if (element != null) {
                    val matcher = PATTERN.matcher(element.text())
                    if (matcher.find()) {
                        val localTitle = matcher.group(1)
                        val season = matcher.group(2).toInt()
                        val episodeNumber = matcher.group(3).toInt()
                        val title = matcher.group(4)
                        ShowDetails(title, localTitle, season, episodeNumber)
                    } else null
                } else null
            } catch (e: IOException) {
                LOGGER.error(e)
            }
        }
        return null
    }

    data class ShowDetails(val title: String, val localTitle: String, val season: Int, val episodeNumber: Int)
}