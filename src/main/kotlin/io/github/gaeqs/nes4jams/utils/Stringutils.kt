package io.github.gaeqs.nes4jams.utils

import java.util.*

fun String.toIntOldWay(): Int {
    val trimmed = trim()
    if (trimmed.isEmpty()) toInt()
    return when (trimmed[0]) {
        '$' -> trimmed.substring(1).toInt(16)
        '%' -> trimmed.substring(1).toInt(2)
        else -> trimmed.toInt()
    }
}

fun String.toIntOldWayOrNull(): Int? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    return when (trimmed[0]) {
        '$' -> trimmed.substring(1).toIntOrNull(16)
        '%' -> trimmed.substring(1).toIntOrNull(2)
        else -> trimmed.toIntOrNull()
    }
}

fun String.parseParameterExpresion(ignoreInvalidParameters: Boolean = false): Value? {
    return try {
        ParameterExpressionSolver(this, ignoreInvalidParameters).solve()
    } catch (ex: IllegalArgumentException) {
        null
    }
}

fun String.parseParameterExpresionWithInvalids(): Pair<Value?, Set<String>> {
    return try {
        val solver = ParameterExpressionSolver(this, true)
        Pair(solver.solve(), solver.ignoredInvalidNumbers)
    } catch (ex: IllegalArgumentException) {
        Pair(null, emptySet())
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

fun removeComments(string: String): String {
    var insideString = false
    var insideChar = false
    var escape = true
    for (i in string.indices) {
        val c = string[i]
        if (c == '"' && !escape) {
            insideString = !insideString
        }
        if (c == '\'' && !escape) {
            insideChar = !insideChar
        }
        if (c == ';' && !insideString && !insideChar) {
            return string.substring(0, i)
        }
        escape = !escape && c == '\\'
    }
    return string
}

private val illegalCharacters = Arrays.asList("\\", ";", "\"", "#", "'", "(", ")")


fun isLabelLegal(label: String): Boolean {
    if (label.isEmpty()) return false
    val str = label.filter { !it.isWhitespace() }.lowercase()
    if (str.startsWith(",x") || str.startsWith(",y")) return false

    //Special case: ':' is not allowed, but "::" is.
    var colon = -2
    do {
        colon = label.indexOf(':', colon + 2)
        if (colon == -1) break
        if (label.length <= colon + 1 || label[colon + 1] != ':') {
            return false
        }
    } while (label.length > colon + 2)
    return illegalCharacters.stream().noneMatch { label.contains(it) }
}