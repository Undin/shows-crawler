package com.warrior.shows_notifier.sources

enum class Sources(val sourceName: String, val baseUrl: String) {
    ALEX_FILM("alexfilm", "http://alexfilm.cc/"),
    LOST_FILM("lostfilm", "https://www.lostfilm.tv/"),
    NEW_STUDIO("newstudio", "http://newstudio.tv/")
}
