package io.github.gaeqs.nes4jams.audio

import io.github.gaeqs.nes4jams.cartridge.TVType
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class Beeper(sampleRate: Int, tvType: TVType) {

    private val line: SourceDataLine
    private val buffer = ByteArray(4) { 0 }
    private var volume = 0.8


    init {
        val audioFormat = AudioFormat(sampleRate.toFloat(), 16, 2, true, false)
        line = AudioSystem.getSourceDataLine(audioFormat)
        line.open(audioFormat, sampleRate / 2)
    }

    fun flush(waitIfFull: Boolean) {
        //if (line.available() >= amount || waitIfFull) {
        //    line.write(buffer, 0, amount)
        //}
        //amount = 0
    }

    fun sample(sample: Int) {
        val finalSample = max(min((sample * volume).toInt(), 32767), -32768)
        buffer[0] = finalSample.toByte()
        buffer[1] = (finalSample shr 8).toByte()
        buffer[2] = finalSample.toByte()
        buffer[3] = (finalSample shr 8).toByte()
        if(line. >= sampler)
        line.write(buffer, 0, 4)
    }

    fun samplesBeingProcessed(): Int {
        return line.bufferSize - line.available()
    }

    fun pause() {
        line.flush()
        line.stop()
    }

    fun resume() {
        line.start()
    }

    fun destroy() {
        line.stop()
        line.close()
    }

    fun reset() {
        line.flush()
        volume = 0.8
    }

}