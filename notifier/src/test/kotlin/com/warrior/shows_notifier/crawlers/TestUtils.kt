package com.warrior.shows_notifier.crawlers

import okhttp3.mockwebserver.MockResponse
import okio.Buffer

object TestUtils {

    fun mockHtmlResponse(resourceName: String, contentType: String): MockResponse {
        val stream = javaClass.classLoader.getResourceAsStream(resourceName)
        val buffer = Buffer().readFrom(stream)
        return MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", contentType)
                .setBody(buffer)
    }
}
