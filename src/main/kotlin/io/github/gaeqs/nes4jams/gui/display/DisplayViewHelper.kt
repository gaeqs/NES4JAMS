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
import com.sun.javafx.scene.NodeHelper
import com.sun.javafx.sg.prism.NGNode
import com.sun.javafx.util.Utils
import javafx.scene.Node
import java.lang.IllegalStateException

class DisplayViewHelper private constructor() : NodeHelper() {

    companion object {
        @JvmStatic
        private val INSTANCE = DisplayViewHelper()
        @JvmStatic
        private var accessor: DisplayViewAccessor? = null

        init {
            Utils.forceInit(DisplayViewHelper::class.java)
        }

        @JvmStatic
        fun initHelper(view: DisplayView) = setHelper(view, INSTANCE)

        @JvmStatic
        fun setDisplayViewAccessor (accessor: DisplayViewAccessor) {
            if(this.accessor != null) throw IllegalStateException()
            this.accessor = accessor
        }
    }

    override fun createPeerImpl(node: Node) = accessor?.doCreatePeer(node)

    override fun updatePeerImpl(node: Node) {
        super.updatePeerImpl(node)
        accessor?.doUpdatePeer(node)
    }


    override fun computeContainsImpl(node: Node, localX: Double, localY: Double) =
        accessor?.doComputeContains(node, localX, localY) ?: false

    override fun computeGeomBoundsImpl(node: Node, bounds: BaseBounds, tx: BaseTransform) =
        accessor?.doComputeGeomBounds(node, bounds, tx)

}

interface DisplayViewAccessor {
    fun doCreatePeer(node: Node): NGNode
    fun doUpdatePeer(node: Node)
    fun doComputeGeomBounds(node: Node, bounds: BaseBounds, tx: BaseTransform): BaseBounds?
    fun doComputeContains(node: Node, localX: Double, localY: Double): Boolean
}