package com.warrior.shows_notifier.sources.lostfilm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.warrior.shows_notifier.sources.Crawler
import com.warrior.shows_notifier.entities.ShowEpisode
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.regex.Pattern

/**
 * Created by warrior on 10/29/16.
 */
class LostFilmCrawler(
        private val mode: Mode = LostFilmCrawler.Mode.WEB_PAGE,
        baseUrl: String = LostFilmCrawler.DEFAULT_BASE_URL
) : Crawler {

    private val logger = LogManager.getLogger(javaClass)
    private val baseUri = URI(baseUrl)

    override fun episodes(): List<ShowEpisode> {
        val contentUrl = baseUri.resolve(mode.urlSuffix).toString()
        return when (mode) {
            Mode.WEB_PAGE -> parsePage(contentUrl)
            Mode.RSS -> parseRSS(contentUrl)
        }
    }

    private fun parsePage(contentUrl: String): List<ShowEpisode> {
        return try {
            val document = Jsoup.connect(contentUrl).get()
            return document.select("div.row")
                    .asSequence()
                    .flatMap { row ->
                        val title = row.select("div.name-en")
                                .first()
                                ?.text() ?: return@flatMap emptySequence<ShowEpisode>()
                        val episodeUrl = row.select("a[href~=^/series/.*/season_\\d+/episode_\\d+/$]")
                                .first()
                                ?.attr("href") ?: return@flatMap emptySequence<ShowEpisode>()
                        val url = row.select("a.comment-blue-box[href]")
                                .first()
                                ?.attr("href") ?: return@flatMap emptySequence<ShowEpisode>()
                        val matcher = URL_PATTERN.matcher(url)
                        if (matcher.find()) {
                            val season = matcher.group(1).toInt()
                            val episodeNumber = matcher.group(2).toInt()
                            val episode = ShowEpisode(season, episodeNumber, title, baseUri.resolve(episodeUrl).toString())
                            logger.debug("lostfilm: $episode")
                            sequenceOf(episode)
                        } else emptySequence()

                    }.toList()
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }

    private fun parseRSS(contentUrl: String): List<ShowEpisode> {
        return try {
            val page = Jsoup.connect(contentUrl).get().toString()
            val mapper = XmlMapper()
            val rss = mapper.readValue(page, RSS::class.java)

            val episodeSet = ArrayList<ShowEpisode>(rss.channel.items.size)
            for ((title, link) in rss.channel.items) {
                if (RSS_LINK_PATTERN.matcher(link).find()) {
                    val matcher = RSS_TITLE_PATTERN.matcher(title)
                    if (matcher.find()) {
                        val showTitle = matcher.group(1)
                        val season = matcher.group(2).toInt()
                        val episodeNumber = matcher.group(3).toInt()
                        val episode = ShowEpisode(season, episodeNumber, showTitle, link.trim())
                        episodeSet += episode
                        logger.debug("lostfilm: $episode")
                    }
                }
            }
            episodeSet
        } catch (e: IOException) {
            logger.error(e.message, e)
            emptyList()
        }
    }

    companion object {
        private const val DEFAULT_BASE_URL = "https://www.lostfilm.tv/"

        private val URL_PATTERN = Pattern.compile("season_(\\d+)/episode_(\\d+)")
        private val RSS_TITLE_PATTERN = Pattern.compile("\\((.*?)\\).*\\(S(\\d+)E(\\d+)\\)")
        private val RSS_LINK_PATTERN = Pattern.compile("series/.*/season_\\d+/episode_\\d+/")
    }

    enum class Mode(val urlSuffix: String) {
        WEB_PAGE("/new/"),
        RSS("/rss.xml")
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
    private data class Item(
            @JacksonXmlProperty(localName = "title") val title: String,
            @JacksonXmlProperty(localName = "link") val link: String
    )
}
