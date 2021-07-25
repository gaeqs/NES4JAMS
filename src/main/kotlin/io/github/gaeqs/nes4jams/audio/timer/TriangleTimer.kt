package io.github.gaeqs.nes4jams.audio.timer

import kotlin.math.max

class TriangleTimer : Timer() {

    companion object {
        private val VALUES = IntArray(32) { if (it > 15) 32 - it else it }
    }

    override var period: Int = 0
    override val value: Int
        get() = if (period == 0) 7 else VALUES[position]

    private var position = 0
    private var divider = 0

    override fun setDuty(duty: Int) {
        throw UnsupportedOperationException()
    }

    override fun setDuty(duty: IntArray) {
        throw UnsupportedOperationException()
    }

    override fun reset() {
    }

    override fun clock() {
        if (period + 1 <= 0) return
        divider++
        refreshPosition()
    }

    override fun clock(cycles: Int) {
        if (period < 8) return
        divider += cycles
        refreshPosition()
    }

    private fun refreshPosition() {
        val periods = max((divider + period + 1) / (period + 1), 0)
        position = (position + periods) and 0x1F
        divider -= (period + 1) * periods
    }
}