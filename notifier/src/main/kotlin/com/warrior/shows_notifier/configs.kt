package com.warrior.shows_notifier

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CollectorConfig(
        @JsonProperty("database") val databaseConfig: DatabaseConfig,
        @JsonProperty("sources") val sources: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotifierConfig(
        @JsonProperty("telegram_token") val telegramToken: String,
        @JsonProperty("database") val databaseConfig: DatabaseConfig
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseConfig(
        @JsonProperty("url") val url: String,
        @JsonProperty("username") val username: String,
        @JsonProperty("password") val password: String
)
