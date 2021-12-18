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

import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.event.SimpleEventBroadcast
import net.jamsimulator.jams.event.file.FileEvent
import net.jamsimulator.jams.gui.editor.code.indexing.global.FileCollection
import net.jamsimulator.jams.gui.editor.code.indexing.global.event.FileCollectionAddFileEvent
import net.jamsimulator.jams.gui.editor.code.indexing.global.event.FileCollectionChangeIndexEvent
import net.jamsimulator.jams.gui.editor.code.indexing.global.event.FileCollectionRemoveFileEvent
import org.json.JSONArray
import java.io.File
import java.nio.file.StandardWatchEventKinds

class NESSpritesToAssemble(val project: NESProject) : SimpleEventBroadcast(), FileCollection {

    companion object {
        const val FILE_NAME = "sprites_to_assemble.json"
    }

    private val files = mutableListOf<File>()

    init {
        project.projectTab.ifPresent { it.registerListeners(this, true) }
    }

    override fun getFiles(): List<File> = files.toList()
    override fun containsFile(file: File?): Boolean = file in files

    operator fun contains(file: File?) = containsFile(file)

    override fun addFile(file: File): Boolean {
        if (file in files) return false

        val before = callEvent(FileCollectionAddFileEvent.Before(this, file))
        if (before.isCancelled) return false
        files += file
        callEvent(FileCollectionAddFileEvent.After(this, file))
        return true
    }

    override fun removeFile(file: File): Boolean {
        if (file !in files) return false
        val before = callEvent(FileCollectionRemoveFileEvent.Before(this, file))
        if (before.isCancelled) return false
        files -= file
        callEvent(FileCollectionRemoveFileEvent.After(this, file))
        return true
    }

    override fun moveFile(file: File, index: Int): Boolean {
        if (!files.contains(file) || index !in 0 until files.size) return false
        val old = files.indexOf(file)
        val before = callEvent(FileCollectionChangeIndexEvent.Before(this, file, old, index))
        if (before.isCancelled) return false
        val newIndex = before.newIndex

        if (index !in 0 until files.size) return false
        files -= file
        files.add(newIndex, file)
        callEvent(FileCollectionChangeIndexEvent.After(this, file, old, newIndex))
        return true
    }

    fun load(metadataFolder: File) {
        val file = File(metadataFolder, FILE_NAME)
        if (!file.isFile) return

        JSONArray(file.readText())
            .map { File(project.folder, it.toString()) }
            .filter { it.isFile }
            .forEach { addFile(it) }
    }

    fun save(metadataFolder: File) {
        val json = JSONArray()
        val path = project.folder.toPath()
        files.map { path.relativize(it.toPath()) }.forEach { json.put(it) }
        File(metadataFolder, FILE_NAME).writeText(json.toString(1))
    }

    @Listener
    private fun onFileRemove(event: FileEvent) {
        if (event.watchEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            val file = event.path.toFile()
            if (!removeFile(file)) {
                System.err.println("Couldn't delete file $file from global index.")
            }
        }
    }

}