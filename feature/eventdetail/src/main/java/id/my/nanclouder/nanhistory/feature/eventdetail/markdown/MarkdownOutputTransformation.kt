package id.my.nanclouder.nanhistory.feature.eventdetail.markdown

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import id.my.nanclouder.nanhistory.utils.highlightCode

/**
 * OutputTransformation styling Markdown patterns and fenced code blocks with live syntax highlighting.
 */
class MarkdownOutputTransformation(
    private val isDarkTheme: Boolean = false,
    private val primaryColor: Color = Color(0xFF1976D2),
    private val tagColor: Color = Color(0xFF1976D2),
    private val quoteColor: Color = Color(0xFF757575),
    private val fenceDimColor: Color = Color(0x66888888),
    private val inlineCodeBackgroundColor: Color = Color(0x1F888888),
    private val onCodeBlockRangesUpdated: ((List<IntRange>) -> Unit)? = null,
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        val originalText = asCharSequence().toString()
        if (originalText.isEmpty()) {
            onCodeBlockRangesUpdated?.invoke(emptyList())
            return
        }

        val codeBlocks = findCodeBlocks(originalText)
        val renderedCodeRanges = mutableListOf<IntRange>()

        // Process code blocks in reverse order to preserve string indices for deletions
        for (block in codeBlocks.asReversed()) {
            val cursorInside = selection.max >= block.fullRange.first &&
                selection.min <= (block.closeFenceRange?.let { it.last + 1 } ?: (block.fullRange.last + 1))
            val shouldHideFences = block.closeFenceRange != null && !cursorInside

            val contentStart: Int
            val contentEnd: Int

            if (!shouldHideFences) {
                // Dim opening and closing fences
                addStyle(SpanStyle(color = fenceDimColor), block.openFenceRange.first, block.openFenceRange.last + 1)
                block.closeFenceRange?.let {
                    addStyle(SpanStyle(color = fenceDimColor), it.first, it.last + 1)
                }

                val blockStart = block.fullRange.first
                val blockEnd = block.fullRange.last + 1
                if (blockEnd > blockStart) {
                    renderedCodeRanges.add(blockStart until blockEnd)
                }

                contentStart = block.codeContentRange.first
                contentEnd = contentStart + block.codeText.length
            } else {
                // Delete closing fence first, then opening fence
                delete(block.closeFenceRange.first, block.closeFenceRange.last + 1)
                delete(block.openFenceRange.first, block.openFenceRange.last + 1)

                contentStart = block.openFenceRange.first
                contentEnd = contentStart + block.codeText.length

                if (contentEnd > contentStart) {
                    renderedCodeRanges.add(contentStart until contentEnd)
                }
            }

            if (contentEnd > contentStart) {
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace), contentStart, contentEnd)

                val highlights = highlightCode(block.codeText, block.languageTag, isDarkTheme)
                for (h in highlights) {
                    val s = (contentStart + h.location.start).coerceIn(contentStart, contentEnd)
                    val e = (contentStart + h.location.end).coerceIn(contentStart, contentEnd)
                    if (e > s) {
                        when (h) {
                            is ColorHighlight -> {
                                val opaqueColor = Color(h.rgb or -0x1000000)
                                addStyle(SpanStyle(color = opaqueColor), s, e)
                            }
                            is BoldHighlight -> {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, e)
                            }
                        }
                    }
                }
            }
        }

        renderedCodeRanges.sortBy { it.first }
        onCodeBlockRangesUpdated?.invoke(renderedCodeRanges)

        // Apply general markdown styles to the current buffer
        applyGeneralMarkdownStyles(this, renderedCodeRanges)
    }

    private fun applyGeneralMarkdownStyles(
        buffer: TextFieldBuffer,
        codeRanges: List<IntRange>,
    ) {
        val text = buffer.asCharSequence().toString()

        fun IntRange.intersectsCode(): Boolean =
            codeRanges.any { this.first <= it.last && this.last >= it.first }

        headingRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                val hashes = match.groupValues[1].trimStart()
                val fontSize = when (hashes.length) {
                    1 -> 24.sp
                    2 -> 20.sp
                    3 -> 18.sp
                    else -> 16.sp
                }
                buffer.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        boldRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }
        }

        strikethroughRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        italicRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }
        }

        inlineCodeRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = inlineCodeBackgroundColor),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        linkRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(
                    SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        mentionRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(
                    SpanStyle(color = tagColor, fontWeight = FontWeight.Medium),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        blockquoteRegex.findAll(text).forEach { match ->
            if (!match.range.intersectsCode()) {
                buffer.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }

    internal data class CodeBlock(
        val fullRange: IntRange,
        val languageTag: String,
        val openFenceRange: IntRange,
        val closeFenceRange: IntRange?,
        val codeContentRange: IntRange,
        val codeText: String,
    )

    internal fun findCodeBlocks(text: String): List<CodeBlock> {
        val blocks = mutableListOf<CodeBlock>()
        var i = 0
        while (i < text.length) {
            val fenceIndex = text.indexOf("```", i)
            if (fenceIndex == -1) break

            val openFenceEnd = fenceIndex + 3
            val newlineIndex = text.indexOf('\n', openFenceEnd)
            val langTag: String
            val codeStart: Int

            if (newlineIndex != -1) {
                langTag = text.substring(openFenceEnd, newlineIndex).trim()
                codeStart = newlineIndex + 1
            } else {
                langTag = text.substring(openFenceEnd).trim()
                codeStart = text.length
            }

            val closeFenceIndex = text.indexOf("```", codeStart)
            if (closeFenceIndex != -1) {
                val codeEnd = if (closeFenceIndex > codeStart && text[closeFenceIndex - 1] == '\n') {
                    closeFenceIndex - 1
                } else {
                    closeFenceIndex
                }
                val closeFenceEnd = closeFenceIndex + 3
                val codeText = text.substring(codeStart, codeEnd)
                blocks.add(
                    CodeBlock(
                        fullRange = fenceIndex until closeFenceEnd,
                        languageTag = langTag,
                        openFenceRange = fenceIndex until codeStart,
                        closeFenceRange = (if (codeEnd < closeFenceIndex) codeEnd else closeFenceIndex) until
                            closeFenceEnd,
                        codeContentRange = codeStart until codeEnd,
                        codeText = codeText
                    )
                )
                i = closeFenceEnd
            } else {
                val codeText = text.substring(codeStart)
                blocks.add(
                    CodeBlock(
                        fullRange = fenceIndex until text.length,
                        languageTag = langTag,
                        openFenceRange = fenceIndex until codeStart,
                        closeFenceRange = null,
                        codeContentRange = codeStart until text.length,
                        codeText = codeText
                    )
                )
                break
            }
        }
        return blocks
    }

    companion object {
        private val headingRegex = Regex("(?m)^(\\s{0,3}#{1,6})\\s+.*$")
        private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        private val strikethroughRegex = Regex("~~(.+?)~~")
        private val italicRegex =
            Regex("(?<!\\*)\\*(?!\\*)([^*\\n]+?)(?<!\\*)\\*(?!\\*)|(?<!_)_{1}([^_\\n]+?)_{1}(?!_)")
        private val inlineCodeRegex = Regex("`([^`\\n]+)`")
        private val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
        private val mentionRegex = Regex("(?<=^|\\s)@([A-Za-z0-9_]+)")
        private val blockquoteRegex = Regex("(?m)^\\s*>.*$")
    }
}
