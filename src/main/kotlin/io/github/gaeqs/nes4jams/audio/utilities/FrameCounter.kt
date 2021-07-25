package io.github.gaeqs.nes4jams.audio.utilities

import io.github.gaeqs.nes4jams.cartridge.TVType

class FrameCounter(private val tvType: TVType, private val run: () -> Unit) {

    var value = 7456
    var mode = 4
    var frame = 0

    var interrupt = false

    fun clock() {
        value--
        if (value <= 0) {
            value = tvType.audioFrameCounterReload
            run()
        }
    }

    fun reset() {
        value = 7456
        mode = 4
        frame = 0
    }

}