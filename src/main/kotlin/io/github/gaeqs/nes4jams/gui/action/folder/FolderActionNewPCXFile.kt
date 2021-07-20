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

package io.github.gaeqs.nes4jams.gui.action.folder

import io.github.gaeqs.nes4jams.file.pcx.PCXFileType
import net.jamsimulator.jams.gui.action.context.ContextAction
import net.jamsimulator.jams.gui.action.defaults.explorerelement.folder.FolderActionRegions
import net.jamsimulator.jams.gui.editor.CodeFileEditor
import net.jamsimulator.jams.gui.explorer.Explorer
import net.jamsimulator.jams.gui.explorer.ExplorerElement
import net.jamsimulator.jams.gui.explorer.folder.ExplorerFile
import net.jamsimulator.jams.gui.explorer.folder.ExplorerFolder
import net.jamsimulator.jams.gui.explorer.folder.FolderExplorer
import net.jamsimulator.jams.gui.main.MainMenuBar

class FolderActionNewPCXFile : ContextAction(
    NAME,
    REGION_TAG,
    LANGUAGE_NODE,
    null,
    FolderActionRegions.NEW_GENERAL,
    null,
    PCXFileType.INSTANCE.icon
) {

    companion object {
        const val NAME = "FOLDER_EXPLORER_ELEMENT_NEW_PCX_FILE"
        const val REGION_TAG = "FOLDER_EXPLORER_ELEMENT"
        const val LANGUAGE_NODE = "ACTION_FOLDER_EXPLORER_ELEMENT_NEW_PCX_FILE"
    }

    override fun run(node: Any?) {
        if (node !is ExplorerElement) return
        val explorer = node.explorer
        if (explorer !is FolderExplorer || explorer.selectedElements.size != 1) return
        val folder = when (val element = explorer.selectedElements[0]) {
            is ExplorerFile -> element.file.parentFile
            is ExplorerFolder -> element.folder
            else -> throw IllegalStateException("Element is not a file or a folder!")
        }

        NewPCXFileWindow.open(folder)
    }

    override fun runFromMenu() {
    }

    override fun supportsExplorerState(explorer: Explorer?) =
        explorer is FolderExplorer && explorer.selectedElements.size == 1

    override fun supportsTextEditorState(p0: CodeFileEditor?) = false
    override fun supportsMainMenuState(p0: MainMenuBar?) = false

}