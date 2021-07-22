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

package io.github.gaeqs.nes4jams.project

import net.jamsimulator.jams.collection.Bag
import net.jamsimulator.jams.event.SimpleEventBroadcast
import net.jamsimulator.jams.gui.editor.FileEditorHolder
import net.jamsimulator.jams.project.FilesToAssemble
import net.jamsimulator.jams.project.Project
import net.jamsimulator.jams.project.mips.event.FileAddToAssembleEvent
import net.jamsimulator.jams.project.mips.event.FileIndexChangedFromAssembleEvent
import net.jamsimulator.jams.project.mips.event.FileRemoveFromAssembleEvent
import org.json.JSONArray
import java.io.File

class NESSpritesToAssemble(val project: NESProject) : SimpleEventBroadcast(), FilesToAssemble {

    private val files = mutableListOf<File>()

    companion object {
        const val FILE_NAME = "sprites_to_assemble.json"
    }

    override fun getProject(): Project = project
    override fun supportsGlobalLabels(): Boolean = false
    override fun getGlobalLabels(): Bag<String>? = null
    override fun getFiles(): List<File> = files.toList()
    override fun containsFile(file: File?): Boolean = file in files

    operator fun contains(file: File?) = containsFile(file)

    override fun addFile(file: File, refreshGlobalLabels: Boolean) {
        if (file in files) return
        val before = callEvent(FileAddToAssembleEvent.Before(file))
        if (before.isCancelled) return
        files += file
        callEvent(FileAddToAssembleEvent.After(file))
    }

    override fun addFile(file: File, holder: FileEditorHolder, refreshGlobalLabels: Boolean) {
        addFile(file, refreshGlobalLabels)
    }

    override fun removeFile(file: File) {
        if (file !in files) return
        val before = callEvent(FileRemoveFromAssembleEvent.Before(file))
        if (before.isCancelled) return
        files -= file
        refreshGlobalLabels()
        callEvent(FileRemoveFromAssembleEvent.After(file))
    }

    override fun moveFileToIndex(file: File, index: Int): Boolean {
        if (!files.contains(file) || index !in 0 until files.size) return false
        val old = files.indexOf(file)
        val before = callEvent(FileIndexChangedFromAssembleEvent.Before(file, old, index))
        if (before.isCancelled) return false
        val newIndex = before.newIndex

        if (index !in 0 until files.size) return false
        files -= file
        files.add(newIndex, file)
        callEvent(FileIndexChangedFromAssembleEvent.After(file, old, newIndex))
        return true
    }

    fun load(metadataFolder: File) {
        val file = File(metadataFolder, FILE_NAME)
        if (!file.isFile) return

        JSONArray(file.readText())
            .map { File(project.folder, it.toString()) }
            .filter { it.isFile }
            .forEach { addFile(it, false) }
    }

    fun save(metadataFolder: File) {
        val json = JSONArray()
        val path = project.folder.toPath()
        files.map { path.relativize(it.toPath()) }.forEach { json.put(it) }
        File(metadataFolder, FILE_NAME).writeText(json.toString(1))
    }

    override fun checkFiles() {
        files.filter { !it.isFile }.forEach { removeFile(it) }
    }

    override fun refreshGlobalLabels() {
    }

}