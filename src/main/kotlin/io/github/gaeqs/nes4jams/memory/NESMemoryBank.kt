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

package io.github.gaeqs.nes4jams.memory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NESMemoryBank(
    @SerialName("bank_start") val start: UShort,
    @SerialName("bank_size") val size: UShort,
    @SerialName("writable") val writable: Boolean,
    @SerialName("write_on_cartridge") val writeOnCartridge: Boolean
)

class NESMemoryBankCollection(raw: Iterable<NESMemoryBank> = emptyList()) : List<NESMemoryBank> {

    private val list = raw.toList()

    constructor(vararg values: NESMemoryBank) : this(values.toList())

    override val size: Int = list.size
    override fun contains(element: NESMemoryBank) = element in list
    override fun containsAll(elements: Collection<NESMemoryBank>) = list.containsAll(elements)
    override fun get(index: Int) = list[index]
    override fun indexOf(element: NESMemoryBank) = list.indexOf(element)
    override fun isEmpty() = list.isEmpty()
    override fun iterator() = list.iterator()
    override fun lastIndexOf(element: NESMemoryBank) = list.lastIndexOf(element)
    override fun listIterator() = list.listIterator()
    override fun listIterator(index: Int) = list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = list.subList(fromIndex, toIndex)

    operator fun plus(bank: NESMemoryBank) = NESMemoryBankCollection(list + bank)
    override fun toString(): String {
        return "NESMemoryBankCollection$list"
    }


}