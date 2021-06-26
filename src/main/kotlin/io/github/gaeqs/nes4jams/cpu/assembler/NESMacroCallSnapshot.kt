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

package io.github.gaeqs.nes4jams.cpu.assembler

import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.utils.StringUtils

class NESMacroCallSnapshot(
    val line: NESAssemblerLine,
    mnemonic: String,
    rawParameters: String
) {

    val parameters: Array<String>
    val mnemonic = mnemonic.uppercase()

    init {
        if (!rawParameters.startsWith('(') || !rawParameters.endsWith(')'))
            throw AssemblerException(line.index, "Invalid macro parameters")

        val raw = rawParameters.substring(1, rawParameters.length - 1)
        parameters = StringUtils.multiSplitIgnoreInsideString(raw, false, " ", ",", "\t")
            .toTypedArray()
    }

}