package com.warrior.shows_notifier.crawlers

import com.warrior.shows_notifier.entities.ShowEpisode
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by warrior on 10/29/16.
 */
class NewStudioCrawler(sourceId: Int) : AbstractCrawler(sourceId) {

    private val logger = LogManager.getLogger(javaClass)

    private val pattern = Pattern.compile("\\(Сезон (\\d+), Серия (\\d+)\\) / (.*) \\(\\d{4}\\)")

    override fun episodes(): List<ShowEpisode> {
         return try {
            val document = Jsoup.connect("http://newstudio.tv/").get()
            val elements = document.select("div.torrent")
            val episodes = ArrayList<ShowEpisode>(elements.size)

            for (element in elements) {
                val desc = element.select("div.tdesc").first()
                if (desc != null) {
                    val text = desc.text()
                    val matcher = pattern.matcher(text)
                    if (matcher.find()) {
                        val season = matcher.group(1).toInt()
                        val episodeNumber = matcher.group(2).toInt()
                        val showTitle = matcher.group(3)
                        val episode = ShowEpisode(showTitle, season, episodeNumber)
                        episodes += episode
                        logger.debug("newstudio: $episode")
                    }
                }
            }
            episodes
        } catch (e: IOException) {
            logger.error(e)
            emptyList()
        }
    }
}
