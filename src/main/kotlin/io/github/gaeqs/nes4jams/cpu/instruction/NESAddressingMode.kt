package io.github.gaeqs.nes4jams.cpu.instruction

import io.github.gaeqs.nes4jams.cpu.OLC6502
import kotlin.reflect.KFunction
import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingModeMatcher as Matcher

enum class NESAddressingMode(
    val usesWordInAssembler: Boolean,
    val bytesUsed: Int,
    val matcher: Matcher,
    val addressingFunction: KFunction<Boolean>
) {
    IMPLIED(false, 0, Matcher.Implied(), OLC6502::imp),
    IMMEDIATE(false, 1, Matcher.Immediate(), OLC6502::imm),
    ZERO_PAGE(false, 1, Matcher.ZeroPage(), OLC6502::zp0),
    ZERO_PAGE_X(false, 1, Matcher.ZeroPageX(), OLC6502::zpx),
    ZERO_PAGE_Y(false, 1, Matcher.ZeroPageY(), OLC6502::zpy),
    RELATIVE(true, 1, Matcher.Relative(), OLC6502::rel),
    ABSOLUTE(true, 2, Matcher.Absolute(), OLC6502::abs),
    ABSOLUTE_X(true, 2, Matcher.AbsoluteX(), OLC6502::abx),
    ABSOLUTE_Y(true, 2, Matcher.AbsoluteY(), OLC6502::aby),
    INDIRECT(true, 2, Matcher.Indirect(), OLC6502::ind),
    INDIRECT_X(false, 1, Matcher.IndirectX(), OLC6502::inx),
    INDIRECT_Y(false, 1, Matcher.IndirectY(), OLC6502::iny);

    companion object {

        fun matchingAddressingModes(parameters: String): List<Pair<NESAddressingMode, MatchResult>> =
            matchingAddressingModesFor(parameters, values())

        fun matchingAddressingModesFor(
            parameters: String,
            possibleModes: Array<NESAddressingMode>
        ): List<Pair<NESAddressingMode, MatchResult>> =
            possibleModes.map { it to it.matcher.matches(parameters) }.filter { it.second.valid }.toList()

        fun matchingAddressingModesFor(
            parameters: String,
            possibleModes: Collection<NESAddressingMode>
        ): List<Pair<NESAddressingMode, MatchResult>> =
            possibleModes.map { it to it.matcher.matches(parameters) }.filter { it.second.valid }.toList()
    }
}
