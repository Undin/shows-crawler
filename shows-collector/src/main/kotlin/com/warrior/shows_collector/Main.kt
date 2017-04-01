package com.warrior.shows_collector

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.warrior.shows_collector.lostfilm.LostFilmCollector
import com.warrior.shows_collector.newstudio.NewStudioCollector
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import java.sql.*
import java.util.*

object Main {

    private const val CONFIG_NAME = "collector-config.yaml"

    private val LOGGER = LogManager.getLogger(javaClass)

    @JvmStatic
    fun main(args: Array<String>) {
        val config = findConfig(CONFIG_NAME)
        val (url, username, password) = config.databaseConfig

        DriverManager.getConnection(url, username, password).use { connection ->
            val collectors = collectors(config.sources)
            collectShows(collectors, connection)
        }
    }

    private fun collectShows(collectors: List<ShowCollector>, connection: Connection) {
        for (collector in collectors) {
            val shows = collector.collect()
            val statement = connection.prepareStatement("""
            INSERT INTO shows (source_name, raw_id, title, local_title, show_url)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT shows_source_name_raw_id_unique DO UPDATE
              SET show_url = EXCLUDED.show_url;""")
            statement.use {
                try {
                    for (show in shows) {
                        statement.setString(1, show.sourceName)
                        statement.setInt(2, show.rawId)
                        statement.setString(3, show.title)
                        statement.setString(4, show.localTitle)
                        statement.setString(5, show.showUrl)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                } catch (e: Exception) {
                    LOGGER.error(e)
                }
            }
        }
    }

    private fun collectors(sources: List<String>): List<ShowCollector> {
        val collectors = ArrayList<ShowCollector>()
        for (source in sources) {
            val collector = when (source) {
                "lostfilm" -> LostFilmCollector(source)
                "newstudio" -> NewStudioCollector(source)
                else -> {
                    LOGGER.warn("collector for $source is not implemented yet")
                    null
                }
            }
            if (collector != null) {
                collectors += collector
            }
        }
        return collectors
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
