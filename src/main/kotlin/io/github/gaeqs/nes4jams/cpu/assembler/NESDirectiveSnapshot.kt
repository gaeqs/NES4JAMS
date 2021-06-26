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

import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import net.jamsimulator.jams.utils.StringUtils
import kotlin.math.min

class NESDirectiveSnapshot(val line: NESAssemblerLine, var address: UShort?, val raw: String) {

    val mnemonic: String
    val parameters: Array<String>
    val directive: NESDirective?

    init {
        var mnemonicIndex = raw.indexOf(' ')
        val tabIndex = raw.indexOf("\t")
        if (mnemonicIndex == -1) mnemonicIndex = tabIndex
        else if (tabIndex != -1) mnemonicIndex = min(mnemonicIndex, tabIndex)

        if (mnemonicIndex == -1) {
            mnemonic = raw.substring(1)
            parameters = emptyArray()
        } else {
            mnemonic = raw.substring(1, mnemonicIndex)
            val raw = raw.substring(mnemonicIndex + 1)
            parameters = StringUtils.multiSplitIgnoreInsideString(raw, false, " ", ",", "\t")
                .toTypedArray()
        }

        directive = NESDirective.DIRECTIVES[mnemonic.lowercase()]
        if (directive == null) {
            line.file.assembler.log?.printWarning("Couldn't find directive .$mnemonic!")
        }
    }

    fun callFirstPass() {
        directive?.firstPassExecute(line.file, line.index, parameters)
    }

    fun callSecondPass() {
        directive?.secondPassExecute(line.file, line.index, parameters)
    }

    fun callThirdPass(): UShort? {
        if (directive != null) {
            val result = directive.thirdPassExecute(line.file, line.index, address!!, parameters)
            if (result != null) address = result
            return result
        }
        return null
    }

    fun callFourthPass() {
        directive?.fourthPassExecute(line.file, line.index, address!!, parameters)
    }

}