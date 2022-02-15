package io.github.gaeqs.nes4jams.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.max
import kotlin.math.min

class Beeper(sampleRate: Int) {

    private val line: SourceDataLine
    private val buffer: ByteArray
    private var amount = 0
    private var volume = 0.8

    init {
        buffer = ByteArray(sampleRate / 8)
        val audioFormat = AudioFormat(sampleRate.toFloat(), 16, 2, true, false)
        line = AudioSystem.getSourceDataLine(audioFormat)
        line.open(audioFormat, sampleRate / 4)
        line.start()
    }

    fun flush() {
        line.write(buffer, 0, amount)
        amount = 0
    }

    fun sample(sample: Int) {
        val finalSample = max(min((sample * volume).toInt(), 32767), -32768)
        buffer[amount++] = finalSample.toByte()
        buffer[amount++] = (finalSample shr 8).toByte()
        buffer[amount++] = finalSample.toByte()
        buffer[amount++] = (finalSample shr 8).toByte()
        if (amount + 4 >= buffer.size) flush()
    }

    fun samplesBeingProcessed(): Int {
        return buffer.size - line.available() + amount
    }

    fun requiredSamples(): Int {
        val amount = samplesBeingProcessed()
        val max = buffer.size / 6
        if (max <= amount) return 0
        return (max - amount) / 4
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
        amount = 0
        line.flush()
        volume = 0.8
    }

}