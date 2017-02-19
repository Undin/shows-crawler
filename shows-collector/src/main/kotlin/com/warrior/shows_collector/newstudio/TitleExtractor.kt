package com.warrior.shows_collector.newstudio

import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Pattern

/**
 * Created by warrior on 2/19/17.
 */
internal object TitleExtractor {

    private val pattern = Pattern.compile("/ (.*?) \\(")

    @JvmStatic
    fun main(args: Array<String>) {
        val title = extractTitle("http://newstudio.tv/viewforum.php?f=194")
        println(title)
    }

    fun extractTitle(url: String): String? {
        try {
            val document = Jsoup.connect(url).get()
            val element = document.select("a.torTopic.tt-text > b").first()
            element?.apply {
                val matcher = pattern.matcher(text())
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}