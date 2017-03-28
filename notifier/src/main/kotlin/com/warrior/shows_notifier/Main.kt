package com.warrior.shows_notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.warrior.shows_notifier.crawlers.Crawler
import com.warrior.shows_notifier.crawlers.LostFilmCrawler
import com.warrior.shows_notifier.crawlers.NewStudioCrawler
import com.warrior.shows_notifier.entities.Episode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Supplier
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Created by warrior on 10/29/16.
 */
object Main {

    private const val CONFIG_FILENAME = "notifier-config.yaml"

    private const val SHOW_QUERY = """
            SELECT
              id, last_season, last_episode
            FROM shows
            WHERE shows.source_name = '%s' AND shows.title = ?;"""
    private const val SUBSCRIPTION_QUERY = """
            SELECT
              users.chat_id
            FROM users
              INNER JOIN subscriptions ON users.id = subscriptions.user_id
            WHERE subscriptions.show_id = ? AND users.active = true;"""
    private const val UPDATE_STATEMENT = """
            UPDATE shows
            SET last_season = ?, last_episode = ?
            WHERE shows.id = ?;
            """

    private val logger = LogManager.getLogger(javaClass)

    private lateinit var notifier: TelegramNotifier

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("start")

        val config = findConfig(CONFIG_FILENAME)
        val (url, username, password) = config.databaseConfig

        notifier = TelegramNotifier(config.telegramToken)

        DriverManager.getConnection(url, username, password).use { connection ->
            val sources = getSources(connection)
            for (source in sources) {
                when (source) {
                    "lostfilm" -> crawlNewSeriesAndNotify(connection, source, LostFilmCrawler())
                    "newstudio" -> crawlNewSeriesAndNotify(connection, source, NewStudioCrawler())
                    else -> logger.warn("Crawler for $source is not implemented yet. Do nothing")
                }
            }
        }

        notifier.shutdown(5, TimeUnit.MINUTES)
        logger.info("stop")
    }

    private fun getSources(connection: Connection): List<String> {
        return connection.createStatement().use { statement ->
            val queryResult = statement.executeQuery("SELECT DISTINCT source_name FROM shows;")

            val sources = ArrayList<String>()
            val builder = StringBuilder("sources:")
            while (queryResult.next()) {
                val source = queryResult.getString("source_name")
                sources += source
                builder.append("\n- ")
                        .append(source)
            }
            logger.debug(Supplier { builder.toString() })
            sources
        }
    }

    private fun crawlNewSeriesAndNotify(connection: Connection, source: String, crawler: Crawler) {
        val episodes = crawler.episodes()
        val episodesMap = episodes.groupBy({ it.showTitle }) { (_, season, episodeNumber) -> Episode(season, episodeNumber) }
        val showStatement = connection.prepareStatement(SHOW_QUERY.format(source))
        val subscriptionStatement = connection.prepareStatement(SUBSCRIPTION_QUERY)
        val updateStatement = connection.prepareStatement(UPDATE_STATEMENT)
        try {
            for ((title, episodes) in episodesMap) {
                showStatement.setString(1, title)
                val (showId, lastSavedEpisode) = showStatement.executeQuery().use { cursor ->
                    if (cursor.next()) {
                        val showId = cursor.getLong("id")
                        val lastSeason = cursor.getInt("last_season", -1)
                        val lastEpisode = cursor.getInt("last_episode", -1)
                        showId to Episode(lastSeason, lastEpisode)
                    } else null
                } ?: continue
                val newEpisodes = episodes.filter { it > lastSavedEpisode }.sorted()
                if (newEpisodes.isEmpty()) {
                    continue
                }
                subscriptionStatement.setLong(1, showId)
                val chatIds = subscriptionStatement.executeQuery().use { cursor ->
                    val chatIds = ArrayList<Long>()
                    while (cursor.next()) {
                        chatIds += cursor.getLong("chat_id")
                    }
                    chatIds
                }
                notify(chatIds, title, source, newEpisodes)
                val (season, episodeNumber) = newEpisodes.last()
                updateStatement.setInt(1, season)
                updateStatement.setInt(2, episodeNumber)
                updateStatement.setLong(3, showId)
                updateStatement.executeUpdate()
            }
        } catch (e: IOException) {
            showStatement.close()
            subscriptionStatement.close()
        }
    }

    private fun notify(chatIds: List<Long>, title: String, source: String, newEpisodes: List<Episode>) {
        for (chatId in chatIds) {
            notifier.notify(chatId, title, source, newEpisodes)
        }
    }

    private fun ResultSet.getInt(columnName: String, defaultValue: Int): Int {
        val value = getInt(columnName)
        return if (wasNull()) defaultValue else value
    }

    private fun findConfig(fileName: String): Config {
        val localFile = File(fileName)
        val stream = if (localFile.exists()) {
            localFile.inputStream().buffered()
        } else {
            javaClass.classLoader.getResourceAsStream(fileName) ?:
                    throw FileNotFoundException("Could not find a file: " + fileName)
        }
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        return mapper.readValue(stream)
    }
}
