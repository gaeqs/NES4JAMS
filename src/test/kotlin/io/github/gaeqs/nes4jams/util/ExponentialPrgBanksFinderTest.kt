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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExponentialPrgBanksFinderTest {

    @Test
    fun testForEach() {
        ExponentialPrgBanksFinder.forEach {
            println(it)
            assertTrue(it.banks == it.multiplier * (1UL shl it.exponent.toInt()))
        }
    }

    @Test
    fun testFind() {
        val values = ExponentialPrgBanksFinder.getAll()

        repeat(10000) {
            val (match, index) = ExponentialPrgBanksFinder.findBestMatch(it.toULong())
            assertTrue(
                match.banks >= it.toULong(),
                "${match.banks} < $it. Index $index"
            )
            if (index > 0) {
                assertTrue(
                    values[index - 1].banks < it.toULong(),
                    "${values[index - 1].banks} >= $it. Index $index"
                )
            }
        }

        // Check max value
        val (match, index) = ExponentialPrgBanksFinder.findBestMatch(ULong.MAX_VALUE)
        assertTrue(index == values.lastIndex)
        assertTrue(match.banks < ULong.MAX_VALUE)

    }
}