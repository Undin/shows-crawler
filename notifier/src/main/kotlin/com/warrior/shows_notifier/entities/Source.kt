package com.warrior.shows_notifier.entities

/**
 * Created by warrior on 3/18/17.
 */
data class Source(val id: Int, val name: String, val url: String) {
    override fun toString(): String = "$name ($id, $url)"
}
