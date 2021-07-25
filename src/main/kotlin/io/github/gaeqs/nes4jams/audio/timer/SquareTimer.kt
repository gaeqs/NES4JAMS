package io.github.gaeqs.nes4jams.audio.timer

import kotlin.math.max

class SquareTimer(cycleLength: Int, private val periodAdd: Int = 0) : Timer(), VolumedTimer {

    override var period: Int = 0
    override val value: Int
        get() = values[position]

    override var volume = 0
    override var envelopeConstantVolume = false

    var sweepEnabled = false
    var sweepSilence = false
    var sweepReload = false
    var sweepPosition = 0
    var sweepPeriod = 15
    var sweepShift = 0
    var sweepNegate = false

    private var position = 0
    private var values = IntArray(cycleLength)
    private var divider = 0

    init {
        setDuty(cycleLength / 2)
    }

    override fun setDuty(duty: Int) {
        repeat(values.size) { values[it] = if (it < duty) 1 else 0 }
    }

    override fun setDuty(duty: IntArray) {
        values = duty
    }

    override fun reset() {
        position = 0
    }

    override fun clock() {
        if (period + periodAdd <= 0) return
        divider++
        refreshPosition()
    }

    override fun clock(cycles: Int) {
        if (period < 8) return
        divider += cycles
        refreshPosition()
    }

    override fun refreshVolume() {
        volume = if (counterLength <= 0 || sweepSilence) 0
        else if (envelopeConstantVolume) envelopeValue else envelopeCounter
    }

    private fun refreshPosition() {
        val periods = max((divider + period + periodAdd) / (period + periodAdd), 0)
        position = (position + periods) % values.size
        divider -= (period + periodAdd) * periods
    }
}