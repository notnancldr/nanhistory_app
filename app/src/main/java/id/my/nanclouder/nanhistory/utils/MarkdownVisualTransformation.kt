package id.my.nanclouder.nanhistory.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * VisualTransformation that styles Markdown syntax patterns dynamically during text input.
 * Keeps text length and indices untouched using [OffsetMapping.Identity] for seamless cursor navigation.
 */
class MarkdownVisualTransformation(
    private val primaryColor: Color = Color(0xFF1976D2),
    private val codeBackgroundColor: Color = Color(0x1F000000),
    private val tagColor: Color = Color(0xFF1976D2),
    private val quoteColor: Color = Color(0xFF757575),
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val transformed = buildAnnotatedString {
            append(raw)

            // 1. Headings (# H1, ## H2, ### H3, etc.)
            headingRegex.findAll(raw).forEach { match ->
                val hashes = match.groupValues[1].trimStart()
                val level = hashes.length
                val fontSize = when (level) {
                    1 -> 24.sp
                    2 -> 20.sp
                    3 -> 18.sp
                    else -> 16.sp
                }
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 2. Bold (**bold**)
            boldRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 3. Strikethrough (~~strike~~)
            strikethroughRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 4. Italic (*italic* or _italic_)
            italicRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 5. Code blocks (```code```)
            codeBlockRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackgroundColor
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 6. Inline code (`code`)
            inlineCodeRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackgroundColor
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 7. Links ([title](url))
            linkRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 8. Mentions (@tag)
            mentionRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(color = tagColor, fontWeight = FontWeight.Medium),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 9. Blockquotes (> quote)
            blockquoteRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        return TransformedText(transformed, OffsetMapping.Identity)
    }

    companion object {
        private val headingRegex = Regex("(?m)^(\\s{0,3}#{1,6})\\s+.*$")
        private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        private val strikethroughRegex = Regex("~~(.+?)~~")
        private val italicRegex =
            Regex("(?<!\\*)\\*(?!\\*)([^*\\n]+?)(?<!\\*)\\*(?!\\*)|(?<!_)_{1}([^_\\n]+?)_{1}(?!_)")
        private val codeBlockRegex = Regex("(?s)```.*?```")
        private val inlineCodeRegex = Regex("`([^`\\n]+)`")
        private val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
        private val mentionRegex = Regex("(?<=^|\\s)@([A-Za-z0-9_]+)")
        private val blockquoteRegex = Regex("(?m)^\\s*>.*$")
    }
}
