package com.warrior.shows_collector.newstudio

import com.warrior.shows_collector.Show
import com.warrior.shows_collector.newstudio.TestUtils.mockHtmlResponse
import okhttp3.mockwebserver.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Created by warrior on 3/19/17.
 */
class NewStudioCollectorTests {

    private val SOURCE_NAME = "newstudio";

    private val ROOT = "/"
    private val FRINGE = "/viewforum.php?f=133"
    private val GAME_OF_THRONES = "/viewforum.php?f=465"
    private val LONGMIRE = "/viewforum.php?f=246"
    private val REVOLUTION = "/viewforum.php?f=254"
    private val OTHER = "/viewforum.php?f=17"

    private val server = MockWebServer()

    init {
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    ROOT -> mockHtmlResponse("newstudio.htm")
                    FRINGE -> mockHtmlResponse("fringe.htm")
                    GAME_OF_THRONES -> mockHtmlResponse("game_of_thrones.htm")
                    LONGMIRE -> mockHtmlResponse("longmire.htm")
                    REVOLUTION -> mockHtmlResponse("revolution.htm")
                    OTHER -> mockHtmlResponse("other.htm")
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
    fun newStudioCollectorTest() {
        val baseUrl = server.url(ROOT).toString()
        val collector = NewStudioCollector(SOURCE_NAME, baseUrl)
        val shows = collector.collect()
        assertThat(shows, equalTo(listOf(
                Show(SOURCE_NAME, 465, "Game of Thrones", "Игра Престолов", server.url(GAME_OF_THRONES).toString()),
                Show(SOURCE_NAME, 254, "Revolution", "Революция", server.url(REVOLUTION).toString()))
        ))
    }

    @Test
    fun errorTest() {
        val baseUrl = server.url("/wrong_path").toString()
        val collector = NewStudioCollector(SOURCE_NAME, baseUrl)
        val shows = collector.collect()
        assertThat(shows, equalTo(emptyList()))
    }
}
