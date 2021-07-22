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

package io.github.gaeqs.nes4jams.util

data class ExponentialPrgBanks(val bytes: ULong, val multiplier: UByte, val exponent: UByte)

class ExponentialPrgBanksFinder {

    companion object {

        private val values = mutableListOf<ExponentialPrgBanks>()

        init {
            for (exponent in 0..63) {
                val value = 1UL shl exponent
                for (multiplier in 1..7 step 2) {
                    val bytes = multiplier.toULong() * value
                    //Check overflow
                    if (bytes / multiplier.toULong() == value) {
                        values += ExponentialPrgBanks(bytes, multiplier.toUByte(), exponent.toUByte())
                    }
                }
            }
            values.sortBy { it.bytes }
        }

        @JvmStatic
        fun forEach(consumer: (ExponentialPrgBanks) -> (Unit)) {
            values.forEach(consumer)
        }

        @JvmStatic
        fun getAll() = values.toList()

        @JvmStatic
        fun findBestMatch(banks: ULong): Pair<ExponentialPrgBanks, Int> {
            var i = values.binarySearch { it.bytes.compareTo(banks) }
            if (i < 0) i = -i - 1
            return if (i >= values.size) Pair(values.last(), values.lastIndex)
            else Pair(values[i], i)
        }
    }

}