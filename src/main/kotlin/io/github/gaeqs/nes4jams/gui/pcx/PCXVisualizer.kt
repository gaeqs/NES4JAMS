/*
 *  MIT License
 *
 *  Copyright (c) 2022 Gael Rial Costas
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

package io.github.gaeqs.nes4jams.gui.pcx

import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import javafx.scene.Cursor
import net.jamsimulator.jams.gui.image.nearest.NearestImageView

open class PCXVisualizer(
    val image: PictureExchangeImage,
    zoom: Double = DEFAULT_ZOOM
) :
    NearestImageView(null, image.header.width * zoom, image.header.height * zoom) {

    companion object {
        const val DEFAULT_ZOOM = 10.0
    }

    var zoom = zoom
        set(value) {
            if (field == value) return
            field = value
            fitWidth = image.header.width * field
            fitHeight = image.header.height * field
        }

    var palette = listOf(0xFF000000.toInt(), 0xFF555555.toInt(), 0xFFAAAAAA.toInt(), 0xFFFFFFFF.toInt())
        set(value) {
            if (value.size != 4) throw IllegalArgumentException("Array must have only 4 elements.")
            field = value
            image.toFXImage(PictureExchangeImage.ColorPalette(value.toIntArray()), currentImage)
        }

    protected var currentImage = image.toFXImage(PictureExchangeImage.ColorPalette(palette.toIntArray()))

    init {
        setImage(currentImage)
        cursor = Cursor.DEFAULT
    }
}