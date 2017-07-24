package com.warrior.shows_notifier.sources.newstudio

import com.warrior.shows_notifier.sources.Crawler
import com.warrior.shows_notifier.entities.ShowEpisode
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.regex.Pattern

/**
 * Created by warrior on 10/29/16.
 */
class NewStudioCrawler(
        baseUrl: String = NewStudioCrawler.DEFAULT_BASE_URL
) : Crawler {

    private val logger = LogManager.getLogger(javaClass)

    private val pattern = Pattern.compile("\\(Сезон (\\d+), Серия (\\d+)\\) / (.*) \\(\\d{4}\\)")
    private val baseUri = URI(baseUrl)

    override fun episodes(): List<ShowEpisode> {
         return try {
            val document = Jsoup.connect(baseUri.toString()).get()
            val elements = document.select("div.torrent")
            val episodes = ArrayList<ShowEpisode>(elements.size)

            for (element in elements) {
                val episodeUrl = element.select("div.ttitle a.label-title")
                        .first()
                        ?.attr("href") ?: continue
                val desc = element.select("div.tdesc").first()
                if (desc != null) {
                    val text = desc.text()
                    val matcher = pattern.matcher(text)
                    if (matcher.find()) {
                        val season = matcher.group(1).toInt()
                        val episodeNumber = matcher.group(2).toInt()
                        val showTitle = matcher.group(3)
                        val episode = ShowEpisode(season, episodeNumber, showTitle, baseUri.resolve(episodeUrl).toString())
                        episodes += episode
                        logger.debug("newstudio: $episode")
                    }
                }
            }
            episodes
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }

    companion object {
        private const val DEFAULT_BASE_URL = "http://newstudio.tv/"
    }
}
