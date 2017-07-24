package com.warrior.shows_notifier.sources.lostfilm

import com.warrior.shows_notifier.BaseTest
import com.warrior.shows_notifier.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.TestUtils.notFound
import com.warrior.shows_notifier.entities.ShowEpisode
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LostFilmCrawlerTest : BaseTest() {

    private val ROOT = "/"
    private val NEW = "/new/"
    private val RSS = "/rss.xml"

    override fun response(request: RecordedRequest): MockResponse = when (request.path) {
        NEW -> mockHtmlResponse("lostfilm/lostfilm.html")
        RSS -> mockHtmlResponse("lostfilm/lostfilm.xml", "text/xml")
        else -> notFound()
    }

    @Test
    fun testShowsCollectionFromWebSite() {
        val crawler = LostFilmCrawler(LostFilmCrawler.Mode.WEB_PAGE, resolve(ROOT))
        val episodes = crawler.episodes()
        val expectedEpisodes = listOf(
                ShowEpisode(5, 18, "Arrow", resolve("/series/Arrow/season_5/episode_18/")),
                ShowEpisode(1, 8, "Legion", resolve("/series/Legion/season_1/episode_8/")),
                ShowEpisode(2, 10, "The Magicians", resolve("/series/The_Magicians/season_2/episode_10/")),
                ShowEpisode(1, 4, "Iron Fist", resolve("/series/Iron_Fist/season_1/episode_4/")),
                ShowEpisode(4, 8, "The 100", resolve("/series/The_100/season_4/episode_8/")),
                ShowEpisode(1, 6, "Snatch", resolve("/series/Snatch/season_1/episode_6/")),
                ShowEpisode(3, 18, "The Flash", resolve("/series/The_Flash/season_3/episode_18/")),
                ShowEpisode(2, 4, "Hand of God", resolve("/series/Hand_Of_God/season_2/episode_4/")),
                ShowEpisode(2, 17, "Supergirl", resolve("/series/Supergirl/season_2/episode_17/")),
                ShowEpisode(2, 16, "Legends of Tomorrow", resolve("/series/Legends_of_Tomorrow/season_2/episode_16/")),
                ShowEpisode(3, 21, "Star Wars Rebels", resolve("/series/Star_Wars_Rebels/season_3/episode_21/")),
                ShowEpisode(1, 5, "Snatch", resolve("/series/Snatch/season_1/episode_5/")),
                ShowEpisode(4, 9, "Black Sails", resolve("/series/Black_Sails/season_4/episode_9/")),
                ShowEpisode(1, 5, "Taken", resolve("/series/Taken/season_1/episode_5/"))
        )

        assertThat(episodes)
                .hasSameSizeAs(expectedEpisodes)
                .containsExactlyElementsOf(expectedEpisodes)
    }

    @Test
    fun testShowsCollectionFromRSS() {
        val crawler = LostFilmCrawler(LostFilmCrawler.Mode.RSS, resolve(ROOT))
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
