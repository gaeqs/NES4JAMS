package io.github.gaeqs.nes4jams.gui.project.editor.element

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NESLineTest {

    @Test
    fun testDirective() {
        val elements = NESFileElements(null)
        val line = NESLine(elements, 0, ".db  \t  3 3  2")
        assertTrue(line.directive != null)
        assertTrue(line.directive!!.parameters.size == 3)
        assertTrue(line.instruction == null)
        assertTrue(line.equivalent == null)
        assertTrue(line.directive!!.directive != null)
        assertEquals("db", line.directive!!.directive!!.mnemonic)
    }

    @Test
    fun testInstruction() {
        val elements = NESFileElements(null)
        val line = NESLine(elements, 0, "   lda\t20")
        assertTrue(line.directive == null)
        assertTrue(line.instruction != null)
        assertTrue(line.instruction!!.expression != null)
        assertTrue(line.equivalent == null)
        assertTrue(line.instruction!!.instruction != null)
        assertEquals("LDA", line.instruction!!.instruction!!.mnemonic)
    }

    @Test
    fun testEquivalent() {
        val elements = NESFileElements(null)
        val line = NESLine(elements, 0, "   aaaa   =\t bbbb")
        assertTrue(line.directive == null)
        assertTrue(line.instruction == null)
        assertTrue(line.equivalent != null)
        assertEquals("aaaa", line.equivalent!!.simpleText)
        assertTrue(line.equivalent!!.expression != null)
        assertEquals("\t bbbb", line.equivalent!!.expression!!.text)
    }

}