package io.github.gaeqs.nes4jams.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private val EXPRESSIONS = mapOf(
    "5" to Value(5, false),
    "-5" to Value(-5, false),
    "${'$'}a" to Value(10, false),
    "%110" to Value(6, false),
    "5 + 10" to Value(15, false),
    "5 / 2" to Value(2, false),
    "256 / 2" to Value(128, true),
    "256.b / 2" to Value(0, false),
    "(~255).b" to Value(0, false),
    "2 + 5 / 2" to Value(4, false),
    "(2 + 5) / 2" to Value(3, false),
    "3 * (2 + 3) + 8" to Value(23, false),
    "(2 + 3)(4)" to null,
    "%10 * 4 / (${'$'}1a - 22)" to Value(2, false),
    "${'$'}7F & 128" to Value(0, false)
)

class ParameterExpressionSolverTest {

    @Test
    fun testExpressions() {
        EXPRESSIONS.forEach { (expression, result) ->
            try {
                val solver = ParameterExpressionSolver(expression)
                val solverResult = solver.solve()

                println("$solverResult -> $result")
                Assertions.assertEquals(solverResult, result)
            } catch (ex: IllegalArgumentException) {
                if (result == null) {
                    println("$expression throws an error. Correct.")
                } else {
                    throw ex
                }
            }
        }
    }

}