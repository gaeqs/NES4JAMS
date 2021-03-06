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

import io.github.gaeqs.nes4jams.gui.display.DisplayView

/**
 * Basic implementation of a display that manages the image update automatically. You can extend this class
 * to provide more complex behaviour.
 *
 * To send a new frame to this display, use [startDataTransmission] and update the given buffer. This method
 * is thread-safe: you can call it from any thread. The image will be displayed on the next frame.
 *
 */
open class BasicDisplay(width: Int, height: Int) : DisplayView(width, height),
    Display {

    private val aspectRatio = width.toFloat() / height

    init {
        setOnMouseClicked { requestFocus(); it.consume() }
    }

    override fun fitToSize(width: Double, height: Double) {
        val scaledWidth = width / aspectRatio

        if (scaledWidth > height) {
            fitWidth = height.toFloat() * aspectRatio
            fitHeight = height.toFloat()
        } else {
            this.fitWidth = width.toFloat()
            this.fitHeight = width.toFloat() / aspectRatio
        }
    }

    override fun asNode() = this
}