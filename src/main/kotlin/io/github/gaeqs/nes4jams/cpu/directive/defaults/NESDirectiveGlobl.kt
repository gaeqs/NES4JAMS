package io.github.gaeqs.nes4jams.cpu.directive.defaults

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerFile
import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.utils.extension.isLabelLegal
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException

class NESDirectiveGlobl : NESDirective(NAME) {

    companion object {
        const val NAME = "globl"
    }

    override fun firstPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ): UShort {
        if (parameters.isEmpty()) throw AssemblerException(lineNumber, ".$NAME must have at least one parameter.")

        for (parameter in parameters) {
            if (!parameter.isLabelLegal()) throw AssemblerException("Illegal label $parameter.")
        }

        for (parameter in parameters) {
            file.setAsGlobalLabel(lineNumber, parameter)
        }

        return address
    }

    override fun secondPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ) {
    }

}