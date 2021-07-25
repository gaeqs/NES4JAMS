package io.github.gaeqs.nes4jams.audio.timer

interface VolumedTimer {

    var volume: Int
    var envelopeConstantVolume : Boolean

    fun refreshVolume()

}