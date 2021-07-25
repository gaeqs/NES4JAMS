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
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import javafx.animation.AnimationTimer
import javafx.scene.canvas.Canvas
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage

class NESSimulationDisplay(val simulation: NESSimulation) :
    Canvas(0.0, 0.0) {

    companion object {
        const val ASPECT_RATIO = 256.0 / 240.0
    }

    private val image = WritableImage(NESPPU.SCREEN_WIDTH, NESPPU.SCREEN_HEIGHT)
    private val handler = RedrawHandler().apply { start() }

    init {
        graphicsContext2D.isImageSmoothing = false
    }

    fun stop() {
        handler.stop()
    }

    fun fitToSize(width: Double, height: Double) {
        val scaledWidth = width / ASPECT_RATIO

        if (scaledWidth > height) {
            this.width = height * ASPECT_RATIO
            this.height = height
        } else {
            this.width = width
            this.height = width / ASPECT_RATIO
        }
    }

    private inner class RedrawHandler : AnimationTimer() {

        val screen = ByteArray(NESPPU.SCREEN_WIDTH * NESPPU.SCREEN_HEIGHT)

        override fun handle(now: Long) {
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

            graphicsContext2D.drawImage(image, 0.0, 0.0, width, height)
        }

    }

}