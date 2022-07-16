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

package io.github.gaeqs.nes4jams.cartridge

import io.github.gaeqs.nes4jams.cartridge.mapper.Mapper
import io.github.gaeqs.nes4jams.cartridge.mapper.MapperBuilderManager
import io.github.gaeqs.nes4jams.ppu.Mirror
import io.github.gaeqs.nes4jams.util.extension.orNull
import java.io.File
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.min

class Cartridge(stream: InputStream, val name: String, closeStream: Boolean = false, defaultMapper: Int? = null) {

    var header: CartridgeHeader

    val prgMemory: UByteArray
    val chrMemory: UByteArray
    val prgBanks: Int
    val chrBanks: Int

    var mapper: Mapper
        private set

    var mirroring: Mirror
        get() {
            val mirror = mapper.mirroring
            return if (mirror == Mirror.HARDWARE) field else mirror
        }
        private set

    constructor(file: File, defaultMapper: Int? = null) : this(file.inputStream(), file.name, true, defaultMapper)

    init {
        header = CartridgeHeader(stream)

        val data = stream.readAllBytes()
        if (closeStream) {
            stream.close()
        }

        val prgSize = header.prgRomSize.toInt()
        val chrSize = header.chrRomSize.toInt()
        prgMemory = UByteArray(if (prgSize == 0) 0x4000 else header.prgRomSize.toInt())
        chrMemory = UByteArray(if (chrSize == 0) 0x2000 else header.chrRomSize.toInt())

        data.copyInto(
            prgMemory.asByteArray(), 0, 0, min(prgMemory.size, data.size)
        )
        data.copyInto(
            chrMemory.asByteArray(), 0, prgMemory.size,
            min(prgMemory.size + chrMemory.size, data.size)
        )

        prgBanks = ceil(prgSize.toDouble() / 0x4000).toInt()
        chrBanks = ceil(chrSize.toDouble() / 0x2000).toInt()

        mirroring = header.mirroring

        mapper = MapperBuilderManager.INSTANCE[header.mapper.toString()].orNull()?.build(this)
            ?: run {
                if (defaultMapper == null) {
                    throw NoSuchElementException("Couldn't find mapper ${header.mapper}!")
                }
                val default = MapperBuilderManager.INSTANCE[defaultMapper.toString()].orNull()
                if (default == null) {
                    throw NoSuchElementException("Couldn't find default mapper ${default}!")
                }
                default.build(this)
            }
    }

    fun refreshHeader(defaultMapper: Int? = null) {
        mirroring = header.mirroring
        mapper = MapperBuilderManager.INSTANCE[header.mapper.toString()].orNull()?.build(this)
            ?: run {
                if (defaultMapper == null) {
                    throw NoSuchElementException("Couldn't find mapper ${header.mapper}!")
                }
                val default = MapperBuilderManager.INSTANCE[header.mapper.toString()].orNull()
                if (default == null) {
                    throw NoSuchElementException("Couldn't find default mapper ${default}!")
                }
                default.build(this)
            }
    }

    fun cpuRead(address: UShort): Pair<Boolean, UByte> {
        val result = mapper.cpuMapRead(address)
        return when {
            result.isIntrinsic -> Pair(true, result.intrinsicValue)
            result.isInArray -> Pair(true, prgMemory[result.arrayAddress])
            else -> Pair(false, 0u)
        }
    }

    fun cpuWrite(address: UShort, data: UByte): Boolean {
        val result = mapper.cpuMapWrite(address, data)
        return when {
            result.isIntrinsic -> true
            result.isInArray -> {
                prgMemory[result.arrayAddress] = data
                true
            }
            else -> false
        }
    }

    fun ppuRead(address: UShort): Pair<Boolean, UByte> {
        val result = mapper.ppuMapRead(address)
        return when {
            result.isIntrinsic -> Pair(true, result.intrinsicValue)
            result.isInArray -> Pair(true, chrMemory[result.arrayAddress])
            else -> Pair(false, 0u)
        }
    }

    fun ppuWrite(address: UShort, data: UByte): Boolean {
        val result = mapper.ppuMapWrite(address, data)
        return when {
            result.isIntrinsic -> true
            result.isInArray -> {
                chrMemory[result.arrayAddress] = data
                true
            }
            else -> false
        }
    }

    fun reset() {
        mapper.reset()
    }


}