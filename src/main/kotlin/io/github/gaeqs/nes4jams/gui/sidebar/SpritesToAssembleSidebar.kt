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

package io.github.gaeqs.nes4jams.gui.sidebar

import io.github.gaeqs.nes4jams.project.NESSpritesToAssemble
import javafx.scene.control.ListView
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.editor.code.indexing.global.event.FileCollectionAddFileEvent
import net.jamsimulator.jams.gui.editor.code.indexing.global.event.FileCollectionRemoveFileEvent
import net.jamsimulator.jams.gui.image.icon.IconData
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.project.Project
import java.io.File

class SpritesToAssembleSidebar(val project: Project, val sprites: NESSpritesToAssemble) : ListView<File>() {

    val icon: IconData = Icons.FILE_IMAGE

    init {
        setCellFactory { SpritesToAssembleSidebarElement(this) }
        sprites.registerListeners(this, true)
        items.addAll(sprites.files)
    }

    @Listener
    private fun onFileAdd(event: FileCollectionAddFileEvent.After) {
        items.add(event.file)
    }

    @Listener
    private fun onFileRemoved(event: FileCollectionRemoveFileEvent.After) {
        items.remove(event.file)
    }
}
