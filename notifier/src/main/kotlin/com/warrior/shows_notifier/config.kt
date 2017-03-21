package com.warrior.shows_notifier

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by warrior on 11/26/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
        @JsonProperty("telegram_token") val telegramToken: String,
        @JsonProperty("database") val databaseConfig: DatabaseConfig
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseConfig(
        @JsonProperty("url") val url: String,
        @JsonProperty("username") val username: String,
        @JsonProperty("password") val password: String
)
