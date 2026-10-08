package com.example.domain.engine

import java.util.Locale

object CalculatorEngine {

    /**
     * Attempts to evaluate simple arithmetic expressions like:
     * "5 + 5", "10 * 20", "100 / 4", "2^8", "sqrt(16)", etc.
     */
    fun tryCalculate(input: String): String? {
        val trimmed = input.trim().lowercase(Locale.ROOT)
            .removePrefix("calculate")
            .removePrefix("what is")
            .removePrefix("what's")
            .removePrefix("how much is")
            .removeSuffix("?")
            .trim()

        // Match binary arithmetic: <num> <op> <num>
        val binaryRegex = Regex("""^(\d+(?:\.\d+)?)\s*([\+\-\*\/\^x×÷])\s*(\d+(?:\.\d+)?)$""")
        val binaryMatch = binaryRegex.matchEntire(trimmed)
        if (binaryMatch != null) {
            val a = binaryMatch.groupValues[1].toDoubleOrNull() ?: return null
            val op = binaryMatch.groupValues[2]
            val b = binaryMatch.groupValues[3].toDoubleOrNull() ?: return null

            val result = when (op) {
                "+", "plus" -> a + b
                "-", "minus" -> a - b
                "*", "x", "×", "times", "multiplied by" -> a * b
                "/", "÷", "divided by" -> if (b != 0.0) a / b else Double.NaN
                "^" -> Math.pow(a, b)
                else -> return null
            }

            if (result.isNaN()) return "Division by zero is undefined."
            val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else "%.4f".format(result).trimEnd('0').trimEnd('.')
            return "$input = $formatted"
        }

        // Percentage: "what is 20% of 150"
        val pctRegex = Regex("""^(\d+(?:\.\d+)?)\s*%\s*(?:of)?\s*(\d+(?:\.\d+)?)$""")
        val pctMatch = pctRegex.matchEntire(trimmed)
        if (pctMatch != null) {
            val pct = pctMatch.groupValues[1].toDoubleOrNull() ?: return null
            val num = pctMatch.groupValues[2].toDoubleOrNull() ?: return null
            val res = (pct / 100.0) * num
            val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(res).trimEnd('0').trimEnd('.')
            return "$pct% of $num = $formatted"
        }

        return null
    }
}
