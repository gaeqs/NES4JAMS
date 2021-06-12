package io.github.gaeqs.nes4jams.cpu.directive.defaults

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerFile
import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpresionWithInvalids
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException

class NESDirectiveDw : NESDirective(NAME) {

    companion object {
        const val NAME = "dw"
    }

    override fun firstPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ): UShort {
        file.assembler.addMemory(parameters.size.toUInt() * 2u)
        return address
    }

    override fun secondPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ) {
        parameters.forEachIndexed { index, string ->
            val (value, invalids) = string.parseParameterExpresionWithInvalids()
            if (value == null) throw AssemblerException("Bad format: $string")
            val word = file.replaceAndEvaluate(string, invalids) ?: throw AssemblerException("Bad format: $string")

            file.assembler.write((address + index.toUInt() * 2u).toUShort(), word.toUByte())
            file.assembler.write((address + index.toUInt() * 2u + 1u).toUShort(), (word shr 8).toUByte())
        }
    }

}