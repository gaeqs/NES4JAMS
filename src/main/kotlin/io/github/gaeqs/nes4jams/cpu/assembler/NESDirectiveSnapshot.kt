package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import net.jamsimulator.jams.utils.StringUtils
import kotlin.math.min

class NESDirectiveSnapshot(val line: Int, var address: UShort, val raw: String) {

    val mnemonic: String
    var parameters: Array<String>

    var directive: NESDirective? = null

    init {
        var mnemonicIndex = raw.indexOf(' ')
        val tabIndex = raw.indexOf("\t")
        if (mnemonicIndex == -1) mnemonicIndex = tabIndex
        else if (tabIndex != -1) mnemonicIndex = min(mnemonicIndex, tabIndex)

        if (mnemonicIndex == -1) {
            mnemonic = raw.substring(1)
            parameters = emptyArray()
        } else {
            mnemonic = raw.substring(1, mnemonicIndex)
            val raw = raw.substring(mnemonicIndex + 1)
            parameters = StringUtils.multiSplitIgnoreInsideString(raw, false, " ", ",", "\t")
                .toTypedArray()
        }
    }

    fun scan() {
        directive = NESDirective.DIRECTIVES[mnemonic.lowercase()]
        if (directive == null) {
            println("WARNING! Couldn't find directive .$mnemonic!")
        }
    }

    fun executeFirstPass(file: NESAssemblerFile): UShort? {
        if (directive != null) {
            val result = directive?.firstPassExecute(file, line, address, parameters)

            // If the first pass returns a new address, assign it to the address of the directive itself!
            if (result != null) address = result;
            return result
        }

        return null
    }

    fun executeSecondPass(file: NESAssemblerFile) {
        if (directive != null) {
            directive?.secondPassExecute(file, line, address, parameters)
        }
    }
}