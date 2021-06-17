package io.github.gaeqs.nes4jams.ppu

@ExperimentalUnsignedTypes
enum class Mirror(private val mapper: (Array<UByteArray>, UShort) -> UByte) {
    HARDWARE({ nameTables, address ->
        0u
    }),
    VERTICAL({ nameTables, address ->
        when (address and 0x0FFFu) {
            in 0x0000u..0x03FFu,
            in 0x0800u..0x0BFFu -> nameTables[0][(address and 0x03FFu).toInt()]
            else -> nameTables[1][(address and 0x03FFu).toInt()]
        }
    }),
    HORIZONTAL({ nameTables, address ->
        when (address and 0x0FFFu) {
            in 0x0000u..0x07FFu -> nameTables[0][(address and 0x03FFu).toInt()]
            else -> nameTables[1][(address and 0x03FFu).toInt()]
        }
    }),
    ONESCREEN_LO({ nameTables, address ->
        nameTables[0][(address and 0x03FFu).toInt()]
    }),
    ONESCREEN_HI({ nameTables, address ->
        nameTables[1][(address and 0x03FFu).toInt()]
    });

    fun map(nameTables: Array<UByteArray>, address: UShort): UByte = mapper(nameTables, address)
}