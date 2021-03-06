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

package io.github.gaeqs.nes4jams.gui.util.converter

import io.github.gaeqs.nes4jams.cartridge.ConsoleType
import io.github.gaeqs.nes4jams.cartridge.TVType
import io.github.gaeqs.nes4jams.memory.NESMemoryBankCollection
import io.github.gaeqs.nes4jams.ppu.Mirror
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerData
import net.jamsimulator.jams.gui.util.converter.ValueConverters

class NESValueConverters private constructor() {

    companion object {

        fun setupConverters() {
            // NES MEMORY BANK
            ValueConverters.addByType(NESMemoryBankValueConverter::class.java, NESMemoryBankValueConverter.INSTANCE)
            ValueConverters.addByName(NESMemoryBankValueConverter.NAME, NESMemoryBankValueConverter.INSTANCE)

            // NES MEMORY BANK COLLECTION
            ValueConverters.addByType(
                NESMemoryBankCollection::class.java,
                NESMemoryBankCollectionValueConverter.INSTANCE
            )
            ValueConverters.addByName(
                NESMemoryBankCollectionValueConverter.NAME,
                NESMemoryBankCollectionValueConverter.INSTANCE
            )

            // TV TYPE
            ValueConverters.addByType(TVType::class.java, TVTypeValueConverter.INSTANCE)
            ValueConverters.addByName(TVTypeValueConverter.NAME, TVTypeValueConverter.INSTANCE)

            // CONSOLE TYPE
            ValueConverters.addByType(ConsoleType::class.java, ConsoleTypeValueConverter.INSTANCE)
            ValueConverters.addByName(ConsoleTypeValueConverter.NAME, ConsoleTypeValueConverter.INSTANCE)

            // MIRROR
            ValueConverters.addByType(Mirror::class.java, MirrorValueConverter.INSTANCE)
            ValueConverters.addByName(MirrorValueConverter.NAME, MirrorValueConverter.INSTANCE)

            // CONTROLLER DATA
            ValueConverters.addByType(NESControllerData::class.java, NESControllerDataValueConverter.INSTANCE)
            ValueConverters.addByName(NESControllerDataValueConverter.NAME, NESControllerDataValueConverter.INSTANCE)
        }
    }

}