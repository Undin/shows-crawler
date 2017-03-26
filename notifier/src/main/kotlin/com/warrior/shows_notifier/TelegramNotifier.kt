package com.warrior.shows_notifier

import com.warrior.shows_notifier.entities.Episode
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramNotifier(telegramToken: String) : Callback {

    private val SOURCES_URLS = mapOf(
            "lostfilm" to "http://www.lostfilm.tv/",
            "newstudio" to "http://newstudio.tv/"
    )

    private val logger = LogManager.getLogger(javaClass)
    private val url: HttpUrl = HttpUrl.parse("https://api.telegram.org/bot$telegramToken/sendMessage")
    private val client: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor(logger::info)
        logging.level = HttpLoggingInterceptor.Level.BASIC
        client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
    }

    fun notify(chatId: Long, title: String, source: String, episodes: List<Episode>) {
        val (season, episodeNumber) = episodes[0]
        val sourceUrl = SOURCES_URLS[source] ?: ""
        val text = if (episodes.size == 1) {
            "$title ${formatEpisodeString(season, episodeNumber)}\n$sourceUrl"
        } else {
            val (lastSeason, lastEpisodeNumber) = episodes.last()
            "$title ${formatEpisodeString(season, episodeNumber)} - ${formatEpisodeString(lastSeason, lastEpisodeNumber)}\n$sourceUrl"
        }

        val body = FormBody.Builder()
                .add("chat_id", chatId.toString())
                .add("text", text)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
        client.newCall(request)
                .enqueue(this)
    }

    fun shutdown(timeout: Long, unit: TimeUnit) {
        val executorService = client.dispatcher().executorService()
        executorService.shutdown()
        executorService.awaitTermination(timeout, unit)
    }

    override fun onFailure(call: Call, e: IOException) {
        logger.error(e)
    }

    override fun onResponse(call: Call, response: Response) {
    }

    private fun formatEpisodeString(season: Int, episodeNumber: Int): String
            = "S${"%02d".format(season)}E${"%02d".format(episodeNumber)}"
}
