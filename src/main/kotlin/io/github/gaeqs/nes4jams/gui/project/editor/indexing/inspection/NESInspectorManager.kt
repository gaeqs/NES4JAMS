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

package io.github.gaeqs.nes4jams.gui.project.editor.indexing.inspection

import io.github.gaeqs.nes4jams.NES4JAMS
import net.jamsimulator.jams.gui.editor.code.indexing.inspection.Inspector
import net.jamsimulator.jams.gui.editor.code.indexing.inspection.defaults.*
import net.jamsimulator.jams.manager.Manager
import net.jamsimulator.jams.manager.ResourceProvider

class NESInspectorManager(
    provider: ResourceProvider,
    loadOnFXThread: Boolean
) : Manager<Inspector<*>>(provider, NAME, Inspector::class.java, loadOnFXThread) {

    companion object {
        val NAME = "nes_inspector"
        val INSTANCE = NESInspectorManager(NES4JAMS.INSTANCE, true)
    }

    override fun loadDefaultElements() {
        add(BadMacroFormatInspector(ResourceProvider.JAMS))
        add(BadMacroCallFormatInspector(ResourceProvider.JAMS))
        add(DuplicatedLabelInspector(ResourceProvider.JAMS))
        add(DuplicatedMacroInspector(ResourceProvider.JAMS))
        add(MacroNotFoundInspector(ResourceProvider.JAMS))
        add(IllegalMacroParameterInspector(ResourceProvider.JAMS))
        add(LabelAddressNotFoundInspector(ResourceProvider.JAMS))
    }

}