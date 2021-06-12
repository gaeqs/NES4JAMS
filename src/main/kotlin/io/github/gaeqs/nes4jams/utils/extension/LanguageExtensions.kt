package io.github.gaeqs.nes4jams.utils.extension

import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.language.Language

val DEFAULT_LANGUAGE: Language get() = Jams.getLanguageManager().default

operator fun Language.get(node: String): String = getOrDefault(node)