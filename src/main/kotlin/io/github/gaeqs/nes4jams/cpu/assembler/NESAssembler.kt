package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.label.OLC6502Label
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException

class NESAssembler(rawFiles: Map<String, String>) {

    val files = rawFiles.map { NESAssemblerFile(it.key, it.value, this) }
    val globalLabels = mutableMapOf<String, OLC6502Label>()

    var assembled = false
        private set

    private lateinit var assembledData: NESAssembledRawData

    var memoryPointer: UShort = 0u

    fun addMemory(value: UInt) {
        memoryPointer = (memoryPointer + value).toUShort()
    }

    fun addGlobalLabel(line: Int, key: String, address: UShort, originFile: String, originLine: Int): OLC6502Label {
        if (globalLabels.containsKey(key)) {
            throw AssemblerException(line, "The global label $key is already defined.")
        }

        if (files.any { it.labels.containsKey(key) }) {
            throw AssemblerException(
                line,
                "The label $key cannot be converted to a global label because there are two or more files with the same label."
            )
        }

        val label = OLC6502Label(key, address, originFile, originLine, true)
        globalLabels[key] = label
        return label
    }

    fun write(address: UShort, data: UByte) {
        assembledData.write(address, data)
    }

    fun assemble(start: UShort, size: Int): NESAssembledRawData {
        if (assembled) throw AssemblerException("The code was already assembled.")
        assembledData = NESAssembledRawData(start, size)

        files.forEach { it.scan() }
        files.forEach {
            it.assembleInstructions()
            it.executeDirectivesSecondPass()
        }

        assembled = true

        return assembledData
    }

}