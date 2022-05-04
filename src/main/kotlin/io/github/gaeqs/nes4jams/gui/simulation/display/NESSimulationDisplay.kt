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

import com.github.strikerx3.jxinput.XInputDevice
import com.github.strikerx3.jxinput.enums.XInputButton
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener
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

    @Volatile
    private var keyboard = NESControllerMap()

    @Volatile
    private var xInputFirstPlayer = NESControllerMap()

    @Volatile
    private var xInputSecondPlayer = NESControllerMap()

    private val deviceFirstPlayer: XInputDevice?
    private val deviceSecondPlayer: XInputDevice?

    init {
        setOnKeyPressed { updateKeyboard(it.code, true); it.consume() }
        setOnKeyReleased { updateKeyboard(it.code, false); it.consume() }
        pane.simulation.registerListeners(this, true)


        deviceFirstPlayer = if (XInputDevice.isAvailable()) XInputDevice.getDeviceFor(0).apply {
            addListener(object : SimpleXInputDeviceListener() {
                override fun buttonChanged(button: XInputButton, pressed: Boolean) {
                    // This code runs on the simulation thread.
                    updateXInput(button, pressed, false)
                }
            })
        } else null
        deviceSecondPlayer = if (XInputDevice.isAvailable()) XInputDevice.getDeviceFor(1).apply {
            addListener(object : SimpleXInputDeviceListener() {
                override fun buttonChanged(button: XInputButton, pressed: Boolean) {
                    // This code runs on the simulation thread.
                    updateXInput(button, pressed, true)
                }
            })
        } else null

    }

    private fun updateKeyboard(key: KeyCode, pressed: Boolean) {
        when (key) {
            KeyCode.X -> keyboard = keyboard.copy(a = pressed)
            KeyCode.Z -> keyboard = keyboard.copy(b = pressed)
            KeyCode.A -> keyboard = keyboard.copy(start = pressed)
            KeyCode.S -> keyboard = keyboard.copy(select = pressed)
            KeyCode.UP -> keyboard = keyboard.copy(up = pressed)
            KeyCode.DOWN -> keyboard = keyboard.copy(down = pressed)
            KeyCode.LEFT -> keyboard = keyboard.copy(left = pressed)
            KeyCode.RIGHT -> keyboard = keyboard.copy(right = pressed)
            KeyCode.SHIFT -> {
                pane.simulation.runSynchronized {
                    pane.simulation.maxFPS = pressed
                }
                return
            }
            else -> {}
        }
        pane.simulation.runSynchronized {
            pane.simulation.sendNextControllerData(keyboard union xInputFirstPlayer, false)
        }
    }

    private fun updateXInput(button: XInputButton, pressed: Boolean, secondPlayer: Boolean) {
        val xInput = if (secondPlayer) xInputSecondPlayer else xInputFirstPlayer
        val result = when (button) {
            XInputButton.A -> xInput.copy(a = pressed)
            XInputButton.B -> xInput.copy(b = pressed)
            XInputButton.START -> xInput.copy(start = pressed)
            XInputButton.BACK -> xInput.copy(select = pressed)
            XInputButton.DPAD_UP -> xInput.copy(up = pressed)
            XInputButton.DPAD_DOWN -> xInput.copy(down = pressed)
            XInputButton.DPAD_LEFT -> xInput.copy(left = pressed)
            XInputButton.DPAD_RIGHT -> xInput.copy(right = pressed)
            else -> xInput
        }

        if (secondPlayer) xInputSecondPlayer = result
        else xInputFirstPlayer = result

        pane.simulation.runSynchronized {
            pane.simulation.sendNextControllerData(if (secondPlayer) result else keyboard union result, secondPlayer)
        }
    }


    @Listener
    private fun onSimulationRender(event: NESSimulationRenderEvent) {
        deviceFirstPlayer?.poll()
        deviceSecondPlayer?.poll()
        if (!drawEnabled) return

        startDataTransmission { buffer ->
            repeat(buffer.size) {
                buffer[it] = PPUColors.INT_COLORS[event.buffer[it].toInt()]
            }
        }
    }
}