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

import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.utils.extension.fit
import javafx.scene.control.Tab
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.JamsApplication
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
import java.io.File
import java.util.function.Consumer

class NESStructurePane(parent: Tab, projectTab: ProjectTab, val project: NESProject) :
    WorkingPane(parent, projectTab, null, false), FileEditorHolderHolder {

    lateinit var explorer: ProjectFolderExplorer
        private set

    lateinit var filesToAssembleSidebar: FilesToAssembleSidebar
        private set

    lateinit var log: Log
        private set

    override fun getFileEditorHolder(): FileEditorHolder = center as FileEditorHolder

    init {
        center = FileEditorHolder(this)
        init()
        loadExplorer()
        loadFilesToAssemble()
        loadLog()
    }

    override fun getLanguageNode(): String = Messages.PROJECT_TAB_STRUCTURE

    override fun populateHBox(hbox: HBox?) {

    }


    override fun onClose() {
        super.onClose()
        explorer.dispose()
    }

    fun openFile(file: File) {
        val holder = center as FileEditorHolder;
        holder.openFile(file)
    }

    private fun loadExplorer() {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.SIDEBAR_EXPLORER).orElse(null)
        val pane = PixelScrollPane().fit()
        explorer = ProjectFolderExplorer(project, project.data, pane)
        pane.content = explorer

        explorer.fileOpenAction = Consumer { openFile(it.file) }

        barMap.registerSnapshot(
            BarSnapshot(
                "explorer",
                pane,
                BarPosition.LEFT_TOP,
                BarSnapshotViewModePane.INSTANCE,
                true,
                icon,
                Messages.BAR_EXPLORER_NAME
            )
        )
    }

    private fun loadFilesToAssemble() {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.SIDEBAR_EXPLORER).orElse(null)
        val pane = PixelScrollPane().fit()
        filesToAssembleSidebar = FilesToAssembleSidebar(project, project.data, pane)
        pane.content = filesToAssembleSidebar

        barMap.registerSnapshot(
            BarSnapshot(
                "files_to_assemble",
                pane,
                BarPosition.LEFT_BOTTOM,
                BarSnapshotViewModePane.INSTANCE,
                true,
                icon,
                Messages.BAR_FILES_TO_ASSEMBLE_NAME
            )
        )
    }

    private fun loadLog() {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.FILE_FILE).orElse(null)
        log = SimpleLog()
        barMap.registerSnapshot(
            BarSnapshot(
                "log",
                log as SimpleLog,
                BarPosition.BOTTOM_LEFT,
                BarSnapshotViewModePane.INSTANCE,
                true,
                icon,
                Messages.BAR_LOG_NAME,
            )
        )
    }
}