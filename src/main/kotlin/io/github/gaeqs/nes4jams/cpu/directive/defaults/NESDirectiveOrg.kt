package io.github.gaeqs.nes4jams.cpu.directive.defaults

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerFile
import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.utils.toIntOldWayOrNull
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException

class NESDirectiveOrg : NESDirective(NAME) {

    companion object {
        const val NAME = "org"
    }

    override fun firstPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ): UShort {
        if (parameters.size != 1) throw AssemblerException(lineNumber, ".$NAME directive must have one parameter!")
        val number = parameters[0].toIntOldWayOrNull() ?: throw AssemblerException(
            lineNumber,
            ".$NAME first parameter must be a number between 0 !"
        )
        val target = number.toUShort()
        file.assembler.memoryPointer = target
        return target
    }

    override fun secondPassExecute(file: NESAssemblerFile, lineNumber: Int, address: UShort, parameters: Array<String>) {
    }

}