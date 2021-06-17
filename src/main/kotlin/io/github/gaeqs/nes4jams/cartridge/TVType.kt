package io.github.gaeqs.nes4jams.cartridge

enum class TVType(
    val audioDMCPeriods: List<Int>,
    val audioNoisePeriod: List<Int>,
    val audioFrameCounterReload: Int,
    private val audioCyclesPerSample: Double,
) {

    NTSC(
        listOf(428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54),
        listOf(4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068),
        7456,
        1789773.0
    ),
    DENDY(
        listOf(428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54),
        listOf(4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068),
        7456,
        1773448.0
    ),
    PAL(
        listOf(398, 354, 316, 298, 276, 236, 210, 198, 176, 148, 132, 118, 98, 78, 66, 50),
        listOf(4, 8, 14, 30, 60, 88, 118, 148, 188, 236, 354, 472, 708, 944, 1890, 3778),
        8312,
        1662607.0
    );

    fun getAudioCyclesPerSample(sampleRate: Double): Double {
        return audioCyclesPerSample / sampleRate
    }

}