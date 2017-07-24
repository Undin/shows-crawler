package com.warrior.shows_notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.warrior.shows_notifier.entities.Show
import com.warrior.shows_notifier.sources.ShowCollector
import com.warrior.shows_notifier.sources.alexfilm.AlexFilmCollector
import com.warrior.shows_notifier.sources.lostfilm.LostFilmCollector
import com.warrior.shows_notifier.sources.newstudio.NewStudioCollector
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import java.sql.Connection
import java.sql.DriverManager

object Collector {

    private const val CONFIG_NAME = "collector-config.yaml"

    private val LOGGER = LogManager.getLogger(javaClass)

    private val RAW_IDS_QUERY = "SELECT raw_id from shows where source_name = ?"

    private val INSERT_SHOWS_STATEMENT = """
            INSERT INTO shows (source_name, raw_id, title, local_title, show_url)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT shows_source_name_raw_id_unique DO UPDATE
              SET show_url = EXCLUDED.show_url;
    """

    @JvmStatic
    fun main(args: Array<String>) {
        val config = findConfig(CONFIG_NAME)
        val (url, username, password) = config.databaseConfig

        DriverManager.getConnection(url, username, password).use { connection ->
            collectShows(config.sources, connection)
        }
    }

    private fun collectShows(sources: List<String>, connection: Connection) {
        connection.prepareStatement(RAW_IDS_QUERY).use { selectStatement ->
            for (source in sources) {
                val collector = createCollector(source) ?: continue
                val rawIds = try {
                    selectStatement.setString(1, source)
                    selectStatement.executeQuery().use { cursor ->
                        val rawIds = mutableSetOf<Long>()
                        while (cursor.next()) {
                            rawIds += cursor.getLong("raw_id")
                        }
                        rawIds
                    }
                } catch (e: Exception) {
                    LOGGER.error(e.message, e)
                    emptySet<Long>()
                }
                val shows = collector.collect(rawIds)
                insertShows(connection, shows)
            }
        }
    }

    private fun insertShows(connection: Connection, shows: List<Show>) {
        try {
            connection.prepareStatement(INSERT_SHOWS_STATEMENT).use { statement ->
                for ((sourceName, rawId, title, localTitle, showUrl) in shows) {
                    statement.setString(1, sourceName)
                    statement.setLong(2, rawId)
                    statement.setString(3, title)
                    statement.setString(4, localTitle)
                    statement.setString(5, showUrl)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
        }
    }

    private fun createCollector(source: String): ShowCollector? = when (source) {
        "alexfilm" -> AlexFilmCollector(source)
        "lostfilm" -> LostFilmCollector(source)
        "newstudio" -> NewStudioCollector(source)
        else -> {
            LOGGER.warn("collector for $source is not implemented yet")
            null
        }
    }

    private fun findConfig(fileName: String): CollectorConfig {
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
