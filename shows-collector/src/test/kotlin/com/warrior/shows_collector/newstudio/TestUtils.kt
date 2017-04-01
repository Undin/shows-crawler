package com.warrior.shows_collector.newstudio

import okhttp3.mockwebserver.MockResponse
import okio.Buffer

/**
 * Created by warrior on 3/19/17.
 */
object TestUtils {

    fun mockHtmlResponse(resourceName: String): MockResponse {
        val stream = javaClass.classLoader.getResourceAsStream(resourceName)
        val buffer = Buffer().readFrom(stream)
        return MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/html")
                .setBody(buffer)
    }
}
