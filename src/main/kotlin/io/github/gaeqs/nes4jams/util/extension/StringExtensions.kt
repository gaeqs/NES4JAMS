/*
 *  MIT License
 *
 *  Copyright (c) 2021 Gael Rial Costas
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.gaeqs.nes4jams.util.extension

import io.github.gaeqs.nes4jams.util.ParameterExpressionSolver
import io.github.gaeqs.nes4jams.util.Value

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
        '@' -> trimmed.substring(1).toIntOrNull(8)
        else -> trimmed.toIntOrNull()
    }
}

fun String.parseParameterExpression(ignoreInvalidParameters: Boolean = false): Value? {
    return try {
        ParameterExpressionSolver(this, ignoreInvalidParameters).solve()
    } catch (ex: IllegalArgumentException) {
        null
    }
}

fun String.parseParameterExpressionWithInvalids(): Pair<Value?, Set<String>> {
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
    return value
}

infix fun UShort.toHex(minSize: Int): String {
    var value = this.toString(16)
    while (value.length < minSize) value = "0${value}"
    return value
}

infix fun UInt.toHex(minSize: Int): String {
    var value = this.toString(16)
    while (value.length < minSize) value = "0${value}"
    return value
}

fun String.removeComments(): String {
    var insideString = false
    var insideChar = false
    var escape = true
    for (i in indices) {
        val c = this[i]
        if (c == '"' && !escape) {
            insideString = !insideString
        }
        if (c == '\'' && !escape) {
            insideChar = !insideChar
        }
        if (c == ';' && !insideString && !insideChar) {
            return substring(0, i)
        }
        escape = !escape && c == '\\'
    }
    return this
}

fun String.getCommentIndex(): Int {
    var c: Char
    var insideString = false
    var insideChar = false
    var escape = true
    for (i in indices) {
        c = this[i]
        if (c == '"' && !escape) insideString = !insideString
        if (c == '\'' && !escape) insideChar = !insideChar
        if ((c == ';') && !insideString && !insideChar) return i
        escape = !escape && c == '\\'
    }
    return -1
}

private val illegalCharacters = listOf("\\", ";", "\"", "#", "'", "(", ")")

fun String.isLabelLegal(): Boolean {
    if (isEmpty()) return false
    if (' ' in this) return false
    val str = lowercase().filter { !it.isWhitespace() }
    if (str.endsWith(",x") || str.endsWith(",y") || str == "a") return false


    //Special case: ':' is not allowed, but "::" is.
    var colon = -2
    do {
        colon = indexOf(':', colon + 2)
        if (colon == -1) break
        if (length <= colon + 1 || this[colon + 1] != ':') {
            return false
        }
    } while (length > colon + 2)
    return illegalCharacters.stream().noneMatch { contains(it) }
}

fun String.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    val regex = if (ignoreCase) Regex(substr, RegexOption.IGNORE_CASE) else Regex(substr)
    return regex.findAll(this).map { it.range.first }.toList()
}