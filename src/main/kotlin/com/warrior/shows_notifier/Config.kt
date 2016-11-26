package com.warrior.shows_notifier

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by warrior on 11/26/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
        @JsonProperty("terminal_notifier") val terminalNotifier: String?,
        @JsonProperty("telegram_params") val telegramParams: TelegramParams?,
        @JsonProperty("shows") val shows: Map<String, List<String>>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramParams(
        @JsonProperty("token") val token: String,
        @JsonProperty("chat_id") val chatId: Int
)
