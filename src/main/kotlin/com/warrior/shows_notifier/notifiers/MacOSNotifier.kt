package com.warrior.shows_notifier.notifiers

import com.warrior.shows_notifier.Notifier
import com.warrior.shows_notifier.ShowEpisode

/**
 * Created by warrior on 11/26/16.
 */
class MacOSNotifier : Notifier {

    private val TERMINAL_NOTIFIER = "TERMINAL_NOTIFIER"

    private val terminalNotifier: String?

    init {
        terminalNotifier = System.getenv(TERMINAL_NOTIFIER)
        if (terminalNotifier == null) {
            System.err.println("$TERMINAL_NOTIFIER variable is not set")
        }
    }

    override fun notify(episode: ShowEpisode, url: String) {
        if (terminalNotifier != null) {
            val (showTitle, season, episodeNumber) = episode
            val processBuilder = ProcessBuilder(terminalNotifier,
                    "-message", "S${"%02d".format(season)}E${"%02d".format(episodeNumber)}",
                    "-title", "\"$showTitle\"",
                    "-sound", "default",
                    "-timeout", Int.MAX_VALUE.toString(),
                    "-open", url)
            processBuilder.start()
        }
    }
}
