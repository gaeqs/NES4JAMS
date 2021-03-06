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

package io.github.gaeqs.nes4jams.gui.util.value

import io.github.gaeqs.nes4jams.cartridge.ConsoleType
import io.github.gaeqs.nes4jams.cartridge.TVType
import io.github.gaeqs.nes4jams.gui.util.converter.NESMemoryBankValueConverter
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.memory.NESMemoryBankCollection
import io.github.gaeqs.nes4jams.ppu.Mirror
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerData
import net.jamsimulator.jams.gui.util.value.ValueEditors

class NESValueEditors private constructor() {

    companion object {

        fun setupEditor() {
            // NES MEMORY BANK
            val nesMemoryBankBuilder = NESMemoryBankValueEditor.Builder()
            ValueEditors.addByType(NESMemoryBank::class.java, nesMemoryBankBuilder)
            ValueEditors.addByName(NESMemoryBankValueConverter.NAME, nesMemoryBankBuilder)

            // NES MEMORY BANK COLLECTION
            val nesMemoryBankCollectionBuilder = NESMemoryBankCollectionValueEditor.Builder()
            ValueEditors.addByType(NESMemoryBankCollection::class.java, nesMemoryBankCollectionBuilder)
            ValueEditors.addByName(NESMemoryBankCollectionValueEditor.NAME, nesMemoryBankCollectionBuilder)

            // MIRROR
            val mirrorBuilder = MirrorValueEditor.Builder()
            ValueEditors.addByType(Mirror::class.java, mirrorBuilder)
            ValueEditors.addByName(MirrorValueEditor.NAME, mirrorBuilder)

            // TV TYPE
            val tvTypeBuilder = TVTypeValueEditor.Builder()
            ValueEditors.addByType(TVType::class.java, tvTypeBuilder)
            ValueEditors.addByName(TVTypeValueEditor.NAME, tvTypeBuilder)

            // CONSOLE TYPE
            val consoleTypeBuilder = ConsoleTypeValueEditor.Builder()
            ValueEditors.addByType(ConsoleType::class.java, consoleTypeBuilder)
            ValueEditors.addByName(ConsoleTypeValueEditor.NAME, consoleTypeBuilder)

            // CONTROLLER DATA
            val controllerDataBuilder = NESControllerDataValueEditor.Builder()
            ValueEditors.addByType(NESControllerData::class.java, controllerDataBuilder)
            ValueEditors.addByName(NESControllerDataValueEditor.NAME, controllerDataBuilder)
        }

    }

}