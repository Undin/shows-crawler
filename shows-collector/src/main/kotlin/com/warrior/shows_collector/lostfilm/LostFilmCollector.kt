package com.warrior.shows_collector.lostfilm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.warrior.shows_collector.Show
import com.warrior.shows_collector.ShowCollector
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.logging.log4j.LogManager
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.util.*

/**
 * Created by warrior on 2/19/17.
 */
class LostFilmCollector(private val lostFilmId: Int) : ShowCollector {

    private val logger = LogManager.getLogger(javaClass)

    private val api: LostFilmApi

    init {
        val logging = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger(logger::info))
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        api = Retrofit.Builder()
                .baseUrl("http://www.lostfilm.tv/")
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
                .build()
                .create(LostFilmApi::class.java)
    }

    override fun collect(): List<Show> {
        val shows = ArrayList<Show>()

        var offset = 0
        var attempts = 0
        while (true) {
            try {
                val response = api.shows(offset).execute()
                if (response.isSuccessful) {
                    val lostfilmShows = response.body().data
                    if (lostfilmShows.isEmpty()) {
                        break
                    } else {
                        shows += lostfilmShows.map { it.toShow(lostFilmId) }
                        offset += 10
                        continue
                    }
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
