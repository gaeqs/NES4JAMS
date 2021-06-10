package io.github.gaeqs.nes4jams.utils


fun UByte.isZero(): Boolean {
    return this <= 0u;
}


fun UShort.isZero(): Boolean {
    return this <= 0u;
}


fun UByte.flip(): UByte {
    var aux = this
    aux = (aux and 0xF0u shr 4) or (aux and 0x0Fu shl 4)
    aux = (aux and 0xCCu shr 2) or (aux and 0x33u shl 2)
    aux = (aux and 0xAAu shr 1) or (aux and 0x55u shl 1)
    return aux
}


infix fun UByte.shl(other: Number): UByte {
    return (this.toUInt() shl other.toInt()).toUByte()
}


infix fun UByte.shr(other: Number): UByte {
    return (this.toUInt() shr other.toInt()).toUByte()
}


infix fun UByte.rol(other: Number): UByte {
    val i = toUInt()
    return ((i shl other) or (i shr (8 - other.toInt()))).toUByte()
}


infix fun UByte.ror(other: Number): UByte {
    val i = toUInt()
    return ((i shr other) or (i shl (8 - other.toInt()))).toUByte()
}


infix fun UShort.shl(other: Number): UShort {
    return (this.toUInt() shl other.toInt()).toUShort()
}


infix fun UShort.shr(other: Number): UShort {
    return (this.toUInt() shr other.toInt()).toUShort()
}


infix fun UShort.rol(other: Number): UShort {
    val i = toUInt()
    return ((i shl other) or (i shr (16 - other.toInt()))).toUShort()
}


infix fun UShort.ror(other: Number): UShort {
    val i = toUInt()
    return ((i shr other) or (i shl (16 - other.toInt()))).toUShort()
}


infix fun UInt.shl(other: Number): UInt {
    return this shl other.toInt()
}


infix fun UInt.shr(other: Number): UInt {
    return this shr other.toInt()
}


infix fun UInt.rol(other: Number): UInt {
    val i = toUInt()
    return ((i shl other) or (i shr (32 - other.toInt())))
}


infix fun UInt.ror(other: Number): UInt {
    val i = toUInt()
    return ((i shr other) or (i shl (32 - other.toInt())))
}


infix fun UByte.concatenate(other: UByte): UShort {
    return (this.toUShort() shl 8) or other.toUShort()
}


infix fun Boolean.concatenate(other: Boolean): UByte {
    return when {
        !this && !other -> 0u
        !this && other -> 1u
        this && !other -> 2u
        else -> 3u
    }
}