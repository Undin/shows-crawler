package com.warrior.shows_notifier.crawlers

import okhttp3.mockwebserver.*
import org.junit.After
import org.junit.Before

abstract class BaseCrawlerTests {

    private val server: MockWebServer = MockWebServer().apply {
        setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = response(request)
        })
    }

    @Before
    fun setup() = server.start()

    @After
    fun stop() = server.shutdown()

    protected fun resolve(path: String): String = server.url(path).toString()

    @Throws(InterruptedException::class)
    protected abstract fun response(request: RecordedRequest): MockResponse
}
