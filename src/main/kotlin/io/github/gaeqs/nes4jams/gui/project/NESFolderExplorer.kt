package io.github.gaeqs.nes4jams.gui.project

import io.github.gaeqs.nes4jams.project.NESProject
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Region
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.explorer.event.ExplorerAddElementEvent
import net.jamsimulator.jams.gui.explorer.folder.ExplorerFile
import net.jamsimulator.jams.gui.explorer.folder.ExplorerFolder
import net.jamsimulator.jams.gui.explorer.folder.FolderExplorer
import net.jamsimulator.jams.gui.mips.explorer.MipsFolderExplorer
import java.io.File
import java.nio.file.Files

class NESFolderExplorer(val project: NESProject, scrollPane: ScrollPane) : FolderExplorer(
    project.folder,
    scrollPane,
    { file -> !file.toPath().startsWith(project.data.metadataFolder.toPath()) }
) {

    init {
        Files.walk(project.data.filesFolder.toPath()).forEach { markOutFile(it.toFile()) }
        registerListeners(this, true)
    }

    fun dispose() {
        killWatchers()
    }

    private fun markOutFile(file: File) {
        val region: Region
        val optional = getExplorerFile(file)
        region = if (optional.isEmpty) {
            val folderOptional = getExplorerFolder(file)
            if (folderOptional.isEmpty) return
            folderOptional.get().representation
        } else {
            optional.get()
        }

        if (!region.styleClass.contains(MipsFolderExplorer.EXPLORER_OUT_FILE_STYLE_CLASS)) {
            region.styleClass.add(MipsFolderExplorer.EXPLORER_OUT_FILE_STYLE_CLASS)
        }
    }

    @Listener
    private fun onFileAdded(event: ExplorerAddElementEvent.After) {
        val element = event.element;
        val file = when (element) {
            is ExplorerFile -> element.file
            is ExplorerFolder -> element.folder
            else -> return
        }

        if (file.toPath().startsWith(project.data.filesFolder.toPath())) {
            element as Region
            element.styleClass.add(MipsFolderExplorer.EXPLORER_OUT_FILE_STYLE_CLASS)
        }
    }

}