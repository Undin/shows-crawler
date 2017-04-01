package com.warrior.shows_notifier.crawlers

import com.warrior.shows_notifier.entities.ShowEpisode
import okhttp3.mockwebserver.*
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class NewStudioCrawlerTests {

    private val server = MockWebServer()

    private val ROOT = "/"

    init {
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    ROOT -> TestUtils.mockHtmlResponse("newstudio.html", "text/html; charset=utf-8")
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
        val crawler = NewStudioCrawler(server.url(ROOT).toString())
        val episodes = crawler.episodes()
        val expectedEpisodes = listOf(
                ShowEpisode(2, 3, "Hap and Leonard", server.resolve("/viewforum.php?f=455")),
                ShowEpisode(1, 6, "Powerless", server.resolve("/viewforum.php?f=543")),
                ShowEpisode(1, 9, "24: Legacy", server.resolve("/viewforum.php?f=544")),
                ShowEpisode(1, 8, "Legion", server.resolve("/viewforum.php?f=537")),
                ShowEpisode(2, 17, "Supergirl", server.resolve("/viewforum.php?f=392")),
                ShowEpisode(2, 15, "Quantico", server.resolve("/viewforum.php?f=418")),
                ShowEpisode(2, 2, "Hap and Leonard", server.resolve("/viewforum.php?f=455")),
                ShowEpisode(4, 8, "The 100", server.resolve("/viewforum.php?f=340"))
        )

        Assertions.assertThat(episodes)
                .hasSameSizeAs(expectedEpisodes)
                .containsExactlyElementsOf(expectedEpisodes)
    }
}