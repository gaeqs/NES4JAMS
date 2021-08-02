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
import io.github.gaeqs.nes4jams.ppu.NESPPU
import io.github.gaeqs.nes4jams.ppu.PPUColors
import io.github.gaeqs.nes4jams.simulation.NESControllerMap
import javafx.animation.AnimationTimer
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import java.nio.IntBuffer

class NESSimulationDisplay(val simulationPane: NESSimulationPane) : Display() {

    companion object {
        const val ASPECT_RATIO = 256.0 / 240.0
    }

    private val buffer = IntBuffer.allocate(NESPPU.SCREEN_WIDTH * NESPPU.SCREEN_HEIGHT)
    private val screen = buffer.array()
    private val pixelBuffer = PixelBuffer(
        NESPPU.SCREEN_WIDTH, NESPPU.SCREEN_HEIGHT,
        buffer, PixelFormat.getIntArgbPreInstance()
    )
    private val image = WritableImage(pixelBuffer).apply { setImage(this) }
    private val handler = RedrawHandler().apply { start() }

    private var controller = NESControllerMap()

    init {
        setOnMouseClicked { requestFocus(); it.consume() }
        setOnKeyPressed { update(it.code, true); it.consume() }
        setOnKeyReleased { update(it.code, false); it.consume() }
    }

    override fun fitToSize(width: Double, height: Double) {
        val scaledWidth = width / ASPECT_RATIO

        if (scaledWidth > height) {
            fitWidth = height * ASPECT_RATIO
            fitHeight = height
        } else {
            this.fitWidth = width
            this.fitHeight = width / ASPECT_RATIO
        }
    }

    override fun stop() {
        handler.stop()
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
        simulationPane.simulation.runSynchronized {
            simulationPane.simulation.sendNextControllerData(controller, false)
        }
    }

    private inner class RedrawHandler : AnimationTimer() {

        var drawnFrame = -1L

        override fun handle(now: Long) {
            if (simulationPane.parentTab?.isSelected == false || !simulationPane.simulation.isRunning) return
            val simulation = simulationPane.simulation
            if (drawnFrame == simulation.frame) return
            drawnFrame = simulation.frame
            simulation.runSynchronized {
                // Let's copy it to let the simulation free
                val b = simulation.ppu.screen
                repeat(b.size) { screen[it] = PPUColors.INT_COLORS[b[it].toInt()] }
            }

            pixelBuffer.updateBuffer { null }



            //val nanos = simulation.lastFrameDelayInNanos.toDouble()
            //val default = 1000000000.0 / simulation.cartridge.header.tvType.framerate
            //val percentage = (nanos / default) * 100
            //println("OVERHEAD: ${String.format("%.2f", percentage)}%")
        }

    }
}