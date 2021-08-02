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

package io.github.gaeqs.nes4jams.gui.simulation.tablename

import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import io.github.gaeqs.nes4jams.gui.project.NESSimulationPane
import io.github.gaeqs.nes4jams.gui.simulation.display.Display
import javafx.animation.AnimationTimer
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import java.nio.IntBuffer

class NESSimulationPatternTableDisplay(val simulationPane: NESSimulationPane) : Display() {

    companion object {
        const val ASPECT_RATIO = 1.0
    }

    private val buffer = IntBuffer.allocate(128 * 128)
    private val screen = buffer.array()
    private val pixelBuffer = PixelBuffer(
        128, 128,
        buffer, PixelFormat.getIntArgbPreInstance()
    )
    private val image = WritableImage(pixelBuffer).apply { setImage(this) }
    private val handler = RedrawHandler().apply { start() }


    override fun stop() {
        handler.stop()
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

    private inner class RedrawHandler : AnimationTimer() {

        val chr = ByteArray(4096)
        var drawnFrame = -1L

        override fun handle(now: Long) {
            if (simulationPane.parentTab?.isSelected == false || !simulationPane.simulation.isRunning) return
            val simulation = simulationPane.simulation
            if (drawnFrame >= simulation.frame + 10) return
            drawnFrame = simulation.frame


            simulation.runSynchronized {
                // Let's copy it to let the simulation free
                repeat(chr.size) { chr[it] = simulation.ppu.ppuRead(it.toUShort()).toByte() }
            }

            val pcx = PictureExchangeImage.fromCHRData(chr)
            pcx.toPixelBuffer(buffer = screen)
            pixelBuffer.updateBuffer { null }
        }

    }

}