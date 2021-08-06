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

package io.github.gaeqs.nes4jams.gui.simulation.display

import io.github.gaeqs.nes4jams.gui.project.NESSimulationPane
import io.github.gaeqs.nes4jams.ppu.PPUColors
import io.github.gaeqs.nes4jams.simulation.NESControllerMap
import io.github.gaeqs.nes4jams.simulation.event.NESSimulationRenderEvent
import javafx.scene.input.KeyCode
import net.jamsimulator.jams.event.Listener

/**
 * The main display for the NES simulation. Shows the output of the PPU to the user.
 * This node also manages the player's input.
 */
class NESSimulationDisplay(val pane: NESSimulationPane) : BasicDisplay(WIDTH, HEIGHT) {

    companion object {
        const val WIDTH = 256
        const val HEIGHT = 240
    }

    var drawEnabled: Boolean
        get() = pane.parentTab?.isSelected != false && pane.simulation.isRunning
        set(_) {}

    private var controller = NESControllerMap()

    init {
        setOnKeyPressed { update(it.code, true); it.consume() }
        setOnKeyReleased { update(it.code, false); it.consume() }
        pane.simulation.registerListeners(this, true)
    }

    private fun update(key: KeyCode, pressed: Boolean) {
        when (key) {
            KeyCode.X -> controller = controller.copy(a = pressed)
            KeyCode.Z -> controller = controller.copy(b = pressed)
            KeyCode.A -> controller = controller.copy(start = pressed)
            KeyCode.S -> controller = controller.copy(select = pressed)
            KeyCode.UP -> controller = controller.copy(up = pressed)
            KeyCode.DOWN -> controller = controller.copy(down = pressed)
            KeyCode.LEFT -> controller = controller.copy(left = pressed)
            KeyCode.RIGHT -> controller = controller.copy(right = pressed)
            else -> {
            }
        }
        pane.simulation.runSynchronized {
            pane.simulation.sendNextControllerData(controller, false)
        }
    }

    @Listener
    private fun onSimulationRender(event: NESSimulationRenderEvent) {
        if(!drawEnabled) return
        startDataTransmission { buffer ->
            repeat(buffer.size) {
                buffer[it] = PPUColors.INT_COLORS[event.buffer[it].toInt()]
            }
        }
    }
}