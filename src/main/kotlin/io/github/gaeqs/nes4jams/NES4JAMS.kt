package io.github.gaeqs.nes4jams

import io.github.gaeqs.nes4jams.project.NESProjectType
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.event.general.JAMSPostInitEvent
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.plugin.Plugin
import java.io.InputStream

class NES4JAMS : Plugin() {

    companion object {
        lateinit var INSTANCE : NES4JAMS
            private set
    }

    override fun onEnable() {
        INSTANCE = this
        if (JamsApplication.isLoaded()) {
            load()
        }
    }

    override fun onDisable() {
        println("Bye!")
    }

    @Listener
    private fun onPostInit(event: JAMSPostInitEvent) = load()

    private fun load() {
        loadLanguages()
        loadThemes()
        Jams.getProjectTypeManager() += NESProjectType.INSTANCE
        Jams.getLanguageManager().registerListeners(this, true)
    }

    private fun loadLanguages() {
        val list = mutableListOf<InputStream>()
        resource("/languages/english.jlang").ifPresent { list.add(it) }
        resource("/languages/spanish.jlang").ifPresent { list.add(it) }
        Jams.getLanguageManager().loadLanguages(list, true)
    }

    private fun loadThemes() {
        val list = mutableListOf<InputStream>()
        //resource("/themes/dark_theme.jtheme").ifPresent { list.add(it) }
        JamsApplication.getThemeManager().loadThemes(list, true)
    }

}