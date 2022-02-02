package io.github.gaeqs.nes4jams.audio

import io.github.gaeqs.nes4jams.cartridge.TVType
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.max
import kotlin.math.min

class Beeper(sampleRate: Int, tvType: TVType) {

    private val line: SourceDataLine
    private val buffer: ByteArray
    private var amount = 0
    private var volume = 0.8

    init {
        val fps = if (tvType == TVType.NTSC) 60.0 else 50.0
        buffer = ByteArray(sampleRate * 4)

        val audioFormat = AudioFormat(sampleRate.toFloat(), 16, 2, true, false)
        line = AudioSystem.getSourceDataLine(audioFormat)
        line.open(audioFormat, sampleRate * 4)
        line.start()
    }

    fun flush(waitIfFull: Boolean) {
        if (line.available() >= amount || waitIfFull) {
            line.write(buffer, 0, amount)
        }
        amount = 0
    }

    fun sample(sample: Int) {
        val finalSample = max(min((sample * volume).toInt(), 32767), -32768)
        buffer[amount++] = finalSample.toByte()
        buffer[amount++] = (finalSample shr 8).toByte()
        buffer[amount++] = finalSample.toByte()
        buffer[amount++] = (finalSample shr 8).toByte()
    }

    fun samplesBeingProcessed(): Int {
        return buffer.size - line.available() + amount
    }

    fun requiredSamples(): Int {
        val amount = samplesBeingProcessed()
        val max = buffer.size / 6
        if(max <= amount) return 0
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
        flush(false)
        line.flush()
        volume = 0.8
    }

}