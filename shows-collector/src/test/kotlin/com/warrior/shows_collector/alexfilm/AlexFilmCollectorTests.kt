package com.warrior.shows_collector.alexfilm

import com.warrior.shows_collector.Show
import com.warrior.shows_collector.TestUtils
import okhttp3.mockwebserver.*
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class AlexFilmCollectorTests {

    private val SOURCE_NAME = "alexfilm"

    private val ROOT = "/"
    private val `11_22_63` = "/?f=130"
    private val CARTOONS = "/?f=79"
    private val INVISIBLE_WORLDS = "?f=23"
    private val WE_BARE_BEARS = "/?f=104"

    private val server = MockWebServer()

    init {
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    ROOT -> TestUtils.mockHtmlResponse("alexfilm/alexfilm.html")
                    `11_22_63` -> TestUtils.mockHtmlResponse("alexfilm/11.22.63.html")
                    CARTOONS -> TestUtils.mockHtmlResponse("alexfilm/cartoons.html")
                    INVISIBLE_WORLDS -> TestUtils.mockHtmlResponse("alexfilm/invisible_worlds.html")
                    WE_BARE_BEARS -> TestUtils.mockHtmlResponse("alexfilm/we_bare_bears.html")
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
    fun testAlexFilmCollector() {
        val baseUrl = server.url(ROOT).toString()
        val collector = AlexFilmCollector(SOURCE_NAME, baseUrl)
        val shows = collector.collect()
        Assertions.assertThat(shows).containsExactlyElementsOf(listOf(
                Show(SOURCE_NAME, 130, "11.22.63", "11/22/63", server.url(`11_22_63`).toString()),
                Show(SOURCE_NAME, 104, "We Bare Bears", "Мы обычные медведи", server.url(WE_BARE_BEARS).toString()))
        )
    }
}
