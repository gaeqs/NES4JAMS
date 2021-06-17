package io.github.gaeqs.nes4jams.cpu.instruction

import io.github.gaeqs.nes4jams.utils.Value
import io.github.gaeqs.nes4jams.utils.extension.isLabelLegal
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpression
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpressionWithInvalids

data class MatchResult(
    val valid: Boolean,
    val validNumber: Boolean = false,
    val number: Value? = null,
    val isWord: Boolean = false,
    val invalidNumbers: Set<String> = emptySet(),
    val label: String? = null
)

interface NESAddressingModeMatcher {

    fun matches(parameters: String): MatchResult


    class Implied : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            return MatchResult(parameters.isBlank() || parameters.trim() == "A", true, Value(0, false))
        }
    }

    class Immediate : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if (!str.startsWith("#")) return MatchResult(false)

            val label = str.substring(1)
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()

            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, invalidResult, invalidResult.isWord, invalidNumbers, label)
        }
    }

    class ZeroPage : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            val number = str.parseParameterExpression()
            val (invalidResult, invalidNumbers) = str.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()


            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, str)
        }
    }

    class ZeroPageX : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            if (!str.endsWith(",x") && !str.endsWith(",X")) return MatchResult(false)

            val label = str.substring(0, str.length - 2)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }

    class ZeroPageY : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            if (!str.endsWith(",y") && !str.endsWith(",Y")) return MatchResult(false)

            val label = str.substring(0, str.length - 2)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }

    class Relative : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            val number = str.parseParameterExpression()
            val (invalidResult, invalidNumbers) = str.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, str)
        }
    }

    class Absolute : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            val number = str.parseParameterExpression()
            val (invalidResult, invalidNumbers) = str.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, str)
        }
    }

    class AbsoluteX : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            if (!str.endsWith(",x") && !str.endsWith(",X")) return MatchResult(false)

            val label = str.substring(0, str.length - 2)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }

    class AbsoluteY : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if(str.startsWith("(")) return MatchResult(false)
            if (!str.endsWith(",y") && !str.endsWith(",Y")) return MatchResult(false)

            val label = str.substring(0, str.length - 2)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }

    class Indirect : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if (!str.startsWith("(") || !str.endsWith(")")) return MatchResult(false)

            val label = str.substring(1, str.length - 1)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }


    class IndirectX : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if (!str.startsWith("(") || !str.endsWith(",x)") && !str.endsWith(",X)")) return MatchResult(false)

            val label = str.substring(1, str.length - 3)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }


    class IndirectY : NESAddressingModeMatcher {
        override fun matches(parameters: String): MatchResult {
            val str = parameters.filter { !it.isWhitespace() }
            if (!str.startsWith("(") || !str.endsWith("),y") && !str.endsWith("),Y")) return MatchResult(false)

            val label = str.substring(1, str.length - 3)
            val number = label.parseParameterExpression()
            val (invalidResult, invalidNumbers) = label.parseParameterExpressionWithInvalids()
            if (invalidResult == null) return MatchResult(false)
            if (invalidNumbers.any { !it.isLabelLegal() }) return MatchResult(false)
            val validNumber = invalidNumbers.isEmpty()

            return MatchResult(true, validNumber, number, invalidResult.isWord, invalidNumbers, label)
        }
    }


}