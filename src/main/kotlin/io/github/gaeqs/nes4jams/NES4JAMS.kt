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

import io.github.gaeqs.nes4jams.cartridge.mapper.MapperBuilderManager
import io.github.gaeqs.nes4jams.file.nes.NESFileType
import io.github.gaeqs.nes4jams.file.pcx.PCXFileType
import io.github.gaeqs.nes4jams.gui.action.folder.*
import io.github.gaeqs.nes4jams.gui.project.editor.NESAssemblyFileEditor
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.inspection.NESInspectorManager
import io.github.gaeqs.nes4jams.gui.simulation.memory.representation.NESNumberRepresentationManager
import io.github.gaeqs.nes4jams.gui.simulation.memory.view.NESMemoryViewManager
import io.github.gaeqs.nes4jams.gui.util.converter.NESValueConverters
import io.github.gaeqs.nes4jams.gui.util.value.NESValueEditors
import io.github.gaeqs.nes4jams.project.NESProjectType
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerDeviceBuilderManager
import io.github.gaeqs.nes4jams.util.extension.orNull
import io.github.gaeqs.nes4jams.util.manager
import io.github.gaeqs.nes4jams.util.managerOf
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.configuration.ConfigurationAttachment
import net.jamsimulator.jams.configuration.RootConfiguration
import net.jamsimulator.jams.configuration.format.ConfigurationFormat
import net.jamsimulator.jams.configuration.format.ConfigurationFormatJSON
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.event.general.JAMSApplicationPostInitEvent
import net.jamsimulator.jams.event.general.JAMSPostInitEvent
import net.jamsimulator.jams.file.AssemblyFileType
import net.jamsimulator.jams.file.FileType
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.theme.ThemeManager
import net.jamsimulator.jams.gui.theme.exception.ThemeLoadException
import net.jamsimulator.jams.language.Language
import net.jamsimulator.jams.language.LanguageManager
import net.jamsimulator.jams.language.exception.LanguageLoadException
import net.jamsimulator.jams.plugin.Plugin
import net.jamsimulator.jams.project.ProjectType
import java.nio.charset.StandardCharsets
import java.nio.file.Path

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
        NESValueConverters.setupConverters()
        NESValueEditors.setupEditor()

        loadLanguages()

        Jams.REGISTRY.registerSecondary(NESInspectorManager.INSTANCE)
        Jams.REGISTRY.registerPrimary(MapperBuilderManager.INSTANCE)
        Jams.REGISTRY.registerPrimary(NESMemoryViewManager.INSTANCE)
        Jams.REGISTRY.registerPrimary(NESNumberRepresentationManager.INSTANCE)
        Jams.REGISTRY.registerPrimary(NESControllerDeviceBuilderManager.INSTANCE)

        val fileManager = managerOf<FileType>()
        val assemblyFileType = fileManager[AssemblyFileType.NAME].orNull()
        if (assemblyFileType is AssemblyFileType) {
            assemblyFileType.addBuilder(NESProjectType.INSTANCE) { NESAssemblyFileEditor(it) }
        }
        fileManager += PCXFileType.INSTANCE
        fileManager += NESFileType.INSTANCE

        managerOf<Language>().registerListeners(this, true)

        loadConfiguration()
    }

    private fun loadApplication() {
        managerOf<ProjectType<*>>() += NESProjectType.INSTANCE
        loadThemes()
        JamsApplication.getActionManager() += FolderActionNewPCXFile()
        JamsApplication.getActionManager() += FolderActionAddSpriteToAssembler()
        JamsApplication.getActionManager() += FolderActionAddAllSpritesToAssembler()
        JamsApplication.getActionManager() += FolderActionRemoveSpriteFromAssembler()
        JamsApplication.getActionManager() += FolderActionRemoveAllSpritesFromAssembler()
    }

    private fun loadLanguages() {
        val manager = manager<LanguageManager>()
        val jarResource = javaClass.getResource("/languages")
        if (jarResource != null) {
            manager.loadLanguagesInDirectory(this, Path.of(jarResource.toURI()))
                .forEach { (_: Path, e: LanguageLoadException) -> e.printStackTrace() }
        }
        manager.refresh()
    }

    private fun loadConfiguration() {
        val format = managerOf<ConfigurationFormat>().getOrNull(ConfigurationFormatJSON.NAME)
        resource("/configurations/nes4jams.json").ifPresent {
            Jams.getMainConfiguration().data.addNotPresentValues(
                RootConfiguration(it.reader(StandardCharsets.UTF_8), format)
            )
            it.close()
        }

        resource("/configurations/nes4jams_meta.json").ifPresent {
            Jams.getMainConfiguration().metadata.addAttachment(
                ConfigurationAttachment(
                    this,
                    RootConfiguration(it.reader(StandardCharsets.UTF_8), format),
                    0
                )
            )
            it.close()
        }
    }

    private fun loadThemes() {
        val manager = manager<ThemeManager>()
        val jarResource = javaClass.getResource("/gui/themes")
        if (jarResource != null) {
            manager.loadThemesInDirectory(this, Path.of(jarResource.toURI()))
                .forEach { (_: Path, e: ThemeLoadException) -> e.printStackTrace() }
        }
        manager.refresh()
    }

}