package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.instruction.MatchResult
import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import io.github.gaeqs.nes4jams.utils.Value
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import kotlin.math.min

class NESInstructionSnapshot(val line: Int, val address: UShort, val raw: String, val original: String) {

    val mnemonic: String
    val parameters: String

    lateinit var instruction: NESInstruction
    var addressingMode: Pair<NESAddressingMode, Value>? = null
    var addressingModeCandidate: Pair<NESAddressingMode, MatchResult>? = null

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


    fun scan(file: NESAssemblerFile): Int {
        val instruction = NESInstruction.INSTRUCTIONS[mnemonic]
            ?: throw AssemblerException(line, "Instruction $mnemonic not found.")
        this.instruction = instruction


        // Search for the valid addressing modes. We want them sorted by the bytes used for later
        val addressingModes =
            NESAddressingMode.matchingAddressingModesFor(parameters, instruction.supportedAddressingModes.keys)
                .sortedBy { if (it.first.usesWordInAssembler == it.second.isWord) 0 else 1 }

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
            addressingMode = Pair(validNumberResult.first, validNumberResult.second.number!!)
            return validNumberResult.first.bytesUsed
        }

        // The instruction is using a label! The label may vary from addressing mode to addressing mode.
        // That's why we have to store all the valid addressing modes. We can now filter them using the
        // label snapshot.

        // Let's first try to replace the label now. This allows us to find the bytes used without doubts.
        // This can save us a byte if there's addressing modes with different byte sizes!
        // We can extrapolate one thing: the format of the addressing modes in the list ARE THE SAME!
        // That's because we only have to match ONE result.
        val first = addressingModes[0]
        val result = file.replaceAndEvaluate(first.second.label!!, first.second.invalidNumbers)
        if (result != null) {
            // RESULT FOUND! Now we can extrapolate one thing:
            // the format of the addressing modes in the list ARE THE SAME!
            // We just need to return the best match.

            val candidate = addressingModes.find { it.first.usesWordInAssembler == result.isWord }
            return if (candidate != null) {
                addressingMode = Pair(candidate.first, result)
                candidate.first.bytesUsed
            } else {
                // If the candidate is not found, return the first one
                val other = addressingModes[0]
                addressingMode = Pair(other.first, result)
                other.first.bytesUsed
            }
        }


        //val addressingModeCandidates = addressingModes
        //    // Check if the word type used by the addressing mode corresponds to the bytes used by the label
        //    // Instructions that use word can use bytes, but they have no priority!
        //    .filter { it.first.usesWordInAssembler || it.first.usesWordInAssembler == it.second.isWord }
        //    .toMap()
        this.addressingModeCandidate = addressingModes[0]

        //if (addressingModeCandidates.isEmpty())
        //    throw AssemblerException(line, "Addressing mode not found for $mnemonic $parameters.")


        // We can't know yet what candidate is, but we can know the bytes used, as THEY MUST USE the same bytes.
        // Right now it's impossible that one candidate uses one byte and other uses two bytes.
        return addressingModeCandidate!!.first.bytesUsed
    }

    fun assemble(file: NESAssemblerFile) {
        // First we fetch the addressing mode.
        val (addressingMode, value) = findAddressingMode(file)
        val assembler = file.assembler

        // Now we have to write the instruction! We start with the operation code.
        assembler.write(line, address, instruction.supportedAddressingModes[addressingMode]!!)

        // Now we can write the value associated to the instruction. The low part comes first!
        var data = value
        for (i in 1u..addressingMode.bytesUsed.toUInt()) {
            assembler.write(line, (address + i).toUShort(), data.toUByte())
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
                    validNumberResult.second.value - address.toInt() - NESAddressingMode.RELATIVE.bytesUsed - 1
                )
                else -> Pair(validNumberResult.first, validNumberResult.second.value)
            }
        }

        // Now we search for a candidate. If found, return.
        val it = addressingModeCandidate!!

        val finalValue =
            file.replaceAndEvaluate(it.second.label!!, it.second.invalidNumbers) ?: throw AssemblerException(
                line,
                "Addressing mode not found for $mnemonic $parameters. ($it)"
            )

        return if (it.first == NESAddressingMode.RELATIVE) {
            val from = -address.toInt() - NESAddressingMode.RELATIVE.bytesUsed - 1
            Pair(it.first, finalValue.value + from)
        } else {
            Pair(it.first, finalValue.value)
        }
    }
}