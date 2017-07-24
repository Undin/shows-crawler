package com.warrior.shows_notifier.sources.newstudio

import com.warrior.shows_notifier.BaseTest
import com.warrior.shows_notifier.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.TestUtils.notFound
import com.warrior.shows_notifier.entities.ShowEpisode
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions
import org.junit.Test

class NewStudioCrawlerTest : BaseTest() {

    private val ROOT = "/"

    override fun response(request: RecordedRequest): MockResponse = when (request.path) {
        ROOT -> mockHtmlResponse("newstudio/newstudio.html")
        else -> notFound()
    }

    @Test
    fun testShowsCollectionFromWebSite() {
        val crawler = NewStudioCrawler(resolve(ROOT))
        val episodes = crawler.episodes()
        val expectedEpisodes = listOf(
                ShowEpisode(2, 3, "Hap and Leonard", resolve("/viewforum.php?f=455")),
                ShowEpisode(1, 6, "Powerless", resolve("/viewforum.php?f=543")),
                ShowEpisode(1, 9, "24: Legacy", resolve("/viewforum.php?f=544")),
                ShowEpisode(1, 8, "Legion", resolve("/viewforum.php?f=537")),
                ShowEpisode(2, 17, "Supergirl", resolve("/viewforum.php?f=392")),
                ShowEpisode(2, 15, "Quantico", resolve("/viewforum.php?f=418")),
                ShowEpisode(2, 2, "Hap and Leonard", resolve("/viewforum.php?f=455")),
                ShowEpisode(4, 8, "The 100", resolve("/viewforum.php?f=340"))
        )

        Assertions.assertThat(episodes)
                .hasSameSizeAs(expectedEpisodes)
                .containsExactlyElementsOf(expectedEpisodes)
    }
}
