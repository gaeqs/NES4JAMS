package io.github.gaeqs.nes4jams.cpu.assembler

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL

private const val MARIO_URL =
    "https://gist.githubusercontent.com/1wErt3r/4048722/raw/59e88c0028a58c6d7b9156749230ccac647bc7d4/SMBDIS.ASM"

private const val PROGRAM = """
    SND_NOISE_REG = $400c
    
    .org $8000
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

class NESAssemblerTest {
    @Test
    fun assemblyTest() {
        val assembler = NESAssembler(mapOf("test.asm" to PROGRAM))
        val data = assembler.assemble(0x8000u, 0x4000)
        val disassembled = data.disassemble(0x8000u, 0x8020u)
        disassembled.toSortedMap().forEach { instruction -> println(instruction.value) }
        Assertions.assertTrue(assembler.assembled)
    }

    @Test
    fun assembleMario() {
        val raw = URL(MARIO_URL).readText()
        val assembler = NESAssembler(mapOf("test.asm" to raw))
        assembler.files.forEach {
            println("FILE ${it.name}")
            it.lines.forEachIndexed { index, line -> println("${index + 1} -> $line") }
        }
        val data = assembler.assemble(0x8000u, 0x8000)
        val disassembled = data.disassemble(0x8000u, 0x8020u)
        disassembled.toSortedMap().forEach { instruction -> println(instruction.value) }
        Assertions.assertTrue(assembler.assembled)
    }
}
