/*
 *  MIT License
 *
 *  Copyright (c) 2021 Gael Rial Costas
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.gaeqs.nes4jams

import io.github.gaeqs.nes4jams.file.nes.NESFileType
import io.github.gaeqs.nes4jams.file.pcx.PCXFileType
import io.github.gaeqs.nes4jams.gui.action.folder.*
import io.github.gaeqs.nes4jams.gui.project.editor.NESAssemblyFileEditor
import io.github.gaeqs.nes4jams.gui.util.converter.NESValueConverters
import io.github.gaeqs.nes4jams.gui.util.value.NESValueEditors
import io.github.gaeqs.nes4jams.project.NESProjectType
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.event.general.JAMSApplicationPostInitEvent
import net.jamsimulator.jams.event.general.JAMSPostInitEvent
import net.jamsimulator.jams.file.AssemblyFileType
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.plugin.Plugin
import java.io.InputStream

class NES4JAMS : Plugin() {

    companion object {
        lateinit var INSTANCE: NES4JAMS
            private set
    }

    override fun onEnable() {
        INSTANCE = this
        if (JamsApplication.isLoaded()) {
            load()
            loadApplication()
        }
    }

    @Listener
    private fun onPostInit(event: JAMSPostInitEvent) = load()

    @Listener
    private fun onApplicationPostInit(event: JAMSApplicationPostInitEvent) = loadApplication()

    private fun load() {
        loadLanguages()
        loadThemes()

        NESValueConverters.setupConverters()
        NESValueEditors.setupEditor()

        Jams.getProjectTypeManager() += NESProjectType.INSTANCE

        AssemblyFileType.INSTANCE.addBuilder(NESProjectType.INSTANCE) { NESAssemblyFileEditor(it) }

        Jams.getFileTypeManager() += PCXFileType.INSTANCE
        Jams.getFileTypeManager() += NESFileType.INSTANCE

        Jams.getLanguageManager().registerListeners(this, true)
    }

    private fun loadApplication() {
        JamsApplication.getActionManager() += FolderActionNewPCXFile()
        JamsApplication.getActionManager() += FolderActionAddSpriteToAssembler()
        JamsApplication.getActionManager() += FolderActionAddAllSpritesToAssembler()
        JamsApplication.getActionManager() += FolderActionRemoveSpriteFromAssembler()
        JamsApplication.getActionManager() += FolderActionRemoveAllSpritesFromAssembler()
    }

    private fun loadLanguages() {
        val list = mutableListOf<InputStream>()
        resource("/languages/english.jlang").ifPresent { list.add(it) }
        resource("/languages/spanish.jlang").ifPresent { list.add(it) }
        Jams.getLanguageManager().loadLanguages(list, true)
    }

    private fun loadThemes() {
        val list = mutableListOf<InputStream>()
        resource("/gui/themes/dark_theme.jtheme").ifPresent { list.add(it) }
        resource("/gui/themes/light_theme.jtheme").ifPresent { list.add(it) }
        JamsApplication.getThemeManager().loadThemes(list, true)
    }

}