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

import io.github.gaeqs.nes4jams.cpu.label.NESLabel
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.util.extension.toHex
import net.jamsimulator.jams.gui.util.log.Log
import net.jamsimulator.jams.mips.assembler.Macro
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.utils.RawFileData
import kotlin.system.measureTimeMillis

/**
 * # Gael Rial Costas's 6502 assembler.
 *
 * This assembler assembles the given code in four steps.
 * - **Step 1**: metadata scanning. In this step lines are split into their primitive elements.
 * Labels and equivalents are registered without its value.
 * - **Step 2**: macro execution. Macros are invoked and their result is placed in the corresponding position.
 * Added lines execute the step 1.
 * - **Step 3**: address assignation: addresses are assigned to labels, instructions and directives.
 * - **Step 4**: values assignation: in this step the values of labels and equivalents are known.
 * Instructions and directives write their corresponding values into memory.
 *
 * ## Labels and equivalents
 *
 * There are several limitations to the names of labels and equivalents:
 * - The name cannot contain spaces.
 * - The name cannot end with **,x** or **,y**.
 * - The name cannot contain these characters: **\ ; " # ' ( )**
 * - The name cannot contain the character **:** alone. It may contain the string **::**
 * - The name cannot be 'A' or 'a'. These are reserved names.
 * - Names may contain commas, but this is not recommended because labels and
 * equivalents with commas cannot be used in directives.
 *
 * Equivalents and labels starting with **_** are considered locals
 * and can only be used in the file that declares them.
 *
 * To declare an equivalent, use the next syntax: **key = expression**
 *
 * To declare a label, type the name and a **:** at the start of the line: **key:**
 *
 * You can declare an instruction, directive or macro call after a label.
 *
 * There can't be a label and an equivalent with the same name.
 *
 * ## Macros
 * To define a macro use the directives **.macro** and **.endmacro**.
 * The code between these directives will be transformed into a macro.
 *
 * Labels declared inside a macro will have the name concatenated to a suffix.
 * This suffix will change on every macro call and has the next syntax: '_MX', where X is a number.
 *
 * The **.macro** directive must have the next syntax: **.macro name (%param1, %other, %another)**
 *
 * The name of a macro cannot collide with the name of an instruction. The name of a macro can't start with a point.
 * Parameters inside a macro will be replaced on every macro call.
 *
 * To declare a macro call, use the next syntax: 'key (param1, param2, param3)'
 *
 * ## Expressions
 *
 * Expressions support the next operators:
 *
 * ### Unary operators:
 * - **<**: returns the lower byte of a word.
 * - **>**: returns the upper byte of a word.
 * - **.b**: defines the value as a byte. This has the same result as **<**.
 * - **.w**: defines the value as a word.
 * - **~**: inverts the bits of the value.
 *
 * ### Binary operators:
 * - **|**: bitwise or.
 * - **^**: bitwise xor.
 * - **&**: bitwise and.
 * - **<<**: shift left.
 * - **>>**: shift right.
 * - **+**: addition.
 * - **-**: subtraction.
 * - **`*`**: multiplication.
 * - **`/`**: division.
 * - **`%`**: module.
 *
 * Operators use the
 * [default order of operations](https://en.wikipedia.org/wiki/Order_of_operations#Programming_languages).
 *
 * Immediate values will be interpreted as a byte or as a word depending on it's size. If the number
 * can be represented in a byte, then the assembler will interpret it as a byte. Use the **.w** operator
 * if you want to explicitly declare it as a word.
 *
 * ## Memory banks:
 *
 * Data is stored in one or several memory banks. These banks has a start address and a size.
 * For example: the Super Mario Bros. game has one memory bank starting at the address 0x8000
 * with 32KB of memory (0x8000 bytes).
 *
 * Several banks can collide in their memory sections. This is useful if your game uses a special
 * mapper that changes between memory banks at runtime. (Super Mario Bros. 3 uses this technique.)
 *
 * The assembler also defines a special memory bank called 'data bank'. This bank represents addresses
 * outside the cartridge (0x0000 to 0x7FFF). You can declare labels and define memory spaces in this section,
 * but you cannot write any data.
 *
 * Use the directive **.bank** to move between banks. Use the directive **.data** to switch to the data bank.
 *
 * Banks have its own address pointer, so you don't have to manually set the current address when changing banks.
 *
 * @author Gael Rial Costas
 *
 * @constructor
 *
 * Creates a NES assembler.
 * @param rawFiles the raw files to assemble (Name -> Text)
 * @param bankBuilders the builders for the memory banks.
 * @param log the log or null. If present, information about the assembly will be written in this log.
 */
class NESAssembler(
    rawFiles: Iterable<RawFileData>,
    bankBuilders: Iterable<NESMemoryBank>,
    val log: Log?
) {

    /**
     * The files of this assembler. This list is immutable.
     */
    val files = rawFiles.map { NESAssemblerFile(it.file, it.data, this) }

    /**
     * The data bank of this assembler.
     */
    val dataBank = NESAssemblerMemoryBank(-1, 0u, 0x8000u, false, false)

    /**
     * The memory banks of this assembler. This list is immutable.
     */
    val banks = bankBuilders.mapIndexed { index, value -> NESAssemblerMemoryBank(index, value) }

    /**
     * The global equivalents of this assembler. This map (Name -> Equivalent) is mutable.
     */
    val globalEquivalents = mutableMapOf<String, NESAssemblerEquivalent>()

    /**
     * The global labels of this assembler. This map (Name -> Label) is mutable.
     */
    val globalLabels = mutableMapOf<String, NESLabel>()

    /**
     * The global macros of this assembler. This map (Name -> Macro) is mutable.
     */
    val globalMacros = mutableMapOf<String, Macro>()

    /**
     * The selected bank.
     */
    var selectedBank = banks[0]
        private set

    /**
     * Whether the code has been assembled.
     */
    var assembled = false
        private set

    /**
     * Adds a global label to the assembler.
     * @param line the line execution this operation.
     * @param label the label to add.
     */
    fun addGlobalLabel(line: Int, label: NESLabel) {
        if (label.key in globalLabels) {
            throw AssemblerException(line, "The global label ${label.key} is already defined.")
        }

        if (label.key in globalEquivalents) {
            throw AssemblerException(line, "The global label ${label.key} is already defined as an equivalent.")
        }

        globalLabels[label.key] = label
    }

    /**
     * Adds a global equivalent to the assembler.
     * @param line the line execution this operation.
     * @param equivalent the equivalent to add.
     */
    fun addGlobalEquivalent(line: Int, equivalent: NESAssemblerEquivalent) {
        if (equivalent.key in globalEquivalents) {
            throw AssemblerException(line, "The global equivalent ${equivalent.key} is already defined.")
        }

        if (equivalent.key in globalLabels) {
            throw AssemblerException(line, "The global equivalent ${equivalent.key} is already defined as a label.")
        }

        globalEquivalents[equivalent.key] = equivalent
    }

    /**
     * Adds a global macro to the assembler.
     * @param line the line execution this operation.
     * @param macro the macro to add.
     */
    fun addGlobalMacro(line: Int, macro: Macro) {
        if (macro.name in globalMacros) {
            throw AssemblerException(line, "The global macro ${macro.name} is already defined.")
        }

        globalMacros[macro.name] = macro
    }

    /**
     * Selects the given bank.
     * @param line the line execution this operation.
     * @throws AssemblerException when the bank is not found.
     */
    fun selectBank(line: Int, bank: Int) {
        if (bank < 0 || bank >= banks.size) {
            throw AssemblerException(line, "Bank $bank not found!")
        }
        selectedBank = banks[bank]
    }

    /**
     * Selects the data bank.
     */
    fun selectDataBank() {
        selectedBank = dataBank
    }

    /**
     * Writes the given value into the selected address at the given address.
     * @param line the line execution this operation.
     * @param address the address where the value will be stored at.
     * @param data the value to store.
     */
    fun write(line: Int, address: UShort, data: UByte) {
        try {
            selectedBank[address] = data
        } catch (ex: IndexOutOfBoundsException) {
            throw AssemblerException(line, "Out of bounds! ($${address.toHex(4)})", ex)
        }
    }

    /**
     * Assembles the code.
     *
     * @throws AssemblerException when the code was already assembled
     * or when the code contains errors and cannot be assembled.
     */
    fun assemble() {
        if (assembled) throw AssemblerException("The code was already assembled.")
        val duration = measureTimeMillis {
            log?.printInfoLn("Scanning metadata...")
            files.forEach { it.scanMetadata() }
            log?.printInfoLn("Executing macros...")
            files.forEach { it.executeMacros() }
            log?.printInfoLn("Assigning addresses...")
            files.forEach { it.assignAddresses() }
            log?.printInfoLn("Assigning values...")
            files.forEach { it.assignValues() }
        }
        log?.printDoneLn("${files.size} files assembled in $duration milliseconds.")
        assembled = true
    }


}