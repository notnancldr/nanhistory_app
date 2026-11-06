package id.my.nanclouder.nanhistory.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Parse formatted text with Markdown syntax:
 * - `# H1` to `###### H6` for headings
 * - `**text**` for bold
 * - `*text*` or `_text_` for italic
 * - `~~text~~` for strikethrough
 * - `` `code` `` for inline code
 * - `[text](url)` for links
 * - `@mention` for tagged mentions
 * - `- item` for unordered lists
 * - `1. item` for ordered lists
 * - `> quote` for blockquotes
 */
fun parseFormattedText(
    text: String,
    tagColor: Color = Color.Blue,
    onTagClick: ((String) -> Unit)? = null,
    tagBackgroundColor: Color = Color.Blue.copy(alpha = 0.15f),
    onLinkClick: ((String) -> Unit)? = null,
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        var i = 0
        var inCodeBlock = false

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()

            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                if (inCodeBlock) {
                    val codeContent = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        codeContent.appendLine(lines[i])
                        i++
                    }
                    if (i < lines.size && lines[i].trimStart().startsWith("```")) {
                        inCodeBlock = false
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = tagBackgroundColor
                        )
                    ) {
                        append(codeContent.toString().trimEnd())
                    }
                    if (i < lines.size - 1) append("\n")
                }
                i++
                continue
            }

            val whitespaceIndent = line.takeWhile { it.isWhitespace() }.length + 2
            val indentation = (6 * whitespaceIndent.toFloat() + 2).sp

            when {
                // Headings: # H1 through ###### H6
                trimmed.startsWith("#") && trimmed.takeWhile { it == '#' }.length in 1..6 &&
                    trimmed.getOrNull(trimmed.takeWhile { it == '#' }.length) == ' ' -> {
                    val level = trimmed.takeWhile { it == '#' }.length
                    val headingText = trimmed.drop(level + 1)
                    val fontSize = when (level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        else -> 16.sp
                    }
                    val headingStyle = ParagraphStyle()
                    withStyle(headingStyle) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize))
                        parseInlineFormatting(headingText, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                        pop()
                    }
                    if (i < lines.size - 1) append("")
                }
                // Blockquote: > quote
                trimmed.startsWith("> ") -> {
                    val quoteText = trimmed.substring(2)
                    val quoteStyle = ParagraphStyle(textIndent = TextIndent(firstLine = 16.sp, restLine = 16.sp))
                    withStyle(quoteStyle) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        parseInlineFormatting(quoteText, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                        pop()
                    }
                    if (i < lines.size - 1) append("")
                }
                // Unordered list: - item or * item
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    withBulletList(bullet = Bullet.Default, indentation = indentation) {
                        withBulletListItem {
                            parseInlineFormatting(
                                trimmed.substring(2),
                                tagColor,
                                onTagClick,
                                tagBackgroundColor,
                                onLinkClick
                            )
                        }
                    }
                    if (i < lines.size - 1) append("")
                }
                // Ordered list: 1. item, 2. item, etc.
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val numberMatch = Regex("^(\\d+)\\.\\s").find(line)
                    if (numberMatch != null) {
                        val number = numberMatch.groupValues[1]
                        val orderedStyle = ParagraphStyle(textIndent = TextIndent(restLine = 24.sp))
                        withStyle(orderedStyle) {
                            append("$number. ")
                            val itemContent = trimmed.substring(numberMatch.value.length)
                            parseInlineFormatting(
                                itemContent,
                                tagColor,
                                onTagClick,
                                tagBackgroundColor,
                                onLinkClick
                            )
                        }
                    }
                    if (i < lines.size - 1) append("")
                }
                else -> {
                    val paragraphStyle = ParagraphStyle()
                    if (i < lines.size - 1) {
                        withStyle(paragraphStyle) {
                            parseInlineFormatting(line, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                        }
                    } else {
                        parseInlineFormatting(line, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                    }
                }
            }
            i++
        }
    }
}

private fun AnnotatedString.Builder.parseInlineFormatting(
    text: String,
    tagColor: Color = Color.Blue,
    onTagClick: ((String) -> Unit)? = null,
    tagBackgroundColor: Color = Color.Blue.copy(alpha = 0.15f),
    onLinkClick: ((String) -> Unit)? = null,
) {
    var i = 0
    while (i < text.length) {
        when {
            // @mention tagging: @name
            text.getOrNull(i - 1)?.isLetterOrDigit() != true && text[i] == '@' &&
                i + 1 < text.length && text[i + 1].isLetterOrDigit() -> {
                val endIndex = findMentionEnd(text, i + 1)
                val mention = text.substring(i + 1, endIndex)
                val displayMention = mention.addSpaceBeforeCapitals()

                if (onTagClick != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "mention:$mention",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = tagColor,
                                    background = tagBackgroundColor
                                )
                            ),
                            linkInteractionListener = { onTagClick(mention) }
                        )
                    ) {
                        append("@$displayMention")
                    }
                } else {
                    pushStyle(SpanStyle(color = tagColor, background = tagBackgroundColor))
                    append("@$displayMention")
                    pop()
                }

                i = endIndex
                continue
            }
            // Markdown link: [text](url)
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && text.getOrNull(closeBracket + 1) == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val linkUrl = text.substring(closeBracket + 2, closeParen)
                        withLink(
                            LinkAnnotation.Url(
                                url = linkUrl,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = tagColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ),
                                linkInteractionListener = onLinkClick?.let { callback ->
                                    { callback(linkUrl) }
                                }
                            )
                        ) {
                            append(linkText)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }
            // Inline code: `code`
            text[i] == '`' -> {
                val endIndex = text.indexOf('`', i + 1)
                if (endIndex != -1) {
                    val codeContent = text.substring(i + 1, endIndex)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = tagBackgroundColor
                        )
                    )
                    append(codeContent)
                    pop()
                    i = endIndex + 1
                    continue
                }
            }
            // Bold: **text**
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val endIndex = text.indexOf("**", i + 2)
                if (endIndex != -1) {
                    val content = text.substring(i + 2, endIndex)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    parseInlineFormatting(content, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                    pop()
                    i = endIndex + 2
                    continue
                }
            }
            // Strikethrough: ~~text~~
            i + 1 < text.length && text[i] == '~' && text[i + 1] == '~' -> {
                val endIndex = text.indexOf("~~", i + 2)
                if (endIndex != -1) {
                    val content = text.substring(i + 2, endIndex)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    parseInlineFormatting(content, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                    pop()
                    i = endIndex + 2
                    continue
                }
            }
            // Italic: *text* or _text_
            text[i] == '*' || text[i] == '_' -> {
                val delimiter = text[i]
                val endIndex = text.indexOf(delimiter, i + 1)
                if (endIndex != -1 && endIndex > i + 1) {
                    val content = text.substring(i + 1, endIndex)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    parseInlineFormatting(content, tagColor, onTagClick, tagBackgroundColor, onLinkClick)
                    pop()
                    i = endIndex + 1
                    continue
                }
            }
        }
        append(text[i])
        i++
    }
}

internal fun String.addSpaceBeforeCapitals(): String {
    return this.mapIndexed { index, char ->
        if (index > 0 && char.isUpperCase()) {
            " $char"
        } else {
            char.toString()
        }
    }.joinToString("")
}

internal fun findMentionEnd(text: String, startIndex: Int): Int {
    var i = startIndex
    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
        i++
    }
    return i
}
