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

package io.github.gaeqs.nes4jams.ppu

import io.github.gaeqs.nes4jams.util.extension.toRGB
import javafx.scene.paint.Color

class PPUColors {

    companion object {

        val COLORS = arrayOf(
            Color.rgb(84, 84, 84),
            Color.rgb(0, 30, 116),
            Color.rgb(8, 16, 144),
            Color.rgb(48, 0, 136),
            Color.rgb(68, 0, 100),
            Color.rgb(92, 0, 48),
            Color.rgb(84, 4, 0),
            Color.rgb(60, 24, 0),
            Color.rgb(32, 42, 0),
            Color.rgb(8, 58, 0),
            Color.rgb(0, 64, 0),
            Color.rgb(0, 60, 0),
            Color.rgb(0, 50, 60),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(152, 150, 152),
            Color.rgb(8, 76, 196),
            Color.rgb(48, 50, 236),
            Color.rgb(92, 30, 228),
            Color.rgb(136, 20, 176),
            Color.rgb(160, 20, 100),
            Color.rgb(152, 34, 32),
            Color.rgb(120, 60, 0),
            Color.rgb(84, 90, 0),
            Color.rgb(40, 114, 0),
            Color.rgb(8, 124, 0),
            Color.rgb(0, 118, 40),
            Color.rgb(0, 102, 120),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(236, 238, 236),
            Color.rgb(76, 154, 236),
            Color.rgb(120, 124, 236),
            Color.rgb(176, 98, 236),
            Color.rgb(228, 84, 236),
            Color.rgb(236, 88, 180),
            Color.rgb(236, 106, 100),
            Color.rgb(212, 136, 32),
            Color.rgb(160, 170, 0),
            Color.rgb(116, 196, 0),
            Color.rgb(76, 208, 32),
            Color.rgb(56, 204, 108),
            Color.rgb(56, 180, 204),
            Color.rgb(60, 60, 60),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
            Color.rgb(236, 238, 236),
            Color.rgb(168, 204, 236),
            Color.rgb(188, 188, 236),
            Color.rgb(212, 178, 236),
            Color.rgb(236, 174, 236),
            Color.rgb(236, 174, 212),
            Color.rgb(236, 180, 176),
            Color.rgb(228, 196, 144),
            Color.rgb(204, 210, 120),
            Color.rgb(180, 222, 120),
            Color.rgb(168, 226, 144),
            Color.rgb(152, 226, 180),
            Color.rgb(160, 214, 228),
            Color.rgb(160, 162, 160),
            Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0),
        )

        val INT_COLORS = IntArray(COLORS.size) { COLORS[it].toRGB() }

    }

}