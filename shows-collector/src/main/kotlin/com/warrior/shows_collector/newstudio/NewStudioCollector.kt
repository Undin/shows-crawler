package com.warrior.shows_collector.newstudio

import com.warrior.shows_collector.Show
import com.warrior.shows_collector.ShowCollector
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.*

/**
 * Created by warrior on 2/19/17.
 */
class NewStudioCollector(
        private val newStudioId: Int,
        private val baseUrl: String = BASE_URL) : ShowCollector {

    private val EXCLUDED_ELEMENTS = setOf(
            17,
            101
    )

    private val logger = LogManager.getLogger(javaClass)

    override fun collect(): List<Show> {
        val shows = ArrayList<Show>()
        try {
            val document = Jsoup.connect(baseUrl).get()
            val elements = document.select("div#serialist li > a")
            for (e in elements) {
                val href = e.attr("href")
                val rawId = href.removePrefix("/viewforum.php?f=").toInt()
                if (rawId in EXCLUDED_ELEMENTS) {
                    continue
                }
                val showDetails = ShowDetailsExtractor.getShowDetails(URI(baseUrl).resolve(href).toString())
                if (showDetails != null) {
                    val (title, localTitle, season, episodeNumber) = showDetails
                    val show = Show(newStudioId, rawId, title, localTitle, season, episodeNumber)
                    println(show)
                    shows += show
                } else {
                    val elementName = e.text()
                    logger.info("Can't extract show details from $elementName  ($href)")
                }
            }
        } catch (e: IOException) {
            logger.error(e)
            return emptyList()
        }
        return shows
    }

    companion object {
        private const val BASE_URL = "http://newstudio.tv"
    }
}
