package io.github.gaeqs.nes4jams.audio.timer

import kotlin.math.max

class NoiseTimer : Timer(), VolumedTimer {

    override var period: Int = 1
    override val value: Int
        get() = values[position] and 1

    override var volume = 0
    override var envelopeConstantVolume = false

    private var position = 0
    private var values = generateValues(1, 1)
    private var divider = 0
    private var previousDuty = 1

    override fun setDuty(duty: Int) {
        if (duty != previousDuty) {
            values = generateValues(duty, values[position])
            position = 0
        }
        previousDuty = duty
    }

    override fun setDuty(duty: IntArray) {
        throw UnsupportedOperationException()
    }

    override fun reset() {
        position = 0
    }

    override fun clock() {
        divider++
        refreshPosition()
    }

    override fun clock(cycles: Int) {
        divider += cycles
        refreshPosition()
    }

    override fun refreshVolume() {
        volume = if (counterLength <= 0) 0 else if (envelopeConstantVolume) envelopeValue else envelopeCounter
    }

    private fun refreshPosition() {
        val periods = max((divider + period) / period, 0)
        position = (position + periods) % values.size
        divider -= period * periods
    }
}

private fun generateValues(from: Int, startSeed: Int): IntArray {
    var seed = startSeed
    val array = IntArray(if (from == 1) 32767 else 93)
    repeat(array.size) {
        seed = (seed shr 1 or if ((seed and (1 shl from) != 0) xor (seed and 0x1 != 0)) 16384 else 0)
        array[it] = seed
    }
    return array
}