package io.github.gaeqs.nes4jams.audio.timer

abstract class Timer {

    companion object {
        val COUNTER_LENGTH_LOAD = listOf(10, 254, 20, 2, 40, 4, 80, 6,
            160, 8, 60, 10, 14, 12, 26, 14, 12, 16, 24, 18, 48, 20, 96, 22,
            192, 24, 72, 26, 16, 28, 32, 30)
    }

    var counterLengthEnabled = true
    var counterLengthHalt = true
    var counterLength = 0

    var envelopeStartFlag = false
    var envelopePosition = 0
    var envelopeValue = 15
    var envelopeCounter = 0

    abstract var period: Int
    abstract val value: Int

    abstract fun setDuty(duty: Int)

    abstract fun setDuty(duty: IntArray)

    abstract fun reset()

    abstract fun clock()

    abstract fun clock(cycles: Int)

}