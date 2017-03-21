package com.warrior.shows_collector

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by warrior on 2/19/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
        @JsonProperty("database") val databaseConfig: DatabaseConfig,
        @JsonProperty("sources") val sources: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseConfig(
        @JsonProperty("url") val url: String,
        @JsonProperty("username") val username: String,
        @JsonProperty("password") val password: String
)
