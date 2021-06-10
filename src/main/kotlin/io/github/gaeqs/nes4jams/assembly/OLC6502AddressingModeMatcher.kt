package io.github.gaeqs.nes4jams.assembly

import io.github.gaeqs.nes4jams.utils.BYTE_RANGE
import io.github.gaeqs.nes4jams.utils.SHORT_RANGE
import io.github.gaeqs.nes4jams.utils.toIntOldWayOrNull

interface OLC6502AddressingModeMatcher {

    fun matches(parameters: String): Pair<Boolean, Int?>


    class Implied : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            return Pair(parameters.isBlank() || parameters.trim() == "A", null)
        }
    }

    class Immediate : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }
            if (!str.startsWith("#")) return Pair(false, null)
            val number = str.substring(1).toIntOldWayOrNull()
            return Pair(number != null && BYTE_RANGE.contains(number), number)
        }
    }

    class ZeroPage : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }
            val number = str.toIntOldWayOrNull()
            return Pair(number != null && BYTE_RANGE.contains(number), null)
        }
    }

    class ZeroPageX : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.endsWith(",x")) return Pair(false, null)

            val number = str.substring(0, str.length - 2).toIntOldWayOrNull()
            return Pair(number != null && BYTE_RANGE.contains(number), null)
        }
    }

    class ZeroPageY : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.endsWith(",y")) return Pair(false, null)

            val number = str.substring(0, str.length - 2).toIntOldWayOrNull()
            return Pair(number != null && BYTE_RANGE.contains(number), null)
        }
    }

    class Relative : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }
            val number = str.toIntOldWayOrNull()
            return Pair(number != null && BYTE_RANGE.contains(number), null)
        }
    }

    class Absolute : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }
            val number = str.toIntOldWayOrNull()
            return Pair(number != null && SHORT_RANGE.contains(number), null)
        }
    }

    class AbsoluteX : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.endsWith(",x")) return Pair(false, null)

            val number = str.substring(0, str.length - 2).toIntOldWayOrNull()
            return Pair(number != null && SHORT_RANGE.contains(number), null)
        }
    }

    class AbsoluteY : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.endsWith(",y")) return Pair(false, null)

            val number = str.substring(0, str.length - 2).toIntOldWayOrNull()
            return Pair(number != null && SHORT_RANGE.contains(number), null)
        }
    }

    class Indirect : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.startsWith("(") || !str.endsWith(")")) return Pair(false, null)

            val number = str.substring(1, str.length - 1).toIntOldWayOrNull()
            return Pair(number != null && SHORT_RANGE.contains(number), null)
        }
    }


    class IndirectX : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.startsWith("(") || !str.endsWith(",x)")) return Pair(false, null)

            val number = str.substring(1, str.length - 3).toIntOldWayOrNull()
            return Pair(number != null && SHORT_RANGE.contains(number), null)
        }
    }


    class IndirectY : OLC6502AddressingModeMatcher {
        override fun matches(parameters: String): Pair<Boolean, Int?> {
            val str = parameters.filter { !it.isWhitespace() }.lowercase()
            if (!str.startsWith("(") || !str.endsWith("),y")) return Pair(false, null)

            val number = str.substring(1, str.length - 3).toIntOldWayOrNull()
            return Pair(number != null && SHORT_RANGE.contains(number), null)
        }
    }



}