package com.warrior.shows_notifier.sources.lostfilm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.warrior.shows_notifier.sources.ShowCollector
import com.warrior.shows_notifier.entities.Show
import com.warrior.shows_notifier.sources.Sources.LOST_FILM
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.logging.log4j.LogManager
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.net.URI
import java.util.*

/**
 * Created by warrior on 2/19/17.
 */
class LostFilmCollector(
        baseUrl: String = LOST_FILM.baseUrl,
        private val sourceName: String = LOST_FILM.sourceName
) : ShowCollector {

    private val logger = LogManager.getLogger(javaClass)
    private val api: LostFilmApi
    private val baseUri: URI = URI(baseUrl)

    init {
        val logging = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger(logger::debug))
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
                .build()
                .create(LostFilmApi::class.java)
    }

    override fun collect(rawIds: Set<Long>): List<Show> {
        val shows = ArrayList<Show>()

        var offset = 0
        var attempts = 0
        while (true) {
            try {
                val response = api.shows(offset).execute()
                if (response.isSuccessful) {
                    // `response.body()` is not null because `response.isSuccessful` is true
                    val lostfilmShows = response.body()!!.data
                    if (lostfilmShows.isEmpty()) break
                    shows += lostfilmShows.mapNotNull {
                        val show = it.toShow(sourceName, baseUri)
                        if (show.rawId !in rawIds) {
                            logger.info(show)
                            show
                        } else null
                    }
                    offset += 10
                    continue
                } else {
                    attempts++
                }
            } catch (e: IOException) {
                logger.error(e)
                attempts++
            }
            if (attempts > 5) {
                attempts = 0
                offset += 10
            } else {
                Thread.sleep(1000)
            }
        }

        return shows
    }
}
