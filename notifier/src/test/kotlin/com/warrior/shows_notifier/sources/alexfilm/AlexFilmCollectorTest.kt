package com.warrior.shows_notifier.sources.alexfilm

import com.warrior.shows_notifier.BaseTest
import com.warrior.shows_notifier.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.TestUtils.notFound
import com.warrior.shows_notifier.entities.Show
import com.warrior.shows_notifier.sources.Sources
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions
import org.junit.Test

class AlexFilmCollectorTest : BaseTest() {

    private val SOURCE_NAME = Sources.ALEX_FILM.sourceName

    private val ROOT = "/"
    private val `11_22_63` = "/?f=130"
    private val CARTOONS = "/?f=79"
    private val INVISIBLE_WORLDS = "?f=23"
    private val WE_BARE_BEARS = "/?f=104"

    override fun response(request: RecordedRequest): MockResponse = when (request.path) {
        ROOT -> mockHtmlResponse("alexfilm/alexfilm.html")
        `11_22_63` -> mockHtmlResponse("alexfilm/11.22.63.html")
        CARTOONS -> mockHtmlResponse("alexfilm/cartoons.html")
        INVISIBLE_WORLDS -> mockHtmlResponse("alexfilm/invisible_worlds.html")
        WE_BARE_BEARS -> mockHtmlResponse("alexfilm/we_bare_bears.html")
        else -> notFound()
    }

    @Test
    fun testAlexFilmCollector() {
        val baseUrl = resolve(ROOT)
        val collector = AlexFilmCollector(baseUrl)
        val shows = collector.collect()
        Assertions.assertThat(shows).containsExactlyElementsOf(listOf(
                Show(SOURCE_NAME, 130, "11.22.63", "11/22/63", resolve(`11_22_63`)),
                Show(SOURCE_NAME, 104, "We Bare Bears", "Мы обычные медведи", resolve(WE_BARE_BEARS)))
        )
    }
}
