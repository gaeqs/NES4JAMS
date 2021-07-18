package io.github.gaeqs.nes4jams.cartridge

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssembler
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.ppu.Mirror
import net.jamsimulator.jams.utils.RawFileData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

private val BANKS = listOf(NESMemoryBank(0x8000u, 0x8000u, true, true))

class CartridgeHeaderTest {

    @Test
    fun testMario() {
        if (!File("roms/smb.nes").isFile) return
        val stream = File("roms/smb.nes").inputStream()
        val header = CartridgeHeader(stream)
        stream.close()

        println(header.name)
        assertEquals(true, header.isValid)
        assertEquals(false, header.isINES2)
        assertEquals(TVType.NTSC, header.tvType)
        assertEquals(ConsoleType.NES, header.consoleType)
        assertEquals(Mirror.VERTICAL, header.mirroring)
        assertEquals(2u * 0x4000u, header.prgRomSize)
        assertEquals(1u * 0x2000u, header.chrRomSize)
        assertEquals(false, header.hasBatteryComponents)
        assertEquals(false, header.hardWiredFourScreenMode)
        assertEquals(false, header.hasTrainerData)
    }

    @Test
    fun compileMario() {
        if (!File("roms/smb.nes").isFile) return
        val stream = File("roms/smb.nes").inputStream()
        val header = CartridgeHeader(stream)

        if (header.hasTrainerData) {
            stream.skip(512)
        }

        stream.skip(header.prgRomSize.toLong())
        val chrMemory = ByteArray(header.chrRomSize.toInt()) { stream.read().toByte() }
        stream.close()

        val raw = File("roms/mario.asm").readText()
        val assembler = NESAssembler(listOf(RawFileData("test.asm", raw)), BANKS, null)
        assembler.assemble()

        val out = File("roms/out.nes").outputStream()
        out.write(header.toByteArray())
        out.write(assembler.banks[0].array.toByteArray())
        out.write(chrMemory)
        out.close()
    }

    @Test
    fun compare() {
        val original = File("roms/smb.nes").inputStream()
        val new = File("roms/out.nes").inputStream()

        val originalHeader = CartridgeHeader(original)
        val newHeader = CartridgeHeader(new)

        assertEquals(originalHeader, newHeader)

        if (originalHeader.hasTrainerData) {
            original.skip(512)
            new.skip(512)
        }

        var address = 0x8000
        repeat(originalHeader.prgRomSize.toInt()) {
            assertEquals(original.read(), new.read(), "Address ${address.toString(16)}")

            address++
        }

        original.close()
        new.close()
    }

}