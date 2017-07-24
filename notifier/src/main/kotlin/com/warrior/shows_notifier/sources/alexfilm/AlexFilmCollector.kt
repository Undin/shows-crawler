package com.warrior.shows_notifier.sources.alexfilm

import com.warrior.shows_notifier.sources.ShowCollector
import com.warrior.shows_notifier.entities.Show
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.util.regex.Pattern

class AlexFilmCollector(
        private val sourceName: String,
        private val baseUrl: String = AlexFilmCollector.BASE_URL
) : ShowCollector {

    private val logger: Logger = LogManager.getLogger(javaClass)
    private val baseUri: URI = URI.create(baseUrl)

    override fun collect(rawIds: Set<Long>): List<Show> {
        return try {
            val document = Jsoup.connect(baseUrl).get()
            document.select("li.list-group-item-serials a")
                    .mapNotNull { it.toShow(rawIds) }
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }

    private fun Element.toShow(rawIds: Set<Long>): Show? {
        val href = attr("href")
        val rawId = href.removePrefix("?f=").toLong()
        if (rawId in EXCLUDED_ELEMENTS || rawId in rawIds) return null
        val rawTitle = attr("title")

        val matcher = SHORT_TITLE_PATTERN.matcher(rawTitle)
        return if (matcher.find()) {
            val localTitle = matcher.group(1)
            val title = matcher.group(2)
            val showUrl =  baseUri.resolve(href).toString()
            if (!isShow(showUrl, title)) return null
            Show(sourceName, rawId, title, localTitle, showUrl)
        } else null
    }

    private fun isShow(showUrl: String, expectedTitle: String): Boolean {
        return try {
            val document = Jsoup.connect(showUrl).get()
            val elements = document.select("div.panel-default img.img-portal")
            elements.any { e ->
                val rawTitle = e.attr("title")
                val matcher = TITLE_PATTERN.matcher(rawTitle)
                if (matcher.find()) {
                    val title= matcher.group(1)
                    title == expectedTitle
                } else false
            }
        } catch (e: IOException) {
            logger.error(e.message, e)
            false
        }
    }

    companion object {
        private const val BASE_URL: String = "http://alexfilm.cc/"

        private val EXCLUDED_ELEMENTS = setOf(
                79L,
                182
        )

        private val SHORT_TITLE_PATTERN = Pattern.compile("(.*) / (.*)")
        private val TITLE_PATTERN =
                Pattern.compile(".*/ (.*?) / Сезон:? (\\d+) / Серии:? (\\d+)-(\\d+).*\\(AlexFilm\\)")
    }
}
