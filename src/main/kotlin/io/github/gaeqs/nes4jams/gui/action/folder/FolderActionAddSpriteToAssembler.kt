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

import io.github.gaeqs.nes4jams.NES4JAMS
import io.github.gaeqs.nes4jams.file.pcx.PCXFileType
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.util.extension.orNull
import io.github.gaeqs.nes4jams.util.manager
import net.jamsimulator.jams.file.FileTypeManager
import net.jamsimulator.jams.gui.action.context.ContextAction
import net.jamsimulator.jams.gui.action.defaults.explorerelement.folder.FolderActionRegions
import net.jamsimulator.jams.gui.editor.code.CodeFileEditor
import net.jamsimulator.jams.gui.explorer.Explorer
import net.jamsimulator.jams.gui.explorer.ExplorerElement
import net.jamsimulator.jams.gui.explorer.folder.ExplorerFile
import net.jamsimulator.jams.gui.main.MainMenuBar
import net.jamsimulator.jams.gui.project.ProjectFolderExplorer

class FolderActionAddSpriteToAssembler : ContextAction(
    NES4JAMS.INSTANCE,
    NAME,
    REGION_TAG,
    LANGUAGE_NODE,
    null,
    FolderActionRegions.ASSEMBLER,
    null,
    null
) {

    companion object {
        const val NAME = "FOLDER_EXPLORER_ELEMENT_ADD_SPRITE_TO_ASSEMBLER"
        const val REGION_TAG = "FOLDER_EXPLORER_ELEMENT"
        const val LANGUAGE_NODE = "ACTION_FOLDER_EXPLORER_ADD_SPRITE_TO_ASSEMBLER"
    }

    override fun run(node: Any?) {
        if (node !is ExplorerElement) return
        val explorer = node.explorer
        if (explorer !is ProjectFolderExplorer) return
        val project = explorer.project
        if (project !is NESProject) return
        val elements = explorer.selectedElements

        elements.forEach {
            it as ExplorerFile
            project.data.spritesToAssemble.addFile(it.file)
        }
    }

    override fun runFromMenu() {
    }

    override fun supportsExplorerState(explorer: Explorer?): Boolean {
        if (explorer !is ProjectFolderExplorer) return false
        val project = explorer.project
        if (project !is NESProject) return false
        val elements = explorer.selectedElements.takeIf { it.isNotEmpty() } ?: return false
        val sprites = project.data.spritesToAssemble

        var allPresent = true
        elements.forEach {
            if (it !is ExplorerFile) return false
            if (manager<FileTypeManager>().getByFile(it.file).orNull() != PCXFileType.INSTANCE) return false
            allPresent = allPresent && it.file in sprites
        }

        return !allPresent
    }

    override fun supportsTextEditorState(p0: CodeFileEditor?) = false
    override fun supportsMainMenuState(p0: MainMenuBar?) = false


}