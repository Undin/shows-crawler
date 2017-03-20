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

/**
 * Created by warrior on 2/19/17.
 */

object Main {

    private const val CONFIG_NAME = "collector-config.yaml"

    private val LOGGER = LogManager.getLogger(javaClass)

    @JvmStatic
    fun main(args: Array<String>) {
        val config = findConfig(CONFIG_NAME)

        DriverManager.getConnection(config.databaseUrl).use { connection ->
            val sources = sourcesIds(config.sources, connection)
            val collectors = collectors(sources)
            collectShows(collectors, connection)
        }
    }

    private fun collectShows(collectors: List<ShowCollector>, connection: Connection) {
        for (collector in collectors) {
            val shows = collector.collect()
            val statement = connection.prepareStatement("""
            INSERT INTO shows (source_id, raw_id, title, local_title, last_season, last_episode)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING;""")
            statement.use {
                try {
                    for (show in shows) {
                        statement.setInt(1, show.sourceId)
                        statement.setInt(2, show.rawId)
                        statement.setString(3, show.title)
                        statement.setString(4, show.localTitle)
                        statement.setInt(5, show.season)
                        statement.setInt(6, show.episodeNumber)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                } catch (e: Exception) {
                    LOGGER.error(e)
                }
            }
        }
    }

    private fun PreparedStatement.setInt(parameterIndex: Int, value: Int?) {
        if (value != null) {
            setInt(parameterIndex, value)
        } else {
            setNull(parameterIndex, Types.INTEGER)
        }
    }

    private fun collectors(sources: List<Pair<String, Int>>): List<ShowCollector> {
        val collectors = ArrayList<ShowCollector>()
        for ((name, id) in sources) {
            val collector = when (name) {
                "lostfilm" -> LostFilmCollector(id)
                "newstudio" -> NewStudioCollector(id)
                else -> {
                    LOGGER.warn("collector for $name is not implemented yet")
                    null
                }
            }
            if (collector != null) {
                collectors += collector
            }
        }
        return collectors
    }

    private fun sourcesIds(sourceNames: List<String>, connection: Connection): List<Pair<String, Int>> {
        val sources = ArrayList<Pair<String, Int>>()
        connection.prepareStatement("SELECT id FROM sources WHERE name = ?;")
                .use { statement ->
                    for (name in sourceNames) {
                        val id = try {
                            statement.setString(1, name)
                            statement.executeQuery().use { result ->
                                if (result.next()) result.getInt("id") else null
                            }
                        } catch (e: Exception) {
                            LOGGER.error(e)
                            null
                        }
                        if (id != null) {
                            sources += Pair(name, id)
                        }
                    }
                }
        return sources
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
