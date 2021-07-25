package io.github.gaeqs.nes4jams.audio.utilities

import io.github.gaeqs.nes4jams.audio.NESAPU
import io.github.gaeqs.nes4jams.util.extension.isZero
import io.github.gaeqs.nes4jams.util.extension.shr
import kotlin.math.max
import kotlin.math.min

class DMC(private val apu: NESAPU) {

    var rate = 0x36
    var position = 0
    var shiftRegister: UByte = 0u
    var buffer: UByte = 0u
    var sampleLength = 1
    var samplesLeft = 0
    var startAddress: UShort = 0xC000u
    var address = startAddress
    var bitsLeft = 8

    var silence = true
    var irq = false
    var loop = false
    var bufferEmpty = true
    var interrupt = false

    var value = 0

    fun clock() {
        if (bufferEmpty && samplesLeft > 0) fillBuffer()

        position = (position + 1) % rate
        if (position != 0) return

        if (bitsLeft <= 0) {
            bitsLeft = 8
            silence = bufferEmpty
            if (!silence) {
                shiftRegister = buffer
                bufferEmpty = true
            }
        }

        if (!silence) {
            value = min(max(value + if (shiftRegister and 0x1u >= 0u) 2 else -2, 0), 0x7F)
            shiftRegister = shiftRegister shr 1
            bitsLeft--
        }
    }


    private fun fillBuffer() {
        if (samplesLeft > 0) {
            buffer = apu.simulation.cpuRead(address++, false) ?: 0u
            bufferEmpty = false
            apu.simulation.stealCycles(12)

            if (address.isZero()) address = 0x8000u

            if (--samplesLeft == 0) {
                if (loop) restart()
                else if (irq && !interrupt) {
                    apu.simulation.cpu.interruptRequest()
                    interrupt = true
                }
            }
        } else {
            silence = true
        }
    }

    fun restart() {
        address = startAddress
        samplesLeft = sampleLength
        silence = false
    }

    fun reset() {
        rate = 0x36
        position = 0
        shiftRegister = 0u
        buffer = 0u
        sampleLength = 1
        samplesLeft = 0
        startAddress = 0xC000u
        address = startAddress
        bitsLeft = 8
        silence = true
        irq = false
        loop = false
        bufferEmpty = true
        interrupt = false
    }

}