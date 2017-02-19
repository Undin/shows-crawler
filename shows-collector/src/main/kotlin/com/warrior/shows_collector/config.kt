package com.warrior.shows_collector

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by warrior on 2/19/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
        @JsonProperty("db_url") val dbUrl: String,
        @JsonProperty("sources") val sources: List<String>
)
