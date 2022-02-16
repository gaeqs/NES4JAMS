package io.github.gaeqs.nes4jams.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.max
import kotlin.math.min

class Beeper(sampleRate: Int) {

    private val line: SourceDataLine
    private val buffer = ByteArray(4)
    private var volume = 0.8

    private val innerBufferSize: Int

    init {
        val audioFormat = AudioFormat(sampleRate.toFloat(), 16, 2, true, false)
        line = AudioSystem.getSourceDataLine(audioFormat)

        innerBufferSize = sampleRate
        line.open(audioFormat, innerBufferSize)
    }

    fun sample(sample: Int) {
        val finalSample = max(min((sample * volume).toInt(), 32767), -32768)
        buffer[0] = finalSample.toByte()
        buffer[1] = (finalSample shr 8).toByte()
        buffer[2] = finalSample.toByte()
        buffer[3] = (finalSample shr 8).toByte()
        line.write(buffer, 0, 4)
    }

    fun samplesBeingProcessedPercentage(): Float {
        return (innerBufferSize - line.available()) / innerBufferSize.toFloat()
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