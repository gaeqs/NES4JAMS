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

package io.github.gaeqs.nes4jams.simulation

import io.github.gaeqs.nes4jams.cartridge.Cartridge
import io.github.gaeqs.nes4jams.cpu.label.NESLabel
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfiguration
import net.jamsimulator.jams.gui.util.log.Console
import net.jamsimulator.jams.project.mips.configuration.MIPSSimulationConfigurationPresets

class NESSimulationData(
    val cartridge: Cartridge,
    val console: Console?,
    val originalInstructions: Map<Int, String>,
    val labels: Set<NESLabel>,
    val callEvents: Boolean,
    undoEnabled: Boolean
) {

    val undoEnabled = callEvents && undoEnabled

    constructor(
        cartridge: Cartridge,
        console: Console?,
        originalInstructions: Map<Int, String>,
        labels: Set<NESLabel>,
        configuration: NESSimulationConfiguration
    ) : this(
        cartridge,
        console,
        originalInstructions,
        labels,
        configuration.getNodeValue<Boolean>(MIPSSimulationConfigurationPresets.CALL_EVENTS) ?: false,
        configuration.getNodeValue<Boolean>(MIPSSimulationConfigurationPresets.UNDO_ENABLED) ?: false
    )

}