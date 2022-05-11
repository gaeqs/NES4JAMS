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

package io.github.gaeqs.nes4jams.gui.nes

import io.github.gaeqs.nes4jams.cartridge.Cartridge
import io.github.gaeqs.nes4jams.cartridge.ConsoleType
import io.github.gaeqs.nes4jams.cartridge.TVType
import io.github.gaeqs.nes4jams.data.*
import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import io.github.gaeqs.nes4jams.gui.action.folder.NewNESFileWindow
import io.github.gaeqs.nes4jams.gui.action.folder.NewPCXFileWindow
import io.github.gaeqs.nes4jams.gui.pcx.PCXVisualizer
import io.github.gaeqs.nes4jams.ppu.Mirror
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.util.extension.fit
import io.github.gaeqs.nes4jams.util.extension.orNull
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import net.jamsimulator.jams.gui.configuration.RegionDisplay
import net.jamsimulator.jams.gui.editor.FileEditor
import net.jamsimulator.jams.gui.editor.holder.FileEditorTab
import net.jamsimulator.jams.gui.util.AnchorUtils
import net.jamsimulator.jams.gui.util.PixelScrollPane
import net.jamsimulator.jams.gui.util.Spacer
import net.jamsimulator.jams.gui.util.value.RangedIntegerValueEditor
import net.jamsimulator.jams.gui.util.value.ValueEditors
import net.jamsimulator.jams.language.wrapper.LanguageButton
import net.jamsimulator.jams.language.wrapper.LanguageLabel
import net.jamsimulator.jams.task.LanguageTask
import tornadofx.*

class NESFileEditor(private val tab: FileEditorTab) : SplitPane(), FileEditor {

    val cartridge = Cartridge(tab.file, 0)

    init {
        val header = cartridge.header
        val pcx = PictureExchangeImage.fromCHRData(cartridge.chrMemory.toByteArray())

        padding = Insets(5.0)

        val headerScroll = PixelScrollPane().fit()
        val headerBox = VBox().apply {
            alignment = Pos.TOP_LEFT
            padding = Insets(10.0)
            isFillWidth = true
            spacing = 5.0
        }
        headerScroll.content = headerBox

        headerBox.children += RegionDisplay(NES4JAMS_NES_FILE_REGION_GENERAL)
        headerBox.children += LanguageLabel(
            NES4JAMS_NES_FILE_HEADER_FORMAT,
            "{FORMAT}", if (header.isINES2) "iNES 2.0" else "iNES 1.0"
        )

        headerBox.children += RegionDisplay(NES4JAMS_NES_FILE_REGION_MAPPER)

        val mapperEditor = ValueEditors.getByName(RangedIntegerValueEditor.NAME).orNull()?.build()
                as? RangedIntegerValueEditor
        if (mapperEditor != null) {
            mapperEditor.min = 0
            mapperEditor.max = 0b111111111111
            mapperEditor.currentValue = header.mapper.toInt()
            mapperEditor.addListener {
                header.mapper = it.toUShort()
            }
            headerBox.children += mapperEditor.buildConfigNode(LanguageLabel(NES4JAMS_NES_FILE_MAPPER))
        }

        val subMapperEditor = ValueEditors.getByName(RangedIntegerValueEditor.NAME).orNull()?.build()
                as? RangedIntegerValueEditor
        if (subMapperEditor != null) {
            subMapperEditor.min = 0
            subMapperEditor.max = 0b1111
            subMapperEditor.currentValue = header.subMapper.toInt()
            subMapperEditor.addListener { header.subMapper = it.toUByte() }
            headerBox.children += subMapperEditor.buildConfigNode(LanguageLabel(NES4JAMS_NES_FILE_SUB_MAPPER))
        }

        headerBox.children += RegionDisplay(NES4JAMS_NES_FILE_REGION_VISUALIZATION)

        val mirroringEditor = ValueEditors.getByType(Mirror::class.java).orNull()?.build()
        if (mirroringEditor != null) {
            mirroringEditor.currentValue = header.mirroring
            mirroringEditor.addListener { header.mirroring = it }
            headerBox.children += mirroringEditor.buildConfigNode(LanguageLabel(NES4JAMS_NES_FILE_MIRRORING))
        }

        val tvTypeEditor = ValueEditors.getByType(TVType::class.java).orNull()?.build()
        if (tvTypeEditor != null) {
            tvTypeEditor.currentValue = header.tvType
            tvTypeEditor.addListener { header.tvType = it }
            headerBox.children += tvTypeEditor.buildConfigNode(LanguageLabel(NES4JAMS_NES_FILE_TV_TYPE))
        }

        val consoleTypeEditor = ValueEditors.getByType(ConsoleType::class.java).orNull()?.build()
        if (consoleTypeEditor != null) {
            consoleTypeEditor.currentValue = header.consoleType
            consoleTypeEditor.addListener { header.consoleType = it }
            headerBox.children += consoleTypeEditor.buildConfigNode(LanguageLabel(NES4JAMS_NES_FILE_CONSOLE_TYPE))
        }

        headerBox.children += RegionDisplay(NES4JAMS_NES_FILE_REGION_SIZE)

        headerBox.children += LanguageLabel(
            NES4JAMS_NES_FILE_PROGRAM_SIZE, "{SIZE}", header.prgRomSize.toString()
        )
        headerBox.children += LanguageLabel(
            NES4JAMS_NES_FILE_GRAPHIC_SIZE, "{SIZE}", header.chrRomSize.toString()
        )

        headerBox.add(Spacer(0.0, 1.0).apply { VBox.setVgrow(this, Priority.ALWAYS) })


        val buttonsBox = FlowPane()
        buttonsBox.children += LanguageButton(NES4JAMS_NES_FILE_RUN).apply {
            setOnAction {
                val project = tab.workingPane.projectTab.project
                if (project is NESProject) {
                    cartridge.refreshHeader()
                    project.taskExecutor.execute(LanguageTask.of(NES4JAMS_TASK_LOADING_CARTRIDGE) {
                        project.openSimulation(cartridge)
                    })
                }
            }
        }
        buttonsBox.children += LanguageButton(NES4JAMS_NES_FILE_EXPORT_GRAPHICS).apply {
            setOnAction {
                NewPCXFileWindow.open(tab.file.parentFile) { file -> file.outputStream().use { pcx.write(it) } }
            }
        }
        buttonsBox.children += LanguageButton(NES4JAMS_NES_FILE_SAVE_COPY).apply {
            setOnAction {
                NewNESFileWindow.open(tab.file.parentFile) { file ->
                    file.outputStream().use {
                        header.write(it)
                        it.write(cartridge.prgMemory.asByteArray())
                        it.write(cartridge.chrMemory.asByteArray())
                    }
                }
            }
        }
        headerBox.children += buttonsBox

        items += headerScroll

        val imageScroll = PixelScrollPane().apply {
            isPannable = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }

        val visualizer = PCXVisualizer(pcx)
        imageScroll.content = visualizer
        imageScroll.viewportBoundsProperty().addListener { obs, old, new ->
            visualizer.zoom = new.width / pcx.header.width
        }

        items += imageScroll
    }

    override fun supportsActionRegion(region: String): Boolean {
        return "EDITOR" == region
    }

    override fun getTab() = tab
    override fun onClose() {}
    override fun save() {}
    override fun reload() {}

    override fun addNodesToTab(anchor: AnchorPane) {
        anchor.children.addAll(this)
        AnchorUtils.setAnchor(this, 0.0, 0.0, 0.0, 0.0)
    }
}