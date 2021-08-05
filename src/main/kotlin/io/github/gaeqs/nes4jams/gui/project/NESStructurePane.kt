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

package io.github.gaeqs.nes4jams.gui.project

import io.github.gaeqs.nes4jams.data.NES4JAMS_BAR_SPRITES_TO_ASSEMBLE
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.util.extension.fit
import javafx.scene.control.Tab
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.bar.BarPosition
import net.jamsimulator.jams.gui.bar.BarSnapshot
import net.jamsimulator.jams.gui.bar.mode.BarSnapshotViewModePane
import net.jamsimulator.jams.gui.editor.FileEditorHolder
import net.jamsimulator.jams.gui.editor.FileEditorHolderHolder
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.mips.sidebar.FilesToAssembleSidebar
import net.jamsimulator.jams.gui.project.ProjectFolderExplorer
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.gui.util.PixelScrollPane
import net.jamsimulator.jams.gui.util.log.Log
import net.jamsimulator.jams.gui.util.log.SimpleLog
import net.jamsimulator.jams.language.Messages
import tornadofx.clear
import java.io.File
import java.util.function.Consumer

class NESStructurePane(parent: Tab, projectTab: ProjectTab, val project: NESProject) :
    WorkingPane(parent, projectTab, null, false), FileEditorHolderHolder {

    val holder = FileEditorHolder(this)

    lateinit var explorer: ProjectFolderExplorer
        private set

    lateinit var filesToAssembleSidebar: FilesToAssembleSidebar
        private set

    lateinit var spritesToAssembleSidebar: FilesToAssembleSidebar
        private set

    lateinit var log: Log
        private set

    private val paneButtons = NESStructurePaneButtons(project)

    override fun getFileEditorHolder(): FileEditorHolder = center as FileEditorHolder

    init {
        center = holder
        init()
        loadExplorer()
        loadFilesToAssemble()
        loadSpritesToAssemble()
        loadLog()
    }

    override fun getLanguageNode(): String = Messages.PROJECT_TAB_STRUCTURE

    override fun populateHBox(hbox: HBox) {
        hbox.clear()
        hbox.children += paneButtons.nodes
    }


    override fun onClose() {
        super.onClose()
        explorer.dispose()
    }

    override fun saveAllOpenedFiles() {
        fileEditorHolder.saveAll(true)
    }

    fun openFile(file: File) {
        val holder = center as FileEditorHolder
        holder.openFile(file)
    }

    private fun loadExplorer() {
        val pane = PixelScrollPane().fit()
        explorer =
            ProjectFolderExplorer(project, setOf(project.data.filesToAssemble, project.data.spritesToAssemble), pane)
        pane.content = explorer

        explorer.fileOpenAction = Consumer { openFile(it.file) }

        barMap.registerSnapshot(
            BarSnapshot(
                "explorer",
                pane,
                BarPosition.LEFT_TOP,
                BarSnapshotViewModePane.INSTANCE,
                true,
                Icons.SIDEBAR_EXPLORER,
                Messages.BAR_EXPLORER_NAME
            )
        )
    }

    private fun loadFilesToAssemble() {
        val pane = PixelScrollPane().fit()
        filesToAssembleSidebar = FilesToAssembleSidebar(project, project.data.filesToAssemble, pane)
        pane.content = filesToAssembleSidebar

        barMap.registerSnapshot(
            BarSnapshot(
                "files_to_assemble",
                pane,
                BarPosition.LEFT_BOTTOM,
                BarSnapshotViewModePane.INSTANCE,
                true,
                Icons.SIDEBAR_EXPLORER,
                Messages.BAR_FILES_TO_ASSEMBLE_NAME
            )
        )
    }

    private fun loadSpritesToAssemble() {
        val pane = PixelScrollPane().fit()
        spritesToAssembleSidebar = FilesToAssembleSidebar(project, project.data.spritesToAssemble, pane)
        pane.content = spritesToAssembleSidebar

        barMap.registerSnapshot(
            BarSnapshot(
                "sprites_to_assemble",
                pane,
                BarPosition.LEFT_BOTTOM,
                BarSnapshotViewModePane.INSTANCE,
                true,
                Icons.SIDEBAR_EXPLORER,
                NES4JAMS_BAR_SPRITES_TO_ASSEMBLE
            )
        )
    }

    private fun loadLog() {
        log = SimpleLog()
        barMap.registerSnapshot(
            BarSnapshot(
                "log",
                log as SimpleLog,
                BarPosition.BOTTOM_LEFT,
                BarSnapshotViewModePane.INSTANCE,
                true,
                Icons.FILE_FILE,
                Messages.BAR_LOG_NAME,
            )
        )
    }
}