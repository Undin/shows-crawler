package com.warrior.shows_notifier.sources.alexfilm

import com.warrior.shows_notifier.sources.Crawler
import com.warrior.shows_notifier.entities.ShowEpisode
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.regex.Pattern

class AlexFilmCrawler(
        baseUrl: String = AlexFilmCrawler.DEFAULT_BASE_URL
) : Crawler {

    private val logger = LogManager.getLogger(javaClass)

    private val baseUri = URI(baseUrl)

    override fun episodes(): List<ShowEpisode> {
        return try {
            val document = Jsoup.connect(baseUri.toString()).get()
            val elements = document.select("div.panel")
            elements.flatMap { element ->
                val rawTitle = element.select("img.img-portal")
                        .first()
                        ?.attr("title") ?: return@flatMap emptyList<ShowEpisode>()
                val matcher = TITLE_PATTERN.matcher(rawTitle)
                if (matcher.find()) {
                    val title = matcher.group(1)
                    val seasonNumber = matcher.group(2).toInt()
                    val firstEpisode = matcher.group(3).toInt()
                    val lastEpisode = matcher.group(4).toInt()
                    (firstEpisode..lastEpisode).map { ShowEpisode(seasonNumber, it, title, null) }
                } else emptyList()
            }.distinct()
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }

    companion object {
        private const val DEFAULT_BASE_URL: String = "http://alexfilm.cc/"

        private val TITLE_PATTERN =
                Pattern.compile(".*/ (.*?) / Сезон:? (\\d+) / Серии:? (\\d+)-(\\d+).*\\(AlexFilm\\)")
    }
}
