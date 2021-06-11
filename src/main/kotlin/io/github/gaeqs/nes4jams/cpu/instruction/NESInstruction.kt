package io.github.gaeqs.nes4jams.cpu.instruction

class NESInstruction(val mnemonic: String, val supportedAddressingModes: Map<NESAddressingMode, UByte>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return mnemonic == (other as NESInstruction).mnemonic
    }

    override fun hashCode(): Int {
        return mnemonic.hashCode()
    }

    override fun toString(): String {
        return "OLC6502Instruction(mnemonic='$mnemonic')"
    }

    companion object {

        val INSTRUCTIONS: Map<String, NESInstruction>

        init {
            val temp = mutableMapOf<String, MutableMap<NESAddressingMode, UByte>>()
            NESAssembledInstruction.INSTRUCTIONS.forEachIndexed { index, assembled ->
                temp.computeIfAbsent(assembled.mnemonic) { mutableMapOf() } += assembled.addressingMode to index.toUByte()
            }
            INSTRUCTIONS = temp.map { it.key to NESInstruction(it.key, it.value) }.toMap()
        }

    }
}