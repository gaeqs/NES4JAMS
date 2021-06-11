package io.github.gaeqs.nes4jams.cpu.directive

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerFile
import io.github.gaeqs.nes4jams.cpu.directive.defaults.NESDirectiveDb
import io.github.gaeqs.nes4jams.cpu.directive.defaults.NESDirectiveOrg

abstract class NESDirective(val mnemonic: String) {

    abstract fun firstPassExecute(
        file: NESAssemblerFile,
        lineNumber: Int,
        address: UShort,
        parameters: Array<String>
    ): UShort?

    abstract fun secondPassExecute(file: NESAssemblerFile, lineNumber: Int, address: UShort, parameters: Array<String>)


    companion object {

        val DIRECTIVES = mutableMapOf(
            NESDirectiveOrg.NAME.lowercase() to NESDirectiveOrg(),
            NESDirectiveDb.NAME.lowercase() to NESDirectiveDb()
        )

    }
}