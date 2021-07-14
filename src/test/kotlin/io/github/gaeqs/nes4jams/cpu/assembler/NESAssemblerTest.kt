package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import net.jamsimulator.jams.utils.RawFileData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL

private const val MARIO_URL =
    "https://gist.githubusercontent.com/1wErt3r/4048722/raw/59e88c0028a58c6d7b9156749230ccac647bc7d4/SMBDIS.ASM"

private const val PROGRAM = """
    SND_NOISE_REG = $400c
    
    .org $8000
    .globl test
test:
    jmp test
    lda #$01
    lda #test.b
    lda #$03
    and #%01011111
    sta SND_NOISE_REG*2
    jmp test
    bne test
    .db $0FE 20 20
"""

private val BANKS = listOf(NESMemoryBank(0x8000u, 0x8000u, true))

class OLDNESAssemblerTest {
    @Test
    fun assemblyTest() {
        val assembler = NESAssembler(listOf(RawFileData("test.asm", PROGRAM)), BANKS, null)
        assembler.assemble()
        //val disassembled = data.disassemble(0x8000u, 0x8020u)
        //disassembled.toSortedMap().forEach { instruction -> println(instruction.value) }
        Assertions.assertTrue(assembler.assembled)
    }

    @Test
    fun assembleMario() {
        val raw = URL(MARIO_URL).readText()
        val assembler = NESAssembler(listOf(RawFileData("test.asm", raw)), BANKS, null)
        assembler.files.forEach {
            println("FILE ${it.name}")
            it.lines.forEachIndexed { index, line -> println("${index + 1} -> $line") }
        }
        assembler.assemble()

        assembler.banks.forEachIndexed { index, it ->
            println("------- BANK $index -------")
            it.disassemble().toSortedMap().forEach { (address, instruction) -> println("$address\t${instruction}") }
        }
        Assertions.assertTrue(assembler.assembled)
    }
}
