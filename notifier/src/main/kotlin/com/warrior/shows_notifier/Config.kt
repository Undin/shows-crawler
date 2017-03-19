package com.warrior.shows_notifier

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by warrior on 11/26/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
        @JsonProperty("database_url") val databaseUrl: String,
        @JsonProperty("telegram_token") val telegramToken: String
)
