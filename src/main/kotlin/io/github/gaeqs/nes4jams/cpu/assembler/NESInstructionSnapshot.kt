package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.instruction.MatchResult
import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import kotlin.math.min

class NESInstructionSnapshot(val line: Int, val address: UShort, val raw: String, val original: String) {

    val mnemonic: String
    val parameters: String

    lateinit var instruction: NESInstruction
    var addressingMode: Pair<NESAddressingMode, MatchResult>? = null
    var addressingModeCandidates: Map<NESAddressingMode, MatchResult>? = null

    init {
        var mnemonicIndex: Int = raw.indexOf(' ')
        val tabIndex: Int = raw.indexOf("\t")
        if (mnemonicIndex == -1) mnemonicIndex = tabIndex
        else if (tabIndex != -1) mnemonicIndex = min(mnemonicIndex, tabIndex)

        if (mnemonicIndex == -1) {
            mnemonic = raw.uppercase()
            parameters = ""
        } else {
            mnemonic = raw.substring(0, mnemonicIndex).uppercase()
            parameters = raw.substring(mnemonicIndex + 1).trim()
        }
    }


    fun scan(): Int {
        val instruction = NESInstruction.INSTRUCTIONS[mnemonic]
            ?: throw AssemblerException(line, "Instruction $mnemonic not found.")
        this.instruction = instruction

        // Search for the valid addressing modes. We want them sorted by the bytes used for later
        val addressingModes =
            NESAddressingMode.matchingAddressingModesFor(parameters, instruction.supportedAddressingModes.keys)
                .sortedBy { it.first.usesWordInAssembler }

        if (addressingModes.isEmpty()) throw AssemblerException(
            line,
            "Addressing mode not found for $mnemonic $parameters."
        )
        // This is where the fun begins
        // If any of the results have a valid number, THAT's the result we want.
        // If not, we have to check for the label!

        // We can find the first value because the list is sorted.
        val validNumberResult = addressingModes.find { it.second.validNumber }
        if (validNumberResult != null) {
            addressingMode = validNumberResult
            return validNumberResult.first.bytesUsed
        }

        // The instruction is using a label! The label may vary from addressing mode to addressing mode.
        // That's why we have to store all the valid addressing modes. We can now filter them using the
        // label snapshot.
        val addressingModeCandidates = addressingModes
            // Check if the word type used by the addressing mode corresponds to the bytes used by the label
            // Instructions that use word can use bytes, but they have no priority!
            .filter { it.first.usesWordInAssembler || it.first.usesWordInAssembler == it.second.isWord }
            .toMap()
        this.addressingModeCandidates = addressingModeCandidates

        if (addressingModeCandidates.isEmpty())
            throw AssemblerException(line, "Addressing mode not found for $mnemonic $parameters.")


        // We can't know yet what candidate is, but we can know the bytes used, as THEY MUST USE the same bytes.
        // Right now it's impossible that one candidate uses one byte and other uses two bytes.
        return addressingModeCandidates.keys.find { true }!!.bytesUsed
    }

    fun assemble(file: NESAssemblerFile) {
        // First we fetch the addressing mode.
        val (addressingMode, value) = findAddressingMode(file)
        val assembler = file.assembler

        // Now we have to write the instruction! We start with the operation code.
        assembler.write(address, instruction.supportedAddressingModes[addressingMode]!!)

        // Now we can write the value associated to the instruction. The low part comes first!
        var data = value
        for (i in 1u..addressingMode.bytesUsed.toUInt()) {
            assembler.write((address + i).toUShort(), data.toUByte())
            data = data ushr 8
        }
    }

    private fun findAddressingMode(file: NESAssemblerFile): Pair<NESAddressingMode, Int> {
        // We have to find the addressing mode. We can do this now because we have all labels parsed.

        // If we already had a valid number result, return it.
        val validNumberResult = addressingMode
        if (validNumberResult != null) {
            return when (validNumberResult.first) {
                NESAddressingMode.IMPLIED -> Pair(NESAddressingMode.IMPLIED, 0)
                NESAddressingMode.RELATIVE -> Pair(
                    NESAddressingMode.RELATIVE,
                    validNumberResult.second.number!!.value - address.toInt() - NESAddressingMode.RELATIVE.bytesUsed - 1
                )
                else -> Pair(validNumberResult.first, validNumberResult.second.number!!.value)
            }
        }

        // Now we search for a candidate. If found, return.
        addressingModeCandidates!!.forEach {
            val finalValue = file.replaceAndEvaluate(it.value.label!!, it.value.invalidNumbers) ?: return@forEach

            return if (it.key == NESAddressingMode.RELATIVE) {
                val from = -address.toInt() - NESAddressingMode.RELATIVE.bytesUsed - 1
                Pair(it.key, finalValue + from)
            } else {
                Pair(it.key, finalValue)
            }
        }

        // No candidates found! Panic!
        throw AssemblerException(line, "Addressing mode not found for $mnemonic $parameters.")
    }
}