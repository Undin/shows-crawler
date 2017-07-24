package com.warrior.shows_notifier.crawlers

import com.warrior.shows_notifier.crawlers.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.entities.ShowEpisode
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions
import org.junit.Test

class AlexFilmCrawlerTests : BaseCrawlerTests() {

    private val ROOT = "/"

    override fun response(request: RecordedRequest): MockResponse = when (request.path) {
        ROOT -> mockHtmlResponse("alexfilm.html", "text/html; charset=utf-8")
        else -> MockResponse().setResponseCode(404)
    }

    @Test
    fun testShowsCollectionFromWebSite() {
        val crawler = AlexFilmCrawler(resolve(ROOT))
        val episodes = crawler.episodes()
        val expectedEpisodes = listOf(
                *episodeRange(1, 1,4, "Will", null),
                *episodeRange(1, 1, 2, "Castlevania", null),
                *episodeRange(1, 1, 5, "The Mist", null),
                *episodeRange(4, 1, 1, "The Strain", null),
                *episodeRange(2, 1, 5, "Preacher", null)
        )

        Assertions.assertThat(episodes)
                .hasSameSizeAs(expectedEpisodes)
                .containsExactlyElementsOf(expectedEpisodes)
    }

    private fun episodeRange(
            season: Int,
            startEpisode: Int,
            endEpisode: Int,
            title: String,
            url: String?
    ): Array<ShowEpisode> = Array(endEpisode - startEpisode + 1) { ShowEpisode(season, it + 1, title, url) }
}
