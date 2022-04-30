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

package io.github.gaeqs.nes4jams.gui.display

import com.sun.javafx.geom.RectBounds
import com.sun.javafx.sg.prism.NGNode
import com.sun.prism.Graphics
import com.sun.prism.PixelFormat
import com.sun.prism.ResourceFactory
import com.sun.prism.Texture
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NGDisplayView(val width: Int, val height: Int, val data: IntArray, val lock: ReentrantLock) : NGNode() {

    var x: Float = 0.0f
        set(value) {
            if (field == value) return
            field = value
            geometryChanged()
        }

    var y: Float = 0.0f
        set(value) {
            if (field == value) return
            field = value
            geometryChanged()
        }

    var w = 0.0f
    var h = 0.0f

    var disposed = false

    private lateinit var texture: Texture
    private var textureInitialized = false

    override fun renderContent(g: Graphics) {
        if (disposed || w < 1 || h < 1) return
        refreshTexture(g.resourceFactory)
        g.drawTexture(
            texture, x, y, x + w, y + h, 0.0f, 0.0f,
            width.toFloat(), height.toFloat()
        )
        texture.unlock()
    }

    fun markForRefresh() {
        visualsChanged()
    }

    fun dispose() {
        if (textureInitialized) {
            texture.dispose()
            disposed = true
        }
    }

    override fun hasOverlappingContents() = false
    override fun supportsOpaqueRegions() = true
    override fun hasOpaqueRegion(): Boolean {
        if (!super.hasOpaqueRegion() || w < 1 || h < 1) return false
        return true
    }

    override fun computeOpaqueRegion(opaqueRegion: RectBounds): RectBounds {
        return opaqueRegion.deriveWithNewBounds(x, y, 0.0f, x + w, y + h, 0.0f) as RectBounds
    }

    private fun refreshTexture(factory: ResourceFactory) {
        if (!textureInitialized) {
            texture = factory.createTexture(
                PixelFormat.INT_ARGB_PRE,
                Texture.Usage.DYNAMIC,
                Texture.WrapMode.CLAMP_TO_EDGE,
                width,
                height
            )
            texture.linearFiltering = false
            textureInitialized = true
        } else {
            texture.lock()
        }

        lock.withLock {
            try {
                texture.update(
                    IntBuffer.wrap(data),
                    PixelFormat.INT_ARGB_PRE,
                    0, 0, 0, 0,
                    width, height,
                    width shl 2,
                    false
                )
            } catch (ex : NullPointerException) {
                // Thx JavaFX for this behavior :D
                textureInitialized = false
                texture.dispose()
            }
        }
    }
}