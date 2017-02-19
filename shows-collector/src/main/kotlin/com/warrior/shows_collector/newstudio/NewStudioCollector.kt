package com.warrior.shows_collector.newstudio

import com.warrior.shows_collector.Show
import com.warrior.shows_collector.ShowCollector
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*

/**
 * Created by warrior on 2/19/17.
 */
class NewStudioCollector(private val newStudioId: Int) : ShowCollector {

    private val baseUrl = "http://newstudio.tv"

    override fun collect(): List<Show> {
        val shows = ArrayList<Show>()
        try {
            val document = Jsoup.connect(baseUrl).get()
            val elements = document.select("div#serialist li > a")
            for (e in elements) {
                val href = e.attr("href")
                val localTitle = e.text()
                val rawId = href.removePrefix("/viewforum.php?f=").toInt()
                val title = TitleExtractor.extractTitle(baseUrl + href)
                if (title != null) {
                    val show = Show(newStudioId, rawId, title, localTitle)
                    println(show)
                    shows += show
                } else {
                    println("can't extract title for $localTitle ($href)")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
        return shows
    }
}
