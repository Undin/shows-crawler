package com.warrior.shows_notifier.sources.newstudio

import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException

/**
 * Created by warrior on 2/19/17.
 */
internal object ShowDetailsExtractor {

    private val LOGGER = LogManager.getLogger(javaClass)

    fun getShowDetails(url: String): ShowDetails? {
        for (i in 0..5) {
            try {
                val document = Jsoup.connect(url).get()
                val element = document.select("a.torTopic.tt-text > b:matchesOwn($STRING_ELEMENT_PATTERN)").first()
                return if (element != null) {
                    val matcher = ELEMENT_PATTERN.matcher(element.text())
                    if (matcher.find()) {
                        val localTitle = matcher.group(1)
                        val title = matcher.group(5)
                        ShowDetails(title, localTitle)
                    } else null
                } else null
            } catch (e: IOException) {
                LOGGER.error(e)
            }
        }
        return null
    }

    data class ShowDetails(val title: String, val localTitle: String)
}
