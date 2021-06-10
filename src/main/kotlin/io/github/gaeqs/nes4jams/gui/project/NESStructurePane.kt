package io.github.gaeqs.nes4jams.gui.project

import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.utils.fit
import javafx.scene.control.Tab
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.bar.BarPosition
import net.jamsimulator.jams.gui.bar.BarSnapshot
import net.jamsimulator.jams.gui.bar.mode.BarSnapshotViewModePane
import net.jamsimulator.jams.gui.editor.FileEditorHolder
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.gui.util.PixelScrollPane
import net.jamsimulator.jams.language.Messages
import java.io.File
import java.util.function.Consumer

class NESStructurePane(parent: Tab, projectTab: ProjectTab, val project: NESProject) :
    WorkingPane(parent, projectTab, null, false) {

    lateinit var explorer: NESFolderExplorer
        private set

    init {
        center = FileEditorHolder(this)
        init()
        loadExplorer()
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
        explorer = NESFolderExplorer(project, pane)
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
}