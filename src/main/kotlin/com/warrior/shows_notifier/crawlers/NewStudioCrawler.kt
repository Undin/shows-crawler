package com.warrior.shows_notifier.crawlers

import com.warrior.shows_notifier.ShowEpisode
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by warrior on 10/29/16.
 */
class NewStudioCrawler(printLogs: Boolean = false) : AbstractCrawler(printLogs) {

    private val pattern = Pattern.compile("(.*) \\(Сезон (\\d+), Серия (\\d+)\\).*")

    override fun episodes(): List<ShowEpisode> {

        val document = try {
            Jsoup.connect("http://newstudio.tv/").get()
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }

        val elements = document.select("div.torrent")
        val episodes = ArrayList<ShowEpisode>(elements.size)

        for (element in elements) {
            val desc = element.select("div.tdesc").first()
            if (desc != null) {
                val text = desc.text()
                val matcher = pattern.matcher(text)
                if (matcher.find()) {
                    val showTitle = matcher.group(1)
                    val season = matcher.group(2).toInt()
                    val episodeNumber = matcher.group(3).toInt()
                    val episode = ShowEpisode(showTitle, season, episodeNumber)
                    episodes += episode
                    log("newstudio: $episode")
                }
            }
        }

        return episodes
    }
}
