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

import com.sun.javafx.geom.BaseBounds
import com.sun.javafx.geom.transform.BaseTransform
import com.sun.javafx.scene.DirtyBits
import com.sun.javafx.scene.NodeHelper
import javafx.application.Platform
import javafx.beans.property.FloatPropertyBase
import javafx.scene.Node
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

/**
 * Basic implementation of a display that can be updated faster than a regular
 * [WritableImage][javafx.scene.image.WritableImage].
 *
 * The constructor of this DisplayView requires a width and height. These values will be the resolution of the display.
 * These values cannot be changed after the display initialization.
 *
 * Use [startDataTransmission] to update the display. Update the given array using ARGB colors.
 * This method is thread-safe. You can use it in any thread.
 *
 * This display requires to [dispose] all its data when it's not useful anymore.
 */
open class DisplayView(val width: Int, val height: Int) : Node() {

    companion object {
        init {
            DisplayViewHelper.setDisplayViewAccessor(object : DisplayViewAccessor {
                override fun doCreatePeer(node: Node) = (node as DisplayView).doCreatePeer()
                override fun doUpdatePeer(node: Node) = (node as DisplayView).doUpdatePeer()
                override fun doComputeContains(node: Node, localX: Double, localY: Double) = true
                override fun doComputeGeomBounds(node: Node, bounds: BaseBounds, tx: BaseTransform) =
                    (node as DisplayView).doComputeBounds(bounds, tx)
            })
        }
    }

    init {
        DisplayViewHelper.initHelper(this)
    }

    private val xPropertyDelegate = geometryLazyProperty("x")
    private val yPropertyDelegate = geometryLazyProperty("y")
    private val fitWidthPropertyDelegate = viewportLazyProperty("fitWidth")
    private val fitHeightPropertyDelegate = viewportLazyProperty("fitHeight")

    private val data = IntArray(width * height)
    private val lock = ReentrantLock()

    private var validDimensions = false
    private var calculatedWidth = 0.0f
    private var calculatedHeight = 0.0f

    var disposed = false
        private set

    val xProperty by xPropertyDelegate
    val yProperty by yPropertyDelegate
    val fitWidthProperty by fitWidthPropertyDelegate
    val fitHeightProperty by fitHeightPropertyDelegate

    var x: Float
        get() = if (xPropertyDelegate.isInitialized()) xProperty.value else 0.0f
        set(value) = xProperty.set(value)


    var y: Float
        get() = if (yPropertyDelegate.isInitialized()) yProperty.value else 0.0f
        set(value) = yProperty.set(value)

    var fitWidth: Float
        get() = if (fitWidthPropertyDelegate.isInitialized()) fitWidthProperty.value else 0.0f
        set(value) = fitWidthProperty.set(value)

    var fitHeight: Float
        get() = if (fitHeightPropertyDelegate.isInitialized()) fitHeightProperty.value else 0.0f
        set(value) = fitHeightProperty.set(value)

    /**
     * Updates the display with the data inside the buffer provided by the lambda.
     *
     * Use colors in ARGB format to update the buffer.
     *
     * This method is thread-safe. You can use it in any thread.
     *
     * If the display is disposed, this method does nothing.
     */
    fun startDataTransmission(function: (IntArray) -> Unit) {
        lock.withLock {
            if (disposed) return
            function(data)
        }
        Platform.runLater {
            NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS)
        }
    }

    /**
     * Disposes this display, freeing the texture used and all its data.
     *
     * Use this method if you don't need this display anymore.
     */
    open fun dispose() {
        lock.withLock {
            if (disposed) return
            val peer: NGDisplayView = NodeHelper.getPeer(this) ?: return
            peer.dispose()
            disposed = true
        }
    }

    private fun geometryLazyProperty(name: String) = lazy {
        object : FloatPropertyBase() {
            override fun getBean() = this@DisplayView
            override fun getName() = name
            override fun invalidated() {
                invalidateWidthHeight()
                NodeHelper.markDirty(this@DisplayView, DirtyBits.NODE_GEOMETRY)
                NodeHelper.geomChanged(this@DisplayView)
            }
        }
    }

    private fun viewportLazyProperty(name: String) = lazy {
        object : FloatPropertyBase() {
            override fun getBean() = this@DisplayView
            override fun getName() = name
            override fun invalidated() {
                invalidateWidthHeight()
                NodeHelper.markDirty(this@DisplayView, DirtyBits.NODE_VIEWPORT)
                NodeHelper.geomChanged(this@DisplayView)
            }
        }
    }

    private fun doCreatePeer() = NGDisplayView(width, height, data, lock)

    private fun doUpdatePeer() {
        val peer: NGDisplayView = NodeHelper.getPeer(this)
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            peer.x = x
            peer.y = y
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_VIEWPORT)) {
            recomputeWidthHeight()
            peer.w = calculatedWidth
            peer.h = calculatedHeight
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            peer.markForRefresh()
        }
    }

    private fun doComputeBounds(bounds: BaseBounds, tx: BaseTransform): BaseBounds {
        recomputeWidthHeight()
        val temp = bounds.deriveWithNewBounds(x, y, 0.0f, x + calculatedWidth, y + calculatedHeight, 0.0f)
        return tx.transform(temp, temp)
    }

    private fun invalidateWidthHeight() {
        validDimensions = false
    }

    private fun recomputeWidthHeight() {
        if (validDimensions) return
        calculatedWidth = max(fitWidth, 0.0f)
        calculatedHeight = max(fitHeight, 0.0f)

        validDimensions = true
    }

}