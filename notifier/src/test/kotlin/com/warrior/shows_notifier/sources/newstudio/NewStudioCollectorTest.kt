package com.warrior.shows_notifier.sources.newstudio

import com.warrior.shows_notifier.BaseTest
import com.warrior.shows_notifier.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.TestUtils.notFound
import com.warrior.shows_notifier.entities.Show
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by warrior on 3/19/17.
 */
class NewStudioCollectorTest : BaseTest() {

    private val SOURCE_NAME = "newstudio"

    private val ROOT = "/"
    private val FRINGE = "/viewforum.php?f=133"
    private val GAME_OF_THRONES = "/viewforum.php?f=465"
    private val LONGMIRE = "/viewforum.php?f=246"
    private val REVOLUTION = "/viewforum.php?f=254"
    private val OTHERS = "/viewforum.php?f=17"

    override fun response(request: RecordedRequest): MockResponse = when (request.path) {
        ROOT -> mockHtmlResponse("newstudio/newstudio.html")
        FRINGE -> mockHtmlResponse("newstudio/fringe.html")
        GAME_OF_THRONES -> mockHtmlResponse("newstudio/game_of_thrones.html")
        LONGMIRE -> mockHtmlResponse("newstudio/longmire.html")
        REVOLUTION -> mockHtmlResponse("newstudio/revolution.html")
        OTHERS -> mockHtmlResponse("newstudio/others.html")
        else -> notFound()
    }

    @Test
    fun testNewStudioCollector() {
        val baseUrl = resolve(ROOT)
        val collector = NewStudioCollector(SOURCE_NAME, baseUrl)
        val shows = collector.collect()
        assertThat(shows).containsExactlyElementsOf(listOf(
                Show(SOURCE_NAME, 465, "Game of Thrones", "Игра Престолов", resolve(GAME_OF_THRONES)),
                Show(SOURCE_NAME, 254, "Revolution", "Революция", resolve(REVOLUTION)))
        )
    }

    @Test
    fun errorTest() {
        val baseUrl = resolve("/wrong_path")
        val collector = NewStudioCollector(SOURCE_NAME, baseUrl)
        val shows = collector.collect()
        assertThat(shows).isEmpty()
    }
}
