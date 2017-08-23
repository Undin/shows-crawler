package com.warrior.shows_notifier.sources.newstudio

import com.warrior.shows_notifier.entities.ShowEpisode
import com.warrior.shows_notifier.sources.Crawler
import com.warrior.shows_notifier.sources.Sources.NEW_STUDIO
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.*

/**
 * Created by warrior on 10/29/16.
 */
class NewStudioCrawler(
        baseUrl: String = NEW_STUDIO.baseUrl
) : Crawler {

    private val logger = LogManager.getLogger(javaClass)

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
                    val matcher = ELEMENT_PATTERN.matcher(text)
                    if (matcher.find()) {
                        val season = matcher.group(2).toInt()
                        val firstEpisode = matcher.group(3).toInt()
                        val lastEpisode = matcher.group(4)?.removePrefix("-")?.toInt() ?: firstEpisode
                        val showTitle = matcher.group(5)
                        for (episodeNumber in firstEpisode..lastEpisode) {
                            val episode = ShowEpisode(season, episodeNumber, showTitle, baseUri.resolve(episodeUrl).toString())
                            episodes += episode
                            logger.debug("newstudio: $episode")
                        }
                    }
                }
            }
            episodes
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }
}
