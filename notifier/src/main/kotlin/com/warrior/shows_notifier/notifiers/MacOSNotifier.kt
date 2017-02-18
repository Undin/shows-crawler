package com.warrior.shows_notifier.notifiers

import com.warrior.shows_notifier.Notifier
import com.warrior.shows_notifier.ShowEpisode

/**
 * Created by warrior on 11/26/16.
 */
class MacOSNotifier(private val terminalNotifier: String) : Notifier {

    override fun notify(episode: ShowEpisode, sourceUrl: String) {
        val (showTitle, season, episodeNumber) = episode
        val processBuilder = ProcessBuilder(terminalNotifier,
                "-message", "S${"%02d".format(season)}E${"%02d".format(episodeNumber)}",
                "-title", "\"$showTitle\"",
                "-sound", "default",
                "-timeout", Int.MAX_VALUE.toString(),
                "-open", sourceUrl)
        processBuilder.start()
    }
}
