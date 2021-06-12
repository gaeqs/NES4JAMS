package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.label.OLC6502Label
import io.github.gaeqs.nes4jams.utils.extension.isLabelLegal
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpresionWithInvalids
import io.github.gaeqs.nes4jams.utils.extension.removeComments
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.utils.LabelUtils
import net.jamsimulator.jams.utils.StringUtils
import java.util.*

class NESAssemblerFile(val name: String, val rawData: String, val assembler: NESAssembler) {

    val lines = StringUtils.multiSplit(rawData, "\n", "\r")
    val equivalents = mutableMapOf<String, String>()
    val labels = mutableMapOf<String, OLC6502Label>()

    val labelsToAdd: Queue<String> = LinkedList()
    val convertToGlobalLabel = mutableSetOf<String>()
    val instructions = LinkedList<NESInstructionSnapshot>()
    val directives = LinkedList<NESDirectiveSnapshot>()

    fun searchLabel(label: String): OLC6502Label? {
        return labels[label] ?: assembler.globalLabels[label]
    }

    fun replaceAndEvaluate(string: String, replacementsToSearch: Set<String>): Int? {
        var result = string

        var replaced = false
        for (key in replacementsToSearch) {
            val equivalence = equivalents[key]
            if (equivalence != null) {
                result = result.replace(key, "$equivalence")
                replaced = true
            } else {
                val label = searchLabel(key)
                if (label != null) {
                    result = result.replace(label.key, "${label.address}")
                    replaced = true
                }
            }
        }

        val (value, invalids) = result.parseParameterExpresionWithInvalids()
        if (value == null) throw AssemblerException("Bad format: $string")
        if (invalids.isEmpty()) {
            return value.value
        }

        if (replaced) {
            // Try to replace again!
            return replaceAndEvaluate(result, invalids)
        }

        return null
    }


    fun setAsGlobalLabel(executingLine: Int, label: String?) {
        convertToGlobalLabel.add(label!!)
        val instance = labels.remove(label)
        if (instance != null) {
            val global = assembler.addGlobalLabel(
                executingLine,
                instance.key,
                instance.address,
                instance.originFile,
                instance.originLine
            )
            labels[global.key] = global
        }
    }


    fun scan() {
        lines.forEachIndexed { index, line ->

            try {
                scanLine(index, line)
            } catch (ex: Exception) {
                if (ex is AssemblerException) throw ex
                else throw AssemblerException(index, "Error while parsing line \"$line\".", ex)
            }
        }
        while (!labelsToAdd.isEmpty()) checkLabel(lines.size, labelsToAdd.poll(), assembler.memoryPointer)
    }

    fun assembleInstructions() {
        instructions.forEach { it.assemble(this) }
    }

    fun executeDirectivesSecondPass() {
        directives.forEach { it.executeSecondPass(this) }
    }

    private fun scanLine(index: Int, line: String) {
        var sanity = sanityLine(line)

        val equivalenceIndex = sanity.indexOf('=')
        if (equivalenceIndex != -1) {
            val key = sanity.substring(0, equivalenceIndex).filter { !it.isWhitespace() }
            val value = sanity.substring(equivalenceIndex + 1).filter { !it.isWhitespace() }
            if (key.isEmpty()) throw AssemblerException(index, "Equivalence key is empty")
            if (value.isEmpty()) throw AssemblerException(index, "Equivalence value is empty")

            if (equivalents.containsKey(key)) {
                throw AssemblerException(index, "Equivalence for the key $key already exists!")
            }
            if (searchLabel(key) != null) {
                throw AssemblerException(index, "A label with the same key as the equivalence $key already exists!")
            }

            equivalents[key] = value
            return
        }

        val labelIndex = LabelUtils.getLabelFinishIndex(sanity)
        var labelAddress: UShort? = assembler.memoryPointer
        var label: String? = null
        if (labelIndex != -1) {
            label = sanity.substring(0, labelIndex)
            sanity = sanity.substring(labelIndex + 1).trim()
        }

        if (sanity.isEmpty()) {
            if (label != null) {
                labelsToAdd += label
            }
            return
        }

        if (sanity[0] == '.') {
            val snapshot = NESDirectiveSnapshot(index, labelAddress!!, sanity)
            directives += snapshot
            snapshot.scan()
            labelAddress = snapshot.executeFirstPass(this)
        } else {
            val snapshot = NESInstructionSnapshot(index, labelAddress!!, sanity, line)
            instructions += snapshot
            assembler.addMemory((1 + snapshot.scan()).toUInt())
        }

        if (labelAddress == null) {
            if (label != null) {
                labelsToAdd += label
            }
            return
        }

        if (label != null) {
            checkLabel(index, label, labelAddress)
        }

        while (!labelsToAdd.isEmpty()) {
            checkLabel(index, labelsToAdd.poll(), labelAddress)
        }

    }

    private fun checkLabel(index: Int, label: String, address: UShort) {
        if (!label.isLabelLegal()) throw AssemblerException(
            index,
            "Label $label contains illegal characters."
        )

        if (convertToGlobalLabel.contains(label)) {
            assembler.addGlobalLabel(index, label, address, name, index)
        } else {
            if (assembler.globalLabels.containsKey(label)) {
                throw AssemblerException(index, "Label $label is already defined as a global label.")
            }
            if (labels.containsKey(label)) {
                throw AssemblerException(index, "Label $label already defined.")
            }
            if (equivalents.containsKey(label)) {
                throw AssemblerException(index, "An equivalence with the same key as the label $label already exists!")
            }
            labels[label] = OLC6502Label(label, address, name, index, false)
        }
    }

    private fun sanityLine(line: String): String {
        return line.removeComments().trim()
    }

}