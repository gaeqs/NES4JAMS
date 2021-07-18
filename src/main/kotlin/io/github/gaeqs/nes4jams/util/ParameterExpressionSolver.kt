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

package io.github.gaeqs.nes4jams.util

import io.github.gaeqs.nes4jams.data.BYTE_RANGE
import io.github.gaeqs.nes4jams.util.extension.toIntOldWayOrNull

data class Value(val value: Int, val isWord: Boolean)

class UnaryOperation(val mnemonic: String, val applyAtEnd: Boolean, val operation: (Value) -> Value)
class BinaryOperation(val mnemonic: String, val operation: (Value, Value) -> Value)

/**
 * This class solves mathematical expressions. Just enter the expression and use solve() to get the result!
 * If "ignoreInvalidNumbers" is true, invalid numbers are ignored and converted to 0. This is useful when
 * you want to know if the result would be a byte or a word and you don't know yet the labels' values
 */
class ParameterExpressionSolver(val data: String, ignoreInvalidNumbers: Boolean = false) {

    val ignoredInvalidNumbers: Set<String> = mutableSetOf()

    private val binaryOperation: BinaryOperation?
    private val node1: ParameterExpressionSolver?
    private val node2: ParameterExpressionSolver?
    private val unaryOperation: UnaryOperation?
    private val value: Value?

    init {
        // First we check if the data is empty.
        if (data.isBlank()) throw IllegalArgumentException("Invalid expression. Data is empty.")

        var current = data.filter { !it.isWhitespace() }
        // First we have to remove the outside parenthesis.
        while (current[0] == '(' && current[current.length - 1] == ')') {
            current = current.substring(1, current.length - 1)
        }

        // Now we try to parse the number itself. If we success, we're done!
        val value = current.toIntOldWayOrNull()
        if (value != null) {
            this.value = Value(value, !BYTE_RANGE.contains(value))
            node1 = null
            node2 = null
            binaryOperation = null
            unaryOperation = null
        } else {
            // Now we check if the data is empty again.
            if (current.isBlank()) throw IllegalArgumentException("Invalid expression. Data is empty.")

            // Now we have to find out potential action range.
            val ranges = findActionRanges(current)
            val (binaryOperation, breakPoint) = findBreakPoint(current, ranges)
            this.binaryOperation = binaryOperation

            if (binaryOperation != null) {
                // Binary operation found!
                node1 = ParameterExpressionSolver(current.substring(0, breakPoint), ignoreInvalidNumbers)
                node2 = ParameterExpressionSolver(
                    current.substring(breakPoint + binaryOperation.mnemonic.length),
                    ignoreInvalidNumbers
                )
                (ignoredInvalidNumbers as MutableSet<String>) += node1.ignoredInvalidNumbers
                ignoredInvalidNumbers += node2.ignoredInvalidNumbers
                unaryOperation = null
                this.value = null
            } else {
                node2 = null

                // Search for a unary operation
                unaryOperation = findUnary(current)

                if (unaryOperation != null) {
                    this.value = null
                    val child =
                        if (unaryOperation.applyAtEnd) current.substring(
                            0,
                            current.length - unaryOperation.mnemonic.length
                        )
                        else current.substring(unaryOperation.mnemonic.length)
                    node1 = ParameterExpressionSolver(child, ignoreInvalidNumbers)
                    (ignoredInvalidNumbers as MutableSet<String>) += node1.ignoredInvalidNumbers
                } else {
                    if (ignoreInvalidNumbers) {
                        this.value = Value(0, true)
                        node1 = null
                        (ignoredInvalidNumbers as MutableSet<String>) += current
                    } else {
                        throw IllegalArgumentException("Invalid expression. Invalid number.")
                    }
                }
            }
        }
    }

    /**
     * Solves the expression and returns its result.
     */
    fun solve(): Value {
        if (value != null) return value
        if (unaryOperation != null) return unaryOperation.operation(node1!!.solve())
        return binaryOperation!!.operation(node1!!.solve(), node2!!.solve())
    }

    private fun findActionRanges(current: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var parenthesisCounter = 0
        var index = current.length - 1

        // We need to calculate the ranges of the string without parenthesis.
        for (i in current.length - 1 downTo 0) {
            when {
                current[i] == ')' -> {
                    if (parenthesisCounter == 0) {
                        // It is valid when a expression finishes with a parenthesis!
                        if (i != current.length - 1) {
                            if (index == i) {
                                throw IllegalArgumentException("Invalid expression. Bad parenthesis format. $current")
                            }
                            ranges += IntRange(i + 1, index)
                        }
                    }
                    parenthesisCounter++
                }
                current[i] == '(' -> {
                    if (parenthesisCounter == 0) {
                        throw IllegalArgumentException("Invalid expression. Bad parenthesis format. $current")
                    }
                    parenthesisCounter--
                    if (parenthesisCounter == 0) {
                        index = i - 1
                    }
                }
            }
        }


        if (parenthesisCounter != 0) {
            throw IllegalArgumentException("Invalid expression. Bad parenthesis format. $current")
        }

        if (index >= 0) {
            ranges += IntRange(0, index)
        }


        return ranges
    }

    private fun findBreakPoint(current: String, ranges: List<IntRange>): Pair<BinaryOperation?, Int> {
        // We need to check if we have a binary operator using the order of operations.
        // In order, we check the operators from the end to the start of the string.
        // If two operators are present and have the same priority, we have to get the last one.

        for (operations in BINARY_OPERATORS) {
            for (range in ranges) {
                val stringInRange = current.substring(range)
                var lastIndex = -1
                var last: BinaryOperation? = null

                operations.forEach {
                    val index = stringInRange.indexOf(it.mnemonic)
                    if (index > lastIndex) {
                        lastIndex = index
                        last = it
                    }
                }

                if (last != null) {
                    return Pair(last, lastIndex + range.first)
                }
            }
        }
        return Pair(null, 0)
    }

    private fun findUnary(current: String): UnaryOperation? {
        // We now we have no parenthesis and no whitespaces.
        // We can just check if the operator at the start or at the end of the string!
        UNARY_OPERATORS.forEach {
            if (if (it.applyAtEnd) current.endsWith(it.mnemonic) else current.startsWith(it.mnemonic)) {
                return it
            }
        }
        return null
    }

    companion object {
        val UNARY_OPERATORS = setOf(
            UnaryOperation("<", false) { Value(it.value and 0xFF, false) },
            UnaryOperation(">", false) { Value((it.value shr 8) and 0xFF, false) },
            UnaryOperation(".b", true) { Value(it.value and 0xFF, false) },
            UnaryOperation(".w", true) { Value(it.value, true) },
            UnaryOperation("~", false) { Value(it.value.inv(), it.isWord) }
        )

        /**
         * This list is sorted using the order of operations, from least significant to most significant.
         */
        val BINARY_OPERATORS = listOf(
            setOf(
                BinaryOperation("|") { a, b -> Value(a.value or b.value, a.isWord || b.isWord) },
                //BinaryOperation("OR") { a, b -> Value(a.value or b.value, a.isWord || b.isWord) },
            ),
            setOf(
                BinaryOperation("^") { a, b -> Value(a.value xor b.value, a.isWord || b.isWord) },
                //BinaryOperation("XOR") { a, b -> Value(a.value xor b.value, a.isWord || b.isWord) },
            ),
            setOf(
                BinaryOperation("&") { a, b -> Value(a.value and b.value, a.isWord || b.isWord) },
                //BinaryOperation("AND") { a, b -> Value(a.value and b.value, a.isWord || b.isWord) },
            ),
            setOf(
                BinaryOperation("<<") { a, b -> Value(a.value shl b.value, a.isWord || b.isWord) },
                //BinaryOperation("SHL") { a, b -> Value(a.value shl b.value, a.isWord || b.isWord) },
                BinaryOperation(">>") { a, b -> Value(a.value shr b.value, a.isWord || b.isWord) },
                //BinaryOperation("SHR") { a, b -> Value(a.value shr b.value, a.isWord || b.isWord) },
            ),
            setOf(
                BinaryOperation("+") { a, b -> Value(a.value + b.value, a.isWord || b.isWord) },
                BinaryOperation("-") { a, b -> Value(a.value - b.value, a.isWord || b.isWord) },
            ),

            setOf(
                BinaryOperation("*") { a, b -> Value(a.value * b.value, a.isWord || b.isWord) },
                BinaryOperation("/") { a, b -> Value(if(b.value == 0) 0 else a.value / b.value, a.isWord || b.isWord) },
                BinaryOperation("%") { a, b -> Value(a.value % b.value, a.isWord || b.isWord) },
            )
        )
    }

}