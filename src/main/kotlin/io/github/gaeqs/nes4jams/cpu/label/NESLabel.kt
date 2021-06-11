package io.github.gaeqs.nes4jams.cpu.label

data class LabelReference(val address: UShort, val originFile: String, val originLine: Int)

class OLC6502Label(
    val key: String,
    val address: UShort,
    val originFile: String,
    val originLine: Int,
    val global: Boolean,
    val references: MutableSet<LabelReference> = mutableSetOf()
) {

    fun copyAsGlobal(): OLC6502Label {
        return OLC6502Label(key, address, originFile, originLine, true, references.toMutableSet())
    }

}