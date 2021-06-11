package io.github.gaeqs.nes4jams.cpu.directive.defaults

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerFile
import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.utils.parseParameterExpresion
import io.github.gaeqs.nes4jams.utils.parseParameterExpresionWithInvalids
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException

class NESDirectiveDb : NESDirective(NAME) {

    companion object {
        const val NAME = "db"
    }

    override fun firstPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ): UShort {
        file.assembler.addMemory(parameters.size.toUInt())
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

            val result = file.replaceEquivalentsAndLabels(string, invalids)
            val finalValue = result.parseParameterExpresion() ?: throw AssemblerException("Bad format: $string")
            file.assembler.write((address + index.toUInt()).toUShort(), finalValue.value.toUByte())
        }
    }

}