package com.warrior.shows_notifier

import com.warrior.shows_notifier.entities.ShowEpisode
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramNotifier(telegramToken: String) : Callback {

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

    fun notify(chatId: Long, showUrl: String, episodes: List<ShowEpisode>) {
        val (season, episodeNumber, title, episodeUrl) = episodes[0]
        val text = if (episodes.size == 1) {
            "$title ${formatEpisodeString(season, episodeNumber)}\n$episodeUrl"
        } else {
            val (lastSeason, lastEpisodeNumber) = episodes.last()
            "$title ${formatEpisodeString(season, episodeNumber)} - ${formatEpisodeString(lastSeason, lastEpisodeNumber)}\n$showUrl"
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
            = "S%02dE%02d".format(season, episodeNumber)
}
