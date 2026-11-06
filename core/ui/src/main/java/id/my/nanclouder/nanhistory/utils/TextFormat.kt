package id.my.nanclouder.nanhistory.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import dev.jeziellago.compose.markdowntext.MarkdownText
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.ui.MentionModalState
import id.my.nanclouder.nanhistory.ui.component.CodeBlockCard

/**
 * Preprocesses Markdown text to convert @mentions outside code blocks/inline code into Markdown link format.
 */
fun preprocessMentionsForMarkdown(text: String): String {
    val builder = StringBuilder()
    var i = 0
    var inCodeBlock = false
    var inInlineCode = false

    while (i < text.length) {
        if (text.startsWith("```", i)) {
            inCodeBlock = !inCodeBlock
            builder.append("```")
            i += 3
            continue
        }

        if (!inCodeBlock && text[i] == '`') {
            inInlineCode = !inInlineCode
            builder.append('`')
            i++
            continue
        }

        if (!inCodeBlock && !inInlineCode) {
            val prevChar = text.getOrNull(i - 1)
            val isWordBoundary = prevChar == null || !prevChar.isLetterOrDigit()
            if (isWordBoundary && text[i] == '@' && i + 1 < text.length && text[i + 1].isLetterOrDigit()) {
                val endIndex = findMentionEnd(text, i + 1)
                val mention = text.substring(i + 1, endIndex)
                val displayMention = mention.addSpaceBeforeCapitals()
                builder.append("[@$displayMention](mention:$mention)")
                i = endIndex
                continue
            }
        }

        builder.append(text[i])
        i++
    }
    return builder.toString()
}

/**
 * Composable to display Markdown formatted text using compose-markdown.
 * Supports headings, bold, italic, code blocks, tables, links, and @mentions.
 */
@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    tagColor: Color = MaterialTheme.colorScheme.primary,
    tagBackgroundColor: Color = Color.Unspecified,
    onMentionClick: ((String) -> Unit)? = null,
    mentionModalState: MutableState<MentionModalState>? = null,
    onLinkClick: ((String) -> Unit)? = null,
) {
    val defaultMentionHandler: (String) -> Unit = if (mentionModalState != null) {
        { mention -> mentionModalState.value.open(mention) }
    } else {
        { _ -> }
    }

    val handler = onMentionClick ?: defaultMentionHandler
    val uriHandler = LocalUriHandler.current
    val processedText = remember(text) { preprocessMentionsForMarkdown(text) }

    val resolvedColor = if (color != Color.Unspecified) {
        color
    } else {
        style.color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    }

    val mergedStyle = style.copy(
        color = resolvedColor,
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        fontStyle = fontStyle ?: style.fontStyle,
        fontWeight = fontWeight ?: style.fontWeight,
        fontFamily = fontFamily ?: style.fontFamily,
        letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else style.letterSpacing,
        textDecoration = textDecoration ?: style.textDecoration,
        textAlign = textAlign ?: style.textAlign,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight,
    )

    val linkHandler = { url: String ->
        if (url.startsWith("mention:")) {
            val mention = url.removePrefix("mention:")
            handler(mention)
        } else if (onLinkClick != null) {
            onLinkClick(url)
        } else {
            try {
                uriHandler.openUri(url)
            } catch (_: Exception) {
            }
        }
    }

    val syntaxHighlightingEnabled by Config.experimentalMarkdownViewerSyntaxHighlighting.getState()
    val canRenderCodeBlocks = syntaxHighlightingEnabled && maxLines == Int.MAX_VALUE
    val segments = remember(processedText, canRenderCodeBlocks) {
        if (canRenderCodeBlocks) splitMarkdownSegments(processedText) else null
    }

    if (segments != null && segments.any { it is MarkdownSegment.Code }) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (segment in segments) {
                when (segment) {
                    is MarkdownSegment.Text -> {
                        if (segment.content.isNotBlank()) {
                            MarkdownText(
                                markdown = segment.content,
                                style = mergedStyle,
                                linkColor = tagColor,
                                onLinkClicked = linkHandler
                            )
                        }
                    }
                    is MarkdownSegment.Code -> {
                        CodeBlockCard(
                            code = segment.code,
                            languageTag = segment.languageTag
                        )
                    }
                }
            }
        }
    } else {
        MarkdownText(
            markdown = processedText,
            modifier = modifier,
            maxLines = maxLines,
            style = mergedStyle,
            linkColor = tagColor,
            onLinkClicked = linkHandler
        )
    }
}

/**
 * Segment of a Markdown document: either regular Markdown text or a fenced code block.
 */
sealed interface MarkdownSegment {
    data class Text(val content: String) : MarkdownSegment
    data class Code(val code: String, val languageTag: String) : MarkdownSegment
}

/**
 * Splits Markdown text into sequential text and code block segments.
 */
fun splitMarkdownSegments(text: String): List<MarkdownSegment> {
    if (text.isEmpty()) return emptyList()

    val segments = mutableListOf<MarkdownSegment>()
    var currentIndex = 0

    while (currentIndex < text.length) {
        val fenceStart = text.indexOf("```", currentIndex)
        if (fenceStart == -1) {
            val remaining = text.substring(currentIndex)
            if (remaining.isNotEmpty()) {
                segments.add(MarkdownSegment.Text(remaining))
            }
            break
        }

        if (fenceStart > currentIndex) {
            val textBefore = text.substring(currentIndex, fenceStart)
            if (textBefore.isNotEmpty()) {
                segments.add(MarkdownSegment.Text(textBefore))
            }
        }

        val openFenceEnd = fenceStart + 3
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
            val codeText = text.substring(codeStart, codeEnd)
            segments.add(MarkdownSegment.Code(code = codeText, languageTag = langTag))
            currentIndex = closeFenceIndex + 3
            if (currentIndex < text.length && text[currentIndex] == '\n') {
                currentIndex++
            }
        } else {
            val codeText = text.substring(codeStart)
            segments.add(MarkdownSegment.Code(code = codeText, languageTag = langTag))
            break
        }
    }

    return segments
}

/**
 * Composable to display formatted text with proper styling.
 * Overload for AnnotatedString with inline content support (icons, placeholders, etc.).
 */
@Composable
fun FormattedText(
    text: String,
    annotatedString: AnnotatedString.Builder.(AnnotatedString) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    style: TextStyle = LocalTextStyle.current,
    tagColor: Color = MaterialTheme.colorScheme.primary,
    tagBackgroundColor: Color = Color.Unspecified,
    onMentionClick: ((String) -> Unit)? = null,
    mentionModalState: MutableState<MentionModalState>? = null,
    onLinkClick: ((String) -> Unit)? = null,
) {
    val defaultMentionHandler: (String) -> Unit = if (mentionModalState != null) {
        { mention -> mentionModalState.value.open(mention) }
    } else {
        { _ -> }
    }

    val handler = onMentionClick ?: defaultMentionHandler
    val parsed = parseFormattedText(text, tagColor, handler, tagBackgroundColor, onLinkClick)
    val formatted = buildAnnotatedString { annotatedString(parsed) }

    Text(
        text = formatted,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        style = style
    )
}
