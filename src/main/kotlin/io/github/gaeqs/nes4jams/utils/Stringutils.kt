package io.github.gaeqs.nes4jams.utils

fun String.toIntOldWay(): Int {
    val trimmed = trim()
    if (trimmed.isEmpty()) toInt()
    return when (trimmed[0]) {
        '$' -> trimmed.substring(1).toInt(16)
        '%' -> trimmed.substring(1).toInt(2)
        else -> this.substring(1).toInt()
    }
}

fun String.toIntOldWayOrNull(): Int? {
    val trimmed = trim();
    if (trimmed.isEmpty()) return null
    return when (trimmed[0]) {
        '$' -> trimmed.substring(1).toIntOrNull(16)
        '%' -> trimmed.substring(1).toIntOrNull(2)
        else -> trimmed.substring(1).toIntOrNull()
    }
}

infix fun UByte.toHex(minSize: Int): String {
    var value = this.toString(16)
    while (value.length < minSize) value = "0${value}"
    return value;
}

infix fun UShort.toHex(minSize: Int): String {
    var value = this.toString(16)
    while (value.length < minSize) value = "0${value}"
    return value;
}

infix fun UInt.toHex(minSize: Int): String {
    var value = this.toString(16)
    while (value.length < minSize) value = "0${value}"
    return value;
}