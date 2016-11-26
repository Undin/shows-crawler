package com.warrior.shows_notifier.notifiers

import com.warrior.shows_notifier.Notifier
import com.warrior.shows_notifier.ShowEpisode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Created by warrior on 11/26/16.
 */
class TelegramNotifier(private val token: String, private val chatId: Int) : Notifier {

    override fun notify(episode: ShowEpisode, sourceUrl: String) {
        val (title, season, episodeNumber) = episode
        val message = "$title S${"%02d".format(season)}E${"%02d".format(episodeNumber)}\n$sourceUrl"

        try {
            val encodedMessage = URLEncoder.encode(message, Charsets.UTF_8.name())
            val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedMessage")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code < 200 || code >= 300) {
                System.err.println(connection.responseMessage)
                connection.errorStream.buffered().use {
                    it.copyTo(System.err)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(System.err)
        }
    }
}
