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

package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.label.LabelReference
import io.github.gaeqs.nes4jams.util.Value

/**
 * Represents an equivalent inside a [NESAssembler].
 *
 * Equivalents links a key to an expression.
 * This expression can reference other equivalents.
 *
 * Equivalents have the following syntax: **key = expression**
 *
 * The key have the same restrictions as labels:
 * - The key cannot contain spaces.
 * - The key cannot end with **,x** or **,y**.
 * - The key cannot contain these characters: **\ ; " # ' ( )**
 * - The key cannot contain the character **:** alone. It may contain the string **::**
 * - The key cannot be 'A' or 'a'. These are reserved names.
 * - Keys may contain commas, but this is not recommended because equivalents with commas cannot be used in directives.
 *
 * Equivalents starting with **_** are considered local equivalents and can only be used in the file that declares them.
 *
 * There can't be a label and an equivalent with the same name.
 */
class NESAssemblerEquivalent(
    val line: NESAssemblerLine,
    val key: String,
    val rawValue: String,
    var value: Value? = null
) {

    /**
     * Whether this equivalent is a global equivalent.
     */
    fun isGlobal() = key[0] != '_'

    /**
     * Evaluates this equivalent expression with the current information in the assembler.
     * If the value is already defined, this method does nothing and returns true.
     *
     * If the value cannot be evaluated yet, this method returns false.
     *
     * @return whether the expression has been successfully evaluated.
     */
    fun evaluateValue(): Boolean {
        if (value != null) return true
        val result = line.file.evaluate(rawValue, alreadySearched = setOf(key))
        if (result.value == null) return false

        value = result.value

        // Add references!
        result.usedLabels.forEach { (label, deep) ->
            if (deep == 0) {
                label.references += LabelReference(null, line.file.name, line.index)
            }
        }

        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NESAssemblerEquivalent

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}