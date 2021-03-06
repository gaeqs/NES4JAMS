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
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerData
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerDevice
import io.github.gaeqs.nes4jams.simulation.controller.NESKeyboardController
import io.github.gaeqs.nes4jams.simulation.event.NESSimulationRenderEvent
import io.github.gaeqs.nes4jams.util.extension.getAndConvert
import javafx.scene.input.KeyCode
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.configuration.event.ConfigurationNodeChangeEvent
import net.jamsimulator.jams.event.Listener

/**
 * The main display for the NES simulation. Shows the output of the PPU to the user.
 * This node also manages the player's input.
 */
class NESSimulationDisplay(val pane: NESSimulationPane) : BasicDisplay(WIDTH, HEIGHT) {

    companion object {
        const val WIDTH = 256
        const val HEIGHT = 240

        const val PLAYER_1_CONFIGURATION_NODE = "simulation.nes.player_1_controller"
        const val PLAYER_2_CONFIGURATION_NODE = "simulation.nes.player_2_controller"
    }

    var drawEnabled: Boolean
        get() = pane.parentTab?.isSelected != false && pane.simulation.isRunning
        set(_) {}

    @Volatile
    private var player1Device: NESControllerDevice

    @Volatile
    private var player2Device: NESControllerDevice

    init {
        val config = Jams.getMainConfiguration().data
        player1Device = config.getAndConvert<NESControllerData>(PLAYER_1_CONFIGURATION_NODE)?.build()
            ?: NESKeyboardController(emptyMap())

        player2Device = config.getAndConvert<NESControllerData>(PLAYER_2_CONFIGURATION_NODE)?.build()
            ?: NESKeyboardController(emptyMap())
        config.registerListeners(this, true)
    }

    init {
        setOnKeyPressed { updateKeyboard(it.code, true); it.consume() }
        setOnKeyReleased { updateKeyboard(it.code, false); it.consume() }
        pane.simulation.registerListeners(this, true)
    }

    override fun dispose() {
        super.dispose()
        player1Device.dispose()
        player2Device.dispose()
    }

    private fun updateKeyboard(key: KeyCode, pressed: Boolean) {
        player1Device.updateKeyboardKey(key, pressed)
        player2Device.updateKeyboardKey(key, pressed)
    }

    @Listener
    private fun onSimulationRender(event: NESSimulationRenderEvent) {
        pane.simulation.sendNextControllerData(player1Device.currentState, false)
        pane.simulation.sendNextControllerData(player2Device.currentState, true)

        if (!drawEnabled) return

        startDataTransmission { buffer ->
            repeat(buffer.size) {
                buffer[it] = PPUColors.INT_COLORS[event.buffer[it].toInt()]
            }
        }
    }

    @Listener
    private fun onNodeChange(event: ConfigurationNodeChangeEvent.After) {
        when (event.node) {
            PLAYER_1_CONFIGURATION_NODE -> {
                player1Device.dispose()
                player1Device = event.configuration.root
                    .getAndConvert<NESControllerData>(PLAYER_1_CONFIGURATION_NODE)?.build()
                    ?: NESKeyboardController(emptyMap())
            }
            PLAYER_2_CONFIGURATION_NODE -> {
                player2Device.dispose()
                player2Device = event.configuration.root
                    .getAndConvert<NESControllerData>(PLAYER_2_CONFIGURATION_NODE)?.build()
                    ?: NESKeyboardController(emptyMap())
            }
        }
    }
}