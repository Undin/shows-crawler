package com.warrior.shows_notifier.crawlers

import com.warrior.shows_notifier.crawlers.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.entities.ShowEpisode
import okhttp3.mockwebserver.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class LostFilmCrawlerTests {

    private val server = MockWebServer()

    private val ROOT = "/"
    private val NEW = "/new/"
    private val RSS = "/rss.xml"

    init {
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    NEW -> mockHtmlResponse("lostfilm.html", "text/html; charset=utf-8")
                    RSS -> mockHtmlResponse("lostfilm.xml", "text/xml")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        })
    }

    @Before
    fun setup() {
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun testShowsCollectionFromWebSite() {
        val crawler = LostFilmCrawler(LostFilmCrawler.Mode.WEB_PAGE, server.url(ROOT).toString())
        val episodes = crawler.episodes()
        val expectedEpisodes = listOf(
                ShowEpisode(5, 18, "Arrow", server.resolve("/series/Arrow/season_5/episode_18/")),
                ShowEpisode(1, 8, "Legion", server.resolve("/series/Legion/season_1/episode_8/")),
                ShowEpisode(2, 10, "The Magicians", server.resolve("/series/The_Magicians/season_2/episode_10/")),
                ShowEpisode(1, 4, "Iron Fist", server.resolve("/series/Iron_Fist/season_1/episode_4/")),
                ShowEpisode(4, 8, "The 100", server.resolve("/series/The_100/season_4/episode_8/")),
                ShowEpisode(1, 6, "Snatch", server.resolve("/series/Snatch/season_1/episode_6/")),
                ShowEpisode(3, 18, "The Flash", server.resolve("/series/The_Flash/season_3/episode_18/")),
                ShowEpisode(2, 4, "Hand of God", server.resolve("/series/Hand_Of_God/season_2/episode_4/")),
                ShowEpisode(2, 17, "Supergirl", server.resolve("/series/Supergirl/season_2/episode_17/")),
                ShowEpisode(2, 16, "Legends of Tomorrow", server.resolve("/series/Legends_of_Tomorrow/season_2/episode_16/")),
                ShowEpisode(3, 21, "Star Wars Rebels", server.resolve("/series/Star_Wars_Rebels/season_3/episode_21/")),
                ShowEpisode(1, 5, "Snatch", server.resolve("/series/Snatch/season_1/episode_5/")),
                ShowEpisode(4, 9, "Black Sails", server.resolve("/series/Black_Sails/season_4/episode_9/")),
                ShowEpisode(1, 5, "Taken", server.resolve("/series/Taken/season_1/episode_5/"))
        )

        assertThat(episodes)
                .hasSameSizeAs(expectedEpisodes)
                .containsExactlyElementsOf(expectedEpisodes)
    }

    @Test
    fun testShowsCollectionFromRSS() {
        val crawler = LostFilmCrawler(LostFilmCrawler.Mode.RSS, server.url(ROOT).toString())
        val episodes = crawler.episodes()
        val expectedEpisodes = listOf(
                ShowEpisode(5, 18, "Arrow", "https://lostfilm.tv/series/Arrow/season_5/episode_18/"),
                ShowEpisode(1, 8, "Legion", "https://lostfilm.tv/series/Legion/season_1/episode_8/"),
                ShowEpisode(2, 10, "The Magicians", "https://lostfilm.tv/series/The_Magicians/season_2/episode_10/"),
                ShowEpisode(1, 4, "Iron Fist", "https://lostfilm.tv/series/Iron_Fist/season_1/episode_4/"),
                ShowEpisode(4, 8, "The 100", "https://lostfilm.tv/series/The_100/season_4/episode_8/"),
                ShowEpisode(1, 6, "Snatch", "https://lostfilm.tv/series/Snatch/season_1/episode_6/"),
                ShowEpisode(3, 18, "The Flash", "https://lostfilm.tv/series/The_Flash/season_3/episode_18/"),
                ShowEpisode(2, 4, "Hand of God", "https://lostfilm.tv/series/Hand_Of_God/season_2/episode_4/"),
                ShowEpisode(2, 17, "Supergirl", "https://lostfilm.tv/series/Supergirl/season_2/episode_17/"),
                ShowEpisode(2, 16, "Legends of Tomorrow", "https://lostfilm.tv/series/Legends_of_Tomorrow/season_2/episode_16/"),
                ShowEpisode(3, 21, "Star Wars Rebels", "https://lostfilm.tv/series/Star_Wars_Rebels/season_3/episode_21/"),
                ShowEpisode(1, 5, "Snatch", "https://lostfilm.tv/series/Snatch/season_1/episode_5/"),
                ShowEpisode(4, 9, "Black Sails", "https://lostfilm.tv/series/Black_Sails/season_4/episode_9/"),
                ShowEpisode(1, 5, "Taken", "https://lostfilm.tv/series/Taken/season_1/episode_5/")
        )

        assertThat(episodes)
                .hasSameSizeAs(expectedEpisodes)
                .containsExactlyElementsOf(expectedEpisodes)
    }
}