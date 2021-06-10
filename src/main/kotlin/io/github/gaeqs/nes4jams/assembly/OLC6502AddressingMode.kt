package io.github.gaeqs.nes4jams.assembly

import kotlin.reflect.KFunction
import io.github.gaeqs.nes4jams.assembly.OLC6502AddressingModeMatcher as Matcher

enum class OLC6502AddressingMode(val matcher: Matcher, val addressingFunction: KFunction<Boolean>) {

    IMPLIED(Matcher.Implied(), OLC6502::imp),
    IMMEDIATE(Matcher.Immediate(), OLC6502::imm),
    ZERO_PAGE(Matcher.ZeroPage(), OLC6502::zp0),
    ZERO_PAGE_X(Matcher.ZeroPageX(), OLC6502::zpx),
    ZERO_PAGE_Y(Matcher.ZeroPageY(), OLC6502::zpy),
    RELATIVE(Matcher.Relative(), OLC6502::rel),
    ABSOLUTE(Matcher.Absolute(), OLC6502::abs),
    ABSOLUTE_X(Matcher.AbsoluteX(), OLC6502::abx),
    ABSOLUTE_Y(Matcher.AbsoluteY(), OLC6502::aby),
    INDIRECT(Matcher.Indirect(), OLC6502::ind),
    INDIRECT_X(Matcher.IndirectX(), OLC6502::inx),
    INDIRECT_Y(Matcher.IndirectY(), OLC6502::iny)

}
