package io.github.gaeqs.nes4jams.audio.utilities

class Filter {

    private var dcKiller = -6392
    private var linearPointAccumulator = 0

    fun filter(value: Int): Int {
        val sample = value - dcKiller
        dcKiller += sample shr 8 + if (sample > 0) 1 else -1
        linearPointAccumulator += (sample - linearPointAccumulator) / 2
        return linearPointAccumulator
    }

    fun reset() {
        dcKiller = -6392
        linearPointAccumulator = 0
    }

}