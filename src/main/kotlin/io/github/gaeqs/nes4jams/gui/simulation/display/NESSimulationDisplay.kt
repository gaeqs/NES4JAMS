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

import io.github.gaeqs.nes4jams.ppu.NESPPU
import io.github.gaeqs.nes4jams.ppu.PPUColors
import io.github.gaeqs.nes4jams.simulation.NESControllerMap
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import javafx.animation.AnimationTimer
import javafx.scene.canvas.Canvas
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import net.jamsimulator.jams.gui.image.NearestImageView

class NESSimulationDisplay(val simulation: NESSimulation) :
    NearestImageView(null, 0.0, 0.0) {

    companion object {
        const val ASPECT_RATIO = 256.0 / 240.0
    }

    private val image = WritableImage(NESPPU.SCREEN_WIDTH, NESPPU.SCREEN_HEIGHT).apply { setImage(this) }
    private val handler = RedrawHandler().apply { start() }

    private var controller = NESControllerMap()

    init {
        //graphicsContext2D.isImageSmoothing = false

        setOnMouseClicked { requestFocus(); it.consume() }
        setOnKeyPressed { update(it.code, true); it.consume() }
        setOnKeyReleased { update(it.code, false); it.consume() }
    }

    fun stop() {
        handler.stop()
    }

    fun fitToSize(width: Double, height: Double) {
        val scaledWidth = width / ASPECT_RATIO

        if (scaledWidth > height) {
            fitWidth = height * ASPECT_RATIO
            fitHeight = height
        } else {
            this.fitWidth = width
            this.fitHeight = width / ASPECT_RATIO
        }
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
        simulation.runSynchronized {
            simulation.sendNextControllerData(controller, false)
        }
    }

    private inner class RedrawHandler : AnimationTimer() {

        val delay = 1000000000L / simulation.cartridge.header.tvType.framerate
        val screen = ByteArray(NESPPU.SCREEN_WIDTH * NESPPU.SCREEN_HEIGHT)
        var lastTick = System.nanoTime()

        override fun handle(now: Long) {
            if (now - lastTick < delay) return
            lastTick = now
            simulation.runSynchronized {
                // Let's copy it to let the simulation free
                simulation.ppu.screen.copyInto(screen)
            }

            image.pixelWriter.setPixels(
                0,
                0,
                NESPPU.SCREEN_WIDTH,
                NESPPU.SCREEN_HEIGHT,
                PixelFormat.createByteIndexedInstance(PPUColors.INT_COLORS),
                screen,
                0,
                NESPPU.SCREEN_WIDTH
            )

            //graphicsContext2D.drawImage(image, 0.0, 0.0, width, height)

            val nanos = simulation.lastFrameDelayInNanos.toDouble()
            val default = 1000000000.0 / simulation.cartridge.header.tvType.framerate
            val percentage = (nanos / default) * 100

            println("OVERHEAD: ${String.format("%.2f", percentage)}%")

            //graphicsContext2D.fill = Color.RED
//
            //graphicsContext2D.fillText(String.format("%.2f", percentage), 0.0, 100.0)
            //graphicsContext2D.fillText(String.format("%.2f", nanos), 0.0, 120.0)
            //graphicsContext2D.fillText(String.format("%.2f", default), 0.0, 140.0)

        }

    }

}