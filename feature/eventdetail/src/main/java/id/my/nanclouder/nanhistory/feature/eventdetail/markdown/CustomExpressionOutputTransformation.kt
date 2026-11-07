package id.my.nanclouder.nanhistory.feature.eventdetail.markdown

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * OutputTransformation providing syntax highlighting for mathematical and telemetry graph expressions.
 */
class CustomExpressionOutputTransformation(
    private val propertyColor: Color = Color(0xFF1976D2),
    private val functionColor: Color = Color(0xFF00897B),
    private val operatorColor: Color = Color(0xFFF57C00),
    private val numberColor: Color = Color(0xFF7B1FA2),
    private val stringColor: Color = Color(0xFF388E3C),
    private val siblingColor: Color = Color(0xFFC2185B),
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence().toString()
        if (text.isEmpty()) return

        addStyle(SpanStyle(fontFamily = FontFamily.Monospace), 0, text.length)

        numberRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
        }

        stringRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
        }

        functionRegex.findAll(text).forEach { match ->
            val name = match.groupValues[1]
            if (knownFunctions.contains(name.lowercase())) {
                addStyle(
                    SpanStyle(color = functionColor, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.first + name.length
                )
            }
        }

        identRegex.findAll(text).forEach { match ->
            val name = match.value
            if (knownProperties.contains(name.lowercase())) {
                addStyle(
                    SpanStyle(color = propertyColor, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        siblingOffsetRegex.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = siblingColor, fontWeight = FontWeight.SemiBold),
                match.range.first,
                match.range.last + 1
            )
        }

        operatorRegex.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(color = operatorColor, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    companion object {
        private val numberRegex = Regex("\\b\\d+(?:\\.\\d+)?\\b")
        private val stringRegex = Regex("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'")
        private val functionRegex = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(")
        private val identRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        private val siblingOffsetRegex = Regex("\\[\\s*[+-]?\\d+\\s*\\]")
        private val operatorRegex = Regex("[+\\-*/%^]")

        private val knownProperties = setOf(
            "speed", "acceleration", "distance", "altitude", "bearing", "accuracy",
            "speedaccuracy", "bearingaccuracy", "verticalaccuracy",
            "latitude", "lat", "longitude", "lon", "lng", "time"
        )

        private val knownFunctions = setOf(
            "avg", "abs", "sqrt", "min", "max", "round", "floor", "ceil", "property"
        )
    }
}
