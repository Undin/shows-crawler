package com.warrior.shows_notifier.crawlers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.warrior.shows_notifier.entities.ShowEpisode
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by warrior on 10/29/16.
 */
class LostFilmCrawler(private val contentUrl: String? = null) : Crawler {

    private val logger = LogManager.getLogger(javaClass)

    override fun episodes(): List<ShowEpisode> = parsePage()

    private fun parsePage(): List<ShowEpisode> {
        return try {
            val document = Jsoup.connect(contentUrl ?: PAGE_URL).get()
            return document.select("div.row")
                    .asSequence()
                    .flatMap { row ->
                        val title = row.select("div.name-en").first()?.text() ?: return@flatMap emptySequence<ShowEpisode>()
                        val href = row.select("a.comment-blue-box[href]").first() ?: return@flatMap emptySequence<ShowEpisode>()
                        val url = href.attr("href")
                        val matcher = URL_PATTERN.matcher(url)
                        if (matcher.find()) {
                            val season = matcher.group(1).toInt()
                            val episodeNumber = matcher.group(2).toInt()
                            val episode = ShowEpisode(title, season, episodeNumber)
                            logger.debug("lostfilm: $episode")
                            sequenceOf(episode)
                        } else emptySequence()

                    }.toList()
        } catch (e: IOException) {
            logger.error(e)
            emptyList()
        }
    }

    private fun parseRSS(): List<ShowEpisode> {
        return try {
            val page = Jsoup.connect(contentUrl ?: RSS_URL).get().toString()
            val mapper = XmlMapper()
            val rss = mapper.readValue(page, RSS::class.java)

            val episodeSet = HashSet<ShowEpisode>(rss.channel.items.size)
            for (item in rss.channel.items) {
                val matcher = RSS_PATTERN.matcher(item.title)
                if (matcher.find()) {
                    val showTitle = matcher.group(1)
                    val season = matcher.group(2).toInt()
                    val episodeNumber = matcher.group(3).toInt()
                    val episode = ShowEpisode(showTitle, season, episodeNumber)
                    episodeSet += episode
                    logger.debug("lostfilm: $episode")
                }
            }
            episodeSet.toList()
        } catch (e: IOException) {
            logger.error(e)
            emptyList()
        }
    }

    companion object {
        private const val PAGE_URL = "http://www.lostfilm.tv/new/"
        private const val RSS_URL = "http://www.lostfilm.tv/rss.xml"

        private val URL_PATTERN = Pattern.compile("season_(\\d+)/episode_(\\d+)")
        private val RSS_PATTERN = Pattern.compile("(.*?) .*\\(S(\\d)+E(\\d+)\\)")
    }

    @JacksonXmlRootElement(localName = "rss")
    @JsonIgnoreProperties(ignoreUnknown = true)
    private class RSS(
        @JacksonXmlProperty(localName = "channel") val channel: Channel
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Channel {

        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        lateinit var items: List<Item>
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Item(
            @JacksonXmlProperty(localName = "title") val title: String
    )
}
