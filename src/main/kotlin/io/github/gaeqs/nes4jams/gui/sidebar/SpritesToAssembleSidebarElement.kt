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

import io.github.gaeqs.nes4jams.data.ACTION_REGION_SPRITES_TO_ASSEMBLE
import io.github.gaeqs.nes4jams.util.managerOf
import javafx.scene.control.Label
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.DragEvent
import javafx.scene.layout.HBox
import net.jamsimulator.jams.file.FileType
import net.jamsimulator.jams.gui.ActionRegion
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.action.Action
import net.jamsimulator.jams.gui.action.context.ContextAction
import net.jamsimulator.jams.gui.action.context.ContextActionMenuBuilder
import net.jamsimulator.jams.gui.explorer.ExplorerBasicElement
import net.jamsimulator.jams.gui.image.quality.QualityImageView
import net.jamsimulator.jams.gui.util.DraggableListCell
import java.io.File

class SpritesToAssembleSidebarElement(val display: SpritesToAssembleSidebar) : DraggableListCell<File>(), ActionRegion {

    init {
        setOnContextMenuRequested { manageContextMenuRequest(it) }
    }

    private fun getSupportedContextActions(): Set<ContextAction> {
        val actions = managerOf<Action>()
        if (isEmpty || item == null) return emptySet()
        return actions.filterIsInstance<ContextAction>().filter { supportsActionRegion(it.regionTag) }.toSet()
    }

    private fun manageContextMenuRequest(request: ContextMenuEvent) {
        val set: Set<ContextAction> = getSupportedContextActions()
        if (set.isEmpty()) {
            request.consume()
            return
        }
        val main = ContextActionMenuBuilder(this).addAll(set).build()
        JamsApplication.openContextMenu(main, this, request.screenX, request.screenY)
        request.consume()
    }


    override fun supportsActionRegion(p0: String?): Boolean {
        return ACTION_REGION_SPRITES_TO_ASSEMBLE == p0
    }

    override fun updateItem(item: File?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) graphic = null else {
            val hbox = HBox()
            hbox.spacing = ExplorerBasicElement.SPACING.toDouble()
            hbox.children.add(
                QualityImageView(
                    display.icon,
                    FileType.IMAGE_SIZE.toFloat(), FileType.IMAGE_SIZE.toFloat()
                )
            )
            hbox.children.add(Label(display.project.folder.toPath().relativize(item.toPath()).toString()))
            graphic = hbox
        }
    }


    override fun onDragDropped(event: DragEvent) {
        if (item == null) return
        val dragboard = event.dragboard
        if (dragboard.hasString()) {
            val items = listView.items
            val index = dragboard.string.toInt()
            if (index < 0 || index >= items.size) return
            val to = getIndex()
            items.add(to, items.removeAt(index))
            display.sprites.moveFile(item, to)
        }
        event.consume()
    }
}

