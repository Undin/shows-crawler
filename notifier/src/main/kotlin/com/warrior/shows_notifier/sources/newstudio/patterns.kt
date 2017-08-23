package com.warrior.shows_notifier.sources.newstudio

import java.util.regex.Pattern

const val STRING_ELEMENT_PATTERN = "(.*) \\(Сезон (\\d+), Серия (\\d+)(-\\d+)?\\) / (.*) \\(\\d{4}\\)"

val ELEMENT_PATTERN: Pattern = Pattern.compile(STRING_ELEMENT_PATTERN)