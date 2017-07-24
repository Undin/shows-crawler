package com.warrior.shows_notifier.sources.newstudio

import com.warrior.shows_notifier.BaseTest
import com.warrior.shows_notifier.TestUtils.mockHtmlResponse
import com.warrior.shows_notifier.TestUtils.notFound
import com.warrior.shows_notifier.sources.newstudio.ShowDetailsExtractor.ShowDetails
import com.warrior.shows_notifier.sources.newstudio.ShowDetailsExtractor.getShowDetails
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by warrior on 3/18/17.
 */
class ShowDetailsExtractorTest : BaseTest() {

    private val FRINGE = "/viewforum.php?f=133"
    private val GAME_OF_THRONES = "/viewforum.php?f=465"
    private val LONGMIRE = "/viewforum.php?f=246"
    private val REVOLUTION = "/viewforum.php?f=254"
    private val EMERALD_CITY = "/viewforum.php?f=531"

    override fun response(request: RecordedRequest): MockResponse = when (request.path) {
        FRINGE -> mockHtmlResponse("newstudio/fringe.html")
        GAME_OF_THRONES -> mockHtmlResponse("newstudio/game_of_thrones.html")
        LONGMIRE -> mockHtmlResponse("newstudio/longmire.html")
        REVOLUTION -> mockHtmlResponse("newstudio/revolution.html")
        else -> notFound()
    }

    @Test
    fun firstItem() {
        val url = resolve(GAME_OF_THRONES)
        val showDetails = getShowDetails(url)
        assertThat(showDetails)
                .isEqualTo(ShowDetails("Game of Thrones", "Игра Престолов"))
    }

    @Test
    fun notFirstItem() {
        val url = resolve(REVOLUTION)
        val showDetails = getShowDetails(url)
        assertThat(showDetails)
                .isEqualTo(ShowDetails("Revolution", "Революция"))
    }

    @Test
    fun showWithoutSeparateSeries() {
        val url = resolve(FRINGE)
        val showDetails = getShowDetails(url)
        assertThat(showDetails).isNull()
    }

    @Test
    fun noElements() {
        val url = resolve(LONGMIRE)
        val showDetails = getShowDetails(url)
        assertThat(showDetails).isNull()
    }

    @Test
    fun requestError() {
        val url = resolve(EMERALD_CITY)
        val showDetails = getShowDetails(url)
        assertThat(showDetails).isNull()
    }
}
