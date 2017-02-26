package com.warrior.shows_collector

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.warrior.shows_collector.lostfilm.LostFilmCollector
import com.warrior.shows_collector.newstudio.NewStudioCollector
import java.io.File
import java.io.FileNotFoundException
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

/**
 * Created by warrior on 2/19/17.
 */
const val CONFIG_NAME = "collector-config.yaml"

fun main(args: Array<String>) {
    val configFile = findFile(CONFIG_NAME)
    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    val config: Config = mapper.readValue(configFile)

    val connection = DriverManager.getConnection(config.dbUrl)

    connection.use {
        val sources = sourcesIds(config.sources, connection)
        val collectors = collectors(sources)
        collectShows(collectors, connection)
    }
}

private fun collectShows(collectors: List<ShowCollector>, connection: Connection) {
    for (collector in collectors) {
        val shows = collector.collect()
        val statement = connection.prepareStatement("INSERT INTO shows (source_id, raw_id, title, local_title) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;")
        statement.use {
            try {
                for (show in shows) {
                    statement.setInt(1, show.sourceId)
                    statement.setInt(2, show.rawId)
                    statement.setString(3, show.title)
                    statement.setString(4, show.localTitle)
                    statement.addBatch()
                }
                statement.executeBatch()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun collectors(sources: List<Pair<String, Int>>): List<ShowCollector> {
    val collectors = ArrayList<ShowCollector>()
    for ((name, id) in sources) {
        val collector = when (name) {
            "lostfilm" -> LostFilmCollector(id)
            "newstudio" -> NewStudioCollector(id)
            else -> {
                println("collector for $name is not implemented yet")
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
    for (name in sourceNames) {
        val statement = connection.createStatement()
        val id = statement.use {
            try {
                val result = statement.executeQuery("SELECT (id) FROM sources WHERE name='$name';")
                result.getInt("id")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        if (id != null) {
            sources += Pair(name, id)
        }
    }
    return sources
}

private fun findFile(fileName: String): File {
    val localFile = File(fileName)
    if (localFile.exists()) {
        return localFile
    } else {
        // it's terrible way to take resources
        // but kotlin doesn't have syntax to get Class object from top level function
        val resourceUrl = object {}::class.java.classLoader.getResource(fileName)
        if (resourceUrl != null) {
            return File(resourceUrl.file)
        } else {
            throw FileNotFoundException("Could not find a file: " + fileName)
        }
    }
}
