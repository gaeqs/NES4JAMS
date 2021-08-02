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

import javafx.animation.AnimationTimer
import javafx.scene.Node
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import net.jamsimulator.jams.gui.image.NearestImageView
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Basic implementation of a display that manages the image update automatically. You can extend this class
 * to provide more complex behaviour.
 *
 * To send a new frame to this display, use [startDataTransmission] and update the given buffer. This method
 * is thread-safe: you can call it from any thread. The image will be displayed on the next frame.
 *
 * You can disable and enable the draw of frames using the parameter [drawEnabled]
 *
 */
open class BasicDisplay(imageWidth: Int, imageHeight: Int) : NearestImageView(null, 0.0, 0.0),
    Display {

    private val aspectRatio = imageWidth.toDouble() / imageHeight

    private val imageBuffer = IntBuffer.allocate(imageWidth * imageHeight)
    private val pixels = imageBuffer.array()
    private val pixelBuffer = PixelBuffer(imageWidth, imageHeight, imageBuffer, PixelFormat.getIntArgbPreInstance())
    private val image = WritableImage(pixelBuffer)

    private val buffer = IntArray(imageWidth * imageHeight)

    private val handler = RedrawHandler()
    private val lock = ReentrantLock()
    private var dataReady = false

    /**
     * This variable controls whether the display should be updated.
     *
     * You can override this variable in your implementation.
     */
    open var drawEnabled = true

    init {
        setImage(image)
        handler.start()
        setOnMouseClicked { requestFocus(); it.consume() }
    }

    override fun fitToSize(width: Double, height: Double) {
        val scaledWidth = width / aspectRatio

        if (scaledWidth > height) {
            fitWidth = height * aspectRatio
            fitHeight = height
        } else {
            this.fitWidth = width
            this.fitHeight = width / aspectRatio
        }
    }

    override fun killDisplay() {
        handler.stop()
    }

    override fun asNode() = this

    /**
     * Starts the transmission of a frame to this display's pipeline.
     *
     * Use the given buffer to write the new data.
     * This method is thread-safe: you can call it from any thread.
     */
    fun startDataTransmission(function: (IntArray) -> Unit) {
        lock.withLock {
            function(buffer)
            dataReady = true
        }
    }

    private inner class RedrawHandler : AnimationTimer() {

        override fun handle(now: Long) {
            if (!drawEnabled) return
            lock.withLock {
                if (!dataReady) return
                buffer.copyInto(pixels)
                dataReady = false
            }
            pixelBuffer.updateBuffer { null }
        }

    }
}