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

package io.github.gaeqs.nes4jams.cpu.instruction

import io.github.gaeqs.nes4jams.cpu.OLC6502
import kotlin.reflect.KFunction

enum class NESAddressingMode(
    val usesWordInAssembler: Boolean,
    val bytesUsed: Int,
    val addressingFunction: KFunction<Boolean>
) {
    IMPLIED(false, 0, OLC6502::imp),
    IMMEDIATE(false, 1, OLC6502::imm),
    ZERO_PAGE(false, 1, OLC6502::zp0),
    ZERO_PAGE_X(false, 1, OLC6502::zpx),
    ZERO_PAGE_Y(false, 1, OLC6502::zpy),
    RELATIVE(true, 1, OLC6502::rel),
    ABSOLUTE(true, 2, OLC6502::abs),
    ABSOLUTE_X(true, 2, OLC6502::abx),
    ABSOLUTE_Y(true, 2, OLC6502::aby),
    INDIRECT(true, 2, OLC6502::ind),
    INDIRECT_X(false, 1, OLC6502::inx),
    INDIRECT_Y(false, 1, OLC6502::iny);

    companion object {

        fun getCompatibleAddressingModes(parameter: String): Pair<Set<NESAddressingMode>, String> {
            val trimmed = parameter.trim()

            // IMPLIED
            if (trimmed.isEmpty() || trimmed == "A" || trimmed == "a") {
                return Pair(setOf(IMPLIED), "")
            }

            // IMMEDIATE
            if (trimmed.startsWith("#")) {
                return Pair(setOf(IMMEDIATE), trimmed.substring(1))
            }

            // INDIRECT, INDIRECT X
            if (trimmed.endsWith(')') && trimmed.startsWith('(')) {
                var candidate = trimmed.substring(1, trimmed.length - 1).trim()
                if (candidate.endsWith("x") || candidate.endsWith("X")) {
                    candidate = candidate.substring(0, candidate.length - 1).trim()
                    if (candidate.endsWith(',')) {
                        return Pair(setOf(INDIRECT_X), candidate.substring(0, candidate.length - 1))
                    }
                }
                return Pair(setOf(INDIRECT), trimmed)
            }

            // ABSOLUTE X, ZERO PAGE X
            if (trimmed.endsWith('x') || trimmed.endsWith("X")) {
                val candidate = trimmed.substring(0, trimmed.length - 1).trim()
                if (candidate.endsWith(',')) {
                    return Pair(setOf(ABSOLUTE_X, ZERO_PAGE_X), candidate.substring(0, candidate.length - 1))
                }
            }

            //  ABSOLUTE Y, ZERO PAGE Y, INDIRECT Y
            if (trimmed.endsWith('y') || trimmed.endsWith("Y")) {
                val candidate = trimmed.substring(0, trimmed.length - 1).trim()
                if (candidate.endsWith(',')) {
                    val secondCandidate = candidate.substring(0, candidate.length - 1).trim()
                    if (secondCandidate.startsWith('(') && secondCandidate.endsWith(')')) {
                        return Pair(setOf(INDIRECT_Y), secondCandidate.substring(1, secondCandidate.length - 1))
                    }
                    return Pair(setOf(ABSOLUTE_Y, ZERO_PAGE_Y), candidate.substring(0, candidate.length - 1))
                }
            }

            return Pair(setOf(ZERO_PAGE, RELATIVE, ABSOLUTE), parameter)
        }

    }
}
