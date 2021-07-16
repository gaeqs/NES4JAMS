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

import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import io.github.gaeqs.nes4jams.cpu.label.NESLabel
import io.github.gaeqs.nes4jams.utils.Value
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpressionWithInvalids
import io.github.gaeqs.nes4jams.utils.extension.removeComments
import net.jamsimulator.jams.mips.assembler.Macro
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.mips.directive.defaults.DirectiveEndmacro
import net.jamsimulator.jams.utils.StringUtils
import java.util.*

/**
 * Represents a file being assembled.
 */
class NESAssemblerFile(val name: String, rawData: String, val assembler: NESAssembler) {

    /**
     * The raw lines.
     */
    val rawLines = StringUtils.multiSplit(rawData, "\n", "\r")

    /**
     * The lines with its metadata. This list is loaded after the first step.
     */
    val lines = mutableListOf<NESAssemblerLine>()

    /**
     * The local labels of this file. This list is loaded after the first step.
     */
    val localLabels = mutableMapOf<String, NESLabel>()

    /**
     * The local equivalents of this file. This list is loaded after the first step.
     */
    val localEquivalents = mutableMapOf<String, NESAssemblerEquivalent>()

    /**
     * The local macros of this file. This list is loaded after the first step.
     */
    val localMacros = mutableMapOf<String, Macro>()

    /**
     * The labels whose address is assigned in the next directive / instruction. This queue is used in the third step.
     */
    val assignLabelQueue: Queue<NESLabel> = LinkedList()

    /**
     * The macro currently being defined. This value is used in the first step.
     */
    private var definingMacro: Macro? = null

    /**
     * Search the label that matches the given name. This method searches the local and global labels.
     */
    fun searchLabel(label: String): NESLabel? {
        return localLabels[label] ?: assembler.globalLabels[label]
    }

    /**
     * Search the equivalent that matches the given name. This method searches the local and global equivalent.
     */
    fun searchEquivalent(key: String): NESAssemblerEquivalent? {
        return localEquivalents[key] ?: assembler.globalEquivalents[key]
    }

    /**
     * Search the macro that matches the given name. This method searches the local and global macro.
     */
    fun searchMacro(name: String): Macro? {
        return localMacros[name] ?: assembler.globalMacros[name]
    }

    /**
     * Evaluates the given expression. This method is recursive.
     *
     * If the evaluation is possible, this method returns a value.
     * This method also returns a boolean representing whether the value is a word.
     * This boolean is always returned.
     *
     * @param expression the expression to evaluate.
     * @param alreadySearched this collection is used to detect cyclic dependencies.
     * @return the value if the evaluation was possible and whether the value is a word.
     * @throws AssemblerException when the expression has a bad format.
     */
    tailrec fun evaluate(expression: String, alreadySearched: Collection<String> = emptySet()): Pair<Value?, Boolean> {
        val (value, invalids) = expression.parseParameterExpressionWithInvalids()
        if (value == null) throw AssemblerException("Bad expression format: $expression")
        if (invalids.isEmpty()) {
            return Pair(value, value.isWord)
        }

        var result = expression
        var replaced = false
        for (key in invalids) {
            if (key in alreadySearched) {
                throw AssemblerException("Cyclic dependency! $key.")
            }
            val equivalence = searchEquivalent(key)
            if (equivalence != null) {
                if (equivalence.value == null) {
                    equivalence.evaluateValue()
                }
                if (equivalence.value != null) {
                    equivalence.value?.let {
                        result = result.replace(key, it.value.toString())
                        replaced = true
                    }

                }
            } else {
                val label = searchLabel(key)
                if (label?.address != null) {
                    result = result.replace(label.key, "${label.address}")
                    replaced = true
                }
            }
        }

        return if (replaced) evaluate(result, alreadySearched - invalids) else Pair(null, value.isWord)
    }

    /**
     * Starts a macro definition.
     *
     * @param line the line executing this operation.
     * @param name the name of the macro.
     * @param parameters the parameters of the macro.
     * @throws AssemblerException when a macro is already being defined.
     */
    fun startMacroDefinition(line: Int, name: String, parameters: Array<String>) {
        if (definingMacro != null) throw AssemblerException(line, "Another macro is already being defined!")

        if (NESInstruction.INSTRUCTIONS[name.uppercase()] != null)
            throw AssemblerException(line, "Macro name collision with the instruction ${name.uppercase()}!")

        definingMacro = Macro(name, parameters)
    }

    fun stopMacroDefinition(line: Int) {
        val macro = definingMacro ?: throw AssemblerException(line, "No macro is being defined!")

        if (macro.name[0] == '_') {
            localMacros[macro.name] = macro
        } else {
            assembler.addGlobalMacro(line, macro)
        }

        definingMacro = null
    }

    // region STEP 1

    fun scanMetadata() {
        rawLines.forEachIndexed { index, line ->
            try {
                scanMetadataForLine(index, line, true)
            } catch (ex: Exception) {
                if (ex is AssemblerException) throw ex
                else throw AssemblerException(index, "Error while parsing line \"$line\".", ex)
            }
        }
    }

    private fun scanMetadataForLine(
        index: Int,
        original: String,
        add: Boolean,
        suffix: String = ""
    ): NESAssemblerLine? {
        val sanity = original.removeComments().trim()
        if (sanity.isBlank()) return null
        val line = NESAssemblerLine(this, index, sanity, original, suffix)

        if (definingMacro != null) {
            if (line.directive != null && line.directive.directive is DirectiveEndmacro) {
                stopMacroDefinition(index)
            } else {
                definingMacro!!.addLine(sanity)
                return null
            }
        }

        if (add) lines += line

        line.label?.manageLabelAddition(index)
        line.equivalent?.manageEquivalentAddition(index)
        line.directive?.callFirstPass()

        return line
    }

    private fun NESLabel.manageLabelAddition(index: Int) {
        if (isGlobal()) assembler.addGlobalLabel(index, this)
        else {

            if (key in localLabels) {
                throw AssemblerException(index, "The local label $key is already defined.")
            }

            if (key in localEquivalents) {
                throw AssemblerException(index, "The local label $key is already defined as an equivalent.")
            }

            localLabels[key] = this
        }
    }

    private fun NESAssemblerEquivalent.manageEquivalentAddition(index: Int) {
        if (isGlobal()) assembler.addGlobalEquivalent(index, this)
        else {

            if (key in localEquivalents) {
                throw AssemblerException(index, "The local equivalent $key is already defined.")
            }

            if (key in localLabels) {
                throw AssemblerException(index, "The local equivalent $key is already defined as a label.")
            }

            localEquivalents[key] = this
        }
    }

    // endregion

    // region STEP 2

    fun executeMacros() {
        val iterator = lines.listIterator()

        while (iterator.hasNext()) {
            val line = iterator.next()
            line.directive?.callSecondPass()
            if (line.macroCall == null) return
            iterator.remove()

            val macro = searchMacro(line.macroCall.mnemonic)
                ?: throw AssemblerException(line.index, "Macro ${line.macroCall.mnemonic} not found!")

            try {
                val lines = manageMacroExecution(macro, line.macroCall)
                lines.asReversed().forEach { iterator.add(it) }
                repeat(lines.size) { iterator.previous() }
            } catch (ex: Exception) {
                if (ex is AssemblerException) throw ex
                else throw AssemblerException(line.index, "Error while parsing line \"${line.original}\".", ex)
            }
        }
    }


    private fun manageMacroExecution(macro: Macro, call: NESMacroCallSnapshot): List<NESAssemblerLine> {
        val lines = mutableListOf<NESAssemblerLine>()
        macro.executeMacro(call.parameters, call.line.index, assembler.callsToMacros) { lineNumber, line, sufix ->
            lines += scanMetadataForLine(lineNumber, line, false, sufix) ?: return@executeMacro
        }
        assembler.addCallToMacro()
        return lines
    }

    // endregion

    // region STEP 3

    fun assignAddresses() {
        lines.forEach {
            var address: UShort? = null
            when {
                it.directive != null -> {
                    it.directive.address = assembler.selectedBank.pointer
                    it.directive.callThirdPass()
                    address = it.directive.address!!
                }
                it.instruction != null -> {
                    it.instruction.address = assembler.selectedBank.pointer
                    val size = it.instruction.calculateInstructionAddressingMode()
                    assembler.selectedBank.skip(size + 1)
                    address = it.instruction.address!!
                }
            }

            if (address == null) {
                if (it.label != null) {
                    assignLabelQueue += it.label
                }
            } else {
                it.label?.address = address
                while (!assignLabelQueue.isEmpty()) {
                    assignLabelQueue.poll().address = address
                }
            }
        }
    }

    // endregion

    // region STEP 4

    fun assignValues() {
        lines.forEach {
            it.instruction?.writeValue()
            it.directive?.callFourthPass()
        }
    }

    // endregion
}