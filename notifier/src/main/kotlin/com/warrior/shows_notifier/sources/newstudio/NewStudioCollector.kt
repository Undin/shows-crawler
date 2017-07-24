package com.warrior.shows_notifier.sources.newstudio

import com.warrior.shows_notifier.sources.ShowCollector
import com.warrior.shows_notifier.entities.Show
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI

/**
 * Created by warrior on 2/19/17.
 */
class NewStudioCollector(
        private val sourceName: String,
        private val baseUrl: String = NewStudioCollector.BASE_URL
) : ShowCollector {

    private val baseUri: URI  = URI(baseUrl)

    private val logger: Logger = LogManager.getLogger(javaClass)

    override fun collect(rawIds: Set<Long>): List<Show> {
        return try {
            val document = Jsoup.connect(baseUrl).get()
            val elements = document.select("div#serialist li > a")
            elements.mapNotNull { it.toShow(rawIds) }
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }

    private fun Element.toShow(rawIds: Set<Long>): Show? {
        val href = attr("href")
        val rawId = href.removePrefix("/viewforum.php?f=").toLong()
        if (rawId in EXCLUDED_ELEMENTS || rawId in rawIds) return null

        val showUrl = baseUri.resolve(href).toString()
        val showDetails = ShowDetailsExtractor.getShowDetails(showUrl)
        return if (showDetails != null) {
            val (title, localTitle) = showDetails
            val show = Show(sourceName, rawId, title, localTitle, showUrl)
            logger.debug(show)
            show
        } else {
            logger.info("Can't extract show details from ${text()} ($href)")
            null
        }
    }

    companion object {
        private const val BASE_URL = "http://newstudio.tv"

        private val EXCLUDED_ELEMENTS = setOf(
                17L,
                101
        )
    }
}
