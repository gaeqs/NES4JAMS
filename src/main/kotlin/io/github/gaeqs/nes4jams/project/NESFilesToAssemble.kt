package io.github.gaeqs.nes4jams.project

import io.github.gaeqs.nes4jams.gui.project.editor.NESFileEditor
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESFileElements
import javafx.application.Platform
import net.jamsimulator.jams.collection.Bag
import net.jamsimulator.jams.event.SimpleEventBroadcast
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.editor.FileEditorHolder
import net.jamsimulator.jams.project.mips.event.FileAddToAssembleEvent
import net.jamsimulator.jams.project.mips.event.FileRemoveFromAssembleEvent
import org.json.JSONArray
import java.io.File

class NESFilesToAssemble(val project: NESProject) : SimpleEventBroadcast() {

    val files = mutableMapOf<File, NESFileElements>()
    val globalLabels = Bag<String>()

    companion object {
        const val FILE_NAME = "files_to_assemble.json"
    }

    fun addFile(file: File, refreshGlobalLabels: Boolean) {
        if (file in files) return

        val before = callEvent(FileAddToAssembleEvent.Before(file))
        if (before.isCancelled) return

        val elements = NESFileElements(project, this)
        files[file] = elements
        elements.refreshAll(file.readText())

        if (refreshGlobalLabels) {
            refreshGlobalLabels()
        }

        callEvent(FileAddToAssembleEvent.After(file))
    }

    fun addFile(file: File, elements: NESFileElements, refreshGlobalLabels: Boolean) {
        if (file in files) return

        val before = callEvent(FileAddToAssembleEvent.Before(file))
        if (before.isCancelled) return

        files[file] = elements
        elements.filesToAssemble = this

        if (refreshGlobalLabels) {
            refreshGlobalLabels()
        }

        callEvent(FileAddToAssembleEvent.After(file))
    }

    fun addFile(file: File, holder: FileEditorHolder, refreshGlobalLabels: Boolean) {
        val tab = holder.getFileDisplayTab(file, true)
        if (tab.isEmpty || tab.get().display !is NESFileEditor) {
            addFile(file, refreshGlobalLabels)
        } else {
            val editor = tab.get().display as NESFileEditor
            addFile(file, editor.elements, refreshGlobalLabels)
        }
    }

    fun removeFile(file: File) {
        if (file !in files) return
        val before = callEvent(FileRemoveFromAssembleEvent.Before(file))
        if (before.isCancelled) return
        val elements = files.remove(file)!!
        elements.filesToAssemble = null
        refreshDeletedFiles(file, elements)
        refreshGlobalLabels()
        callEvent(FileRemoveFromAssembleEvent.After(file))
    }

    fun refreshGlobalLabels() {
        val requiresUpdate = HashSet(globalLabels)

        globalLabels.clear()
        files.values.forEach { globalLabels.addAll(it.getExistingGlobalLabels()) }
        requiresUpdate += globalLabels

        val tab = JamsApplication.getProjectsTabPane().getProjectTab(project).orElse(null) ?: return
        val node = tab.projectTabPane.workingPane.center
        if (node !is FileEditorHolder) return

        files.forEach { (file, elements) ->
            elements.searchForLabelsUpdates(requiresUpdate)
            val fileTab = node.getFileDisplayTab(file, true).orElse(null) ?: return@forEach
            val display = fileTab.display
            if (display is NESFileEditor) {
                elements.update(display)
            }
        }
    }

    fun load(metadataFolder: File) {
        val file = File(metadataFolder, FILE_NAME)
        if (!file.isFile) return

        JSONArray(file.readText())
            .map { File(project.folder, it.toString()) }
            .filter { it.isFile }
            .forEach { addFile(it, false) }

        Platform.runLater { refreshGlobalLabels() }

    }

    fun save(metadataFolder: File) {
        val json = JSONArray()
        val path = project.folder.toPath()
        files.keys.map { path.relativize(it.toPath()) }.forEach { json.put(it) }
        File(metadataFolder, FILE_NAME).writeText(json.toString(1))
    }

    fun checkFiles() {
        files.keys.filter { !it.isFile }.forEach { removeFile(it) }
    }

    private fun refreshDeletedFiles(file: File, elements: NESFileElements) {
        elements.searchForLabelsUpdates(globalLabels)

        val tab = JamsApplication.getProjectsTabPane().getProjectTab(project).orElse(null) ?: return
        val node = tab.projectTabPane.workingPane.center
        if (node !is FileEditorHolder) return
        val fileTab = node.getFileDisplayTab(file, true).orElse(null) ?: return
        val display = fileTab.display
        if (display is NESFileEditor) {
            elements.update(display)
        }
    }


}