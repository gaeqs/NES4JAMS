package io.github.gaeqs.nes4jams.audio

import io.github.gaeqs.nes4jams.audio.timer.*
import io.github.gaeqs.nes4jams.audio.utilities.DMC
import io.github.gaeqs.nes4jams.audio.utilities.Filter
import io.github.gaeqs.nes4jams.audio.utilities.FrameCounter
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.util.BIT6
import io.github.gaeqs.nes4jams.util.BIT7
import io.github.gaeqs.nes4jams.util.extension.shr

class NESAPU(val simulation: NESSimulation, val sampleRate: Int, val soundFiltering: Boolean) {

    companion object {
        private val TND_LOOKUP = IntArray(203 * 2) { ((163.67 / (24329.0 / it + 100.0)) * 49151.0).toInt() }
        private val SQUARE_LOOKUP = IntArray(203 * 2) { ((95.52 / (8128.0 / it + 100.0)) * 49151.0).toInt() }
        private val DUTY_LOOKUP = arrayOf(
            intArrayOf(0, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 1, 1, 0, 0, 0, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 0, 0, 0),
            intArrayOf(1, 0, 0, 1, 1, 1, 1, 1)
        )
    }

    val tvType = simulation.cartridge.header.tvType

    val beeper = Beeper(sampleRate, tvType)

    var apuClocks = 0L
    var clocksAfterSample = 0L

    private val frameCounter = FrameCounter(tvType, this::clockFrameCounter)
    private val dmc = DMC(this)
    private val filter = Filter()

    private val cyclesPerSample = tvType.getAudioCyclesPerSample(sampleRate.toDouble())
    private val timers = arrayOf(
        SquareTimer(8, 2),
        SquareTimer(8, 2),
        TriangleTimer(),
        NoiseTimer()
    )

    private var interruptFlag = true

    private var linearCounterFlag = false
    private var linearCounter = 0
    private var linearCounterReload = 0

    private var accumulator = 0

    fun pause() {
        beeper.pause()
    }

    fun resume() {
        beeper.resume()
    }

    fun destroy() {
        beeper.destroy()
    }

    fun isRequestingInterrupt() = dmc.interrupt || frameCounter.interrupt

    /**
     * CPU bus communication
     */
    fun cpuWrite(address: UShort, data: UByte) {
        clockTo(simulation.clock)
        when (address.toUInt()) {
            0x4000u -> {
                val t = timers[0] as SquareTimer
                t.counterLengthHalt = (data and 0b00100000u) > 0u
                t.setDuty(DUTY_LOOKUP[data.toInt() shr 6])
                t.envelopeConstantVolume = (data and 0b00010000u) > 0u
                t.envelopeValue = (data and 0xFu).toInt()
            }
            0x4001u -> {
                val t = timers[0] as SquareTimer
                t.sweepEnabled = (data and 0b10000000u) > 0u
                t.sweepPeriod = (data shr 4).toInt() and 0x7
                t.sweepNegate = (data and 0b00001000u) > 0u
                t.sweepShift = (data and 0x7u).toInt()
                t.sweepReload = true
            }
            0x4002u -> {
                timers[0].period = (timers[0].period and 0xFE00) + (data.toInt() shl 1)
            }
            0x4003u -> {
                val t = timers[0] as SquareTimer
                if (t.counterLengthEnabled) {
                    t.counterLength = Timer.COUNTER_LENGTH_LOAD[data.toInt() shr 3]
                }
                t.period = (t.period and 0x1FF) + ((data and 0x7u).toInt() shl 9)
                t.reset()
                t.envelopeStartFlag = true
            }
            0x4004u -> {
                val t = timers[1] as SquareTimer
                t.counterLengthHalt = (data and 0b00100000u) > 0u
                t.setDuty(DUTY_LOOKUP[data.toInt() shr 6])
                t.envelopeConstantVolume = (data and 0b00010000u) > 0u
                t.envelopeValue = (data and 0xFu).toInt()
            }
            0x4005u -> {
                val t = timers[1] as SquareTimer
                t.sweepEnabled = (data and 0b10000000u) > 0u
                t.sweepPeriod = (data shr 4).toInt() and 0x7
                t.sweepNegate = (data and 0b00001000u) > 0u
                t.sweepShift = (data and 0x7u).toInt()
                t.sweepReload = true
            }
            0x4006u -> {
                timers[1].period = (timers[1].period and 0xFE00) + (data.toInt() shl 1)
            }
            0x4007u -> {
                val t = timers[1] as SquareTimer
                if (t.counterLengthEnabled) {
                    t.counterLength = Timer.COUNTER_LENGTH_LOAD[data.toInt() shr 3]
                }
                t.period = (t.period and 0x1FF) + ((data and 0x7u).toInt() shl 9)
                t.reset()
                t.envelopeStartFlag = true
            }
            0x4008u -> {
                linearCounterReload = (data and 0x7Fu).toInt()
                timers[2].counterLengthHalt = ((data and 0b10000000u) > 0u)
            }
            0x400Au -> {
                timers[2].period = (timers[2].period and 0xff00) + data.toInt()
            }
            0x400Bu -> {
                val t = timers[2] as TriangleTimer
                if (t.counterLengthEnabled) {
                    t.counterLength = Timer.COUNTER_LENGTH_LOAD[data.toInt() shr 3]
                }
                t.period = (t.period and 0xFF) + ((data and 0x7u).toInt() shl 8)
                linearCounterFlag = true
            }
            0x400Cu -> {
                val t = timers[3] as NoiseTimer
                t.counterLengthHalt = (data and 0b00100000u) > 0u
                t.envelopeConstantVolume = (data and 0b00010000u) > 0u
                t.envelopeValue = data.toInt() and 0xF
            }
            0x400Eu -> {
                timers[3].setDuty(if (data and 0b10000000u > 0u) 6 else 1)
                timers[3].period = tvType.audioNoisePeriod[data.toInt() and 0xF]
            }
            0x400Fu -> {
                if (timers[3].counterLengthEnabled) {
                    timers[3].counterLength = Timer.COUNTER_LENGTH_LOAD[data.toInt() shr 3]
                }
                timers[3].envelopeStartFlag = true
            }
            0x4010u -> {
                dmc.irq = data and 0b10000000u > 0u
                dmc.loop = data and 0b01000000u > 0u
                dmc.rate = tvType.audioDMCPeriods[data.toInt() and 0xF]
                if (!dmc.irq && dmc.interrupt) {
                    dmc.interrupt = false
                }
            }
            0x4011u -> {
                dmc.value = data.toInt() and 0x7F
            }
            0x4012u -> {
                dmc.startAddress = ((data.toInt() shl 7) + 0xC000).toUShort()
            }
            0x4013u -> {
                dmc.sampleLength = (data.toInt() shl 4) + 1
            }
            0x4015u -> {
                timers.forEachIndexed { i, timer ->
                    timer.counterLengthEnabled = data and (1u shl i).toUByte() > 0u
                    if (!timer.counterLengthEnabled) timer.counterLength = 0
                }
                if (data and 0b00010000u > 0u) {
                    if (dmc.samplesLeft == 0) dmc.restart()
                } else {
                    dmc.samplesLeft = 0
                    dmc.silence = true
                }
                dmc.interrupt = false
            }
            0x4017u -> {
                frameCounter.mode = if (data and BIT7 > 0u) 5 else 4
                interruptFlag = data and BIT6 > 0u
                frameCounter.frame = 0
                frameCounter.value = tvType.audioFrameCounterReload
                if (interruptFlag && frameCounter.interrupt) {
                    frameCounter.interrupt = false
                }
                if (frameCounter.mode == 5) {
                    updateEnvelope()
                    updateLinearCounter()
                    updateLength()
                    updateSweep()
                }
            }
        }
    }

    /**
     * CPU bus communication
     */
    fun cpuRead(address: UShort, readOnly: Boolean = false): UByte {
        clockTo(simulation.clock)
        if (address.toUInt() == 0x4015u) {
            var value: UByte = 0u
            timers.forEachIndexed { i, timer ->
                value = value or if (timer.counterLength > 0) (1u shl i).toUByte() else 0u
            }
            value = value or if (dmc.samplesLeft > 0) 16u else 0u
            value = value or if (frameCounter.interrupt) 64u else 0u
            value = value or if (dmc.interrupt) 128u else 0u
            if (!readOnly) frameCounter.interrupt = false
            return value
        }
        return 0x40u
    }

    fun clockTo(ppuClocks: Long) {
        if (soundFiltering) {
            clockToUsingFiltering(ppuClocks)
        }
    }

    fun onFrameFinish() {
        clockTo(simulation.clock)
        beeper.flush(true)
    }

    private fun clockToUsingFiltering(ppuClocks: Long) {
        while (apuClocks < ppuClocks / 3) {
            clocksAfterSample++
            dmc.clock()
            frameCounter.clock()

            timers.forEachIndexed { i, timer ->
                if (i != 2 || timer.counterLength > 0 && linearCounter > 0) {
                    timer.clock()
                }
            }

            accumulator += getOutputLevel()

            while (clocksAfterSample >= cyclesPerSample) {
                beeper.sample(filter.filter((accumulator / clocksAfterSample).toInt()))
                clocksAfterSample -= cyclesPerSample.toInt()
                accumulator = 0
            }

            apuClocks++
        }
    }

    private fun getOutputLevel(): Int {
        val t0 = timers[0] as SquareTimer
        val t1 = timers[1] as SquareTimer
        val t2 = timers[2] as TriangleTimer
        val t3 = timers[3] as NoiseTimer

        return SQUARE_LOOKUP[t0.volume * t0.value + t1.volume * t1.value] +
                TND_LOOKUP[3 * t2.value + 2 * t3.volume * t3.value + dmc.value]
    }

    private fun clockFrameCounter() {
        if (frameCounter.mode == 4 || frameCounter.mode == 5 && frameCounter.frame != 3) {
            updateEnvelope()
            updateLinearCounter()
        }

        if (frameCounter.mode == 4 && (frameCounter.frame == 1 || frameCounter.frame == 3)
            || (frameCounter.mode == 5 && (frameCounter.frame == 1 || frameCounter.frame == 4))
        ) {
            updateLength()
            updateSweep()
        }

        if (!interruptFlag && frameCounter.frame == 3 && frameCounter.mode == 4 && !frameCounter.interrupt) {
            frameCounter.interrupt = true
        }

        frameCounter.frame = (frameCounter.frame + 1) % frameCounter.mode
        timers.forEach { if (it is VolumedTimer) it.refreshVolume() }
    }

    private fun updateLength() {
        timers.forEach {
            if (!it.counterLengthHalt && it.counterLength > 0) {
                it.counterLength--
                if (it.counterLength == 0 && it is VolumedTimer) {
                    it.refreshVolume()
                }
            }
        }
    }

    private fun updateSweep() {
        var counter = 0
        timers.forEach {
            if (it !is SquareTimer) return@forEach
            it.sweepSilence = false
            if (it.sweepReload) {
                it.sweepReload = false
                it.sweepPosition = it.sweepPeriod
            }

            it.sweepPosition++
            val rawPeriod = it.period shr 1
            var shiftedPeriod = rawPeriod shr it.sweepShift
            if (it.sweepNegate) {
                shiftedPeriod = -shiftedPeriod + counter
            }
            shiftedPeriod += rawPeriod
            if (rawPeriod < 8 || shiftedPeriod > 0x7FF) {
                it.sweepSilence = true
            } else if (it.sweepEnabled && it.sweepShift != 0 && it.counterLength > 0 && it.sweepPosition > it.sweepPeriod) {
                it.sweepPosition = 0
                it.period = shiftedPeriod shl 1
            }

            counter++
        }
    }

    private fun updateEnvelope() {
        timers.forEach {
            if (it.envelopeStartFlag) {
                it.envelopeStartFlag = false
                it.envelopePosition = it.envelopeValue + 1
                it.envelopeCounter = 15
            } else {
                it.envelopePosition--
            }

            if (it.envelopePosition <= 0) {
                it.envelopePosition = it.envelopeValue + 1
                if (it.envelopeCounter > 0) {
                    it.envelopeCounter--
                } else if (it.counterLengthHalt && it.envelopeCounter <= 0) {
                    it.envelopeCounter = 15
                }
            }
        }
    }

    private fun updateLinearCounter() {
        if (linearCounterFlag) {
            linearCounter = linearCounterReload
        } else if (linearCounter > 0) {
            linearCounter--
        }

        if (!timers[2].counterLengthHalt) {
            linearCounterFlag = false
        }
    }

    fun reset() {
        apuClocks = 0
        clocksAfterSample = 0
        frameCounter.reset()
        dmc.reset()
        filter.reset()
        timers.forEach { it.reset() }
        interruptFlag = false
        linearCounterFlag = false
        linearCounterReload = 0
        accumulator = 0
        beeper.reset()
    }

}