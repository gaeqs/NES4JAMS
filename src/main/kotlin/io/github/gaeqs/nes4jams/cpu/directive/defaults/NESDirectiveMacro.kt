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

package io.github.gaeqs.nes4jams.cpu.directive.defaults

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerFile
import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.mips.directive.defaults.DirectiveMacro

class NESDirectiveMacro : NESDirective(NAME, false) {

    companion object {
        const val NAME = "macro"
    }

    override fun firstPassExecute(file: NESAssemblerFile, lineNumber: Int, parameters: Array<String>) {
        if (parameters.isEmpty()) throw AssemblerException(
            lineNumber,
            "." + DirectiveMacro.NAME + " must have at least one parameter."
        )

        val name = parameters[0]
        if (name.startsWith('.') || name.contains("(") || name.contains(")")) throw AssemblerException(
            lineNumber,
            "Macro name cannot contain parenthesis!"
        )
        if (file.searchMacro(name) != null) {
            throw AssemblerException(lineNumber, "Macro $name already exists!")
        }

        val macroParameters: MutableList<String> = ArrayList()

        for (i in 1 until parameters.size) {
            var value = parameters[i]
            if (value == "(" || value == "()") {
                if (i == 1) continue
                panic(lineNumber, value, i)
            } else if (value == ")") {
                if (i == parameters.size - 1) continue
                panic(lineNumber, value, i)
            }
            if (value.startsWith("(")) {
                if (i != 1) panic(lineNumber, value, i)
                value = value.substring(1)
            }
            if (value.endsWith(")")) {
                if (i != parameters.size - 1) panic(lineNumber, value, i)
                value = value.substring(0, value.length - 1)
            }
            if (!value.startsWith("%")) panic(lineNumber, value, i)
            if (value.length == 1) panic(lineNumber, value, 1)
            macroParameters.add(value)
        }

        file.startMacroDefinition(lineNumber, name, macroParameters.toTypedArray())
    }

    override fun secondPassExecute(file: NESAssemblerFile, lineNumber: Int, parameters: Array<String>) {
    }

    override fun thirdPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ): UShort? {
        return null
    }

    override fun fourthPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort?,
        parameters: Array<String>
    ) {
    }


    private fun panic(line: Int, value: String, i: Int) {
        throw AssemblerException(line, "Invalid macro parameter '$value'! (Index $i)")
    }
}