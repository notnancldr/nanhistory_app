package id.my.nanclouder.nanhistory.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.ui.MentionModalState

/**
 * Parse formatted text with markdown-like syntax:
 * - `**text**` for bold
 * - `*text*` or _text_ for italic
 * - `~~text~~` for strikethrough
 * - `@mention` for tagged mentions
 * - `- item` for unordered lists
 * - `1. item` for ordered lists
 * Preserves inline content placeholders (enclosed in square brackets)
 */
fun parseFormattedText(
    text: String,
    tagColor: Color = Color.Blue,
    onTagClick: ((String) -> Unit)? = null,
    tagBackgroundColor: Color = Color.Blue.copy(alpha = 0.15f),
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()
            val whitespaceIndent = line.takeWhile { it.isWhitespace() }.length + 2
            val indentation = (6 * whitespaceIndent.toFloat() + 2).sp

            when {
                // Unordered list: - item
                trimmed.startsWith("- ") -> {
                    withBulletList(bullet = Bullet.Default, indentation = indentation) {
                        withBulletListItem {
                            parseInlineFormatting(trimmed.substring(2), tagColor, onTagClick, tagBackgroundColor)
                        }
                    }
                    if (i < lines.size - 1) append("")
                }
                // Ordered list: 1. item, 2. item, etc.
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val numberMatch = Regex("^(\\d+)\\.\\s").find(line)
                    if (numberMatch != null) {
                        val number = numberMatch.groupValues[1]
                        val orderedStyle = ParagraphStyle(
                            textIndent = TextIndent(restLine = 24.sp),
                        )
                        withStyle(orderedStyle) {
                            append("$number. ")
                            val itemContent = trimmed.substring(numberMatch.value.length)
                            parseInlineFormatting(itemContent, tagColor, onTagClick, tagBackgroundColor)
                        }
                    }
                    if (i < lines.size - 1) append("")
                }
                else -> {
                    // if (line.isNotEmpty()) {
                    //     parseInlineFormatting(line, tagColor, onTagClick, tagBackgroundColor)
                    //     if (i < lines.size - 1) append("\n")
                    // }
                    // else if (i < lines.size - 1) {
                    //     append("\n")
                    // }
                    val paragraphStyle = ParagraphStyle()
                    if (i < lines.size - 1) withStyle(paragraphStyle) {
                        parseInlineFormatting(line, tagColor, onTagClick, tagBackgroundColor)
                    }
                    else parseInlineFormatting(line, tagColor, onTagClick, tagBackgroundColor)
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
) {
    var i = 0
    while (i < text.length) {
        when {
            // @mention tagging: @name
            text.getOrNull(i - 1)?.isLetterOrDigit() != true && text[i] == '@' && i + 1 < text.length && text[i + 1].isLetterOrDigit() -> {
                val endIndex = findMentionEnd(text, i + 1)
                val mention = text.substring(i + 1, endIndex)
                val displayMention = mention.addSpaceBeforeCapitals()
                val startIndex = length

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
                            linkInteractionListener = {
                                onTagClick(mention)
                            }
                        )
                    ) {
                        append("@$displayMention")
                    }
                } else {
                    pushStyle(SpanStyle(
                        color = tagColor,
                        background = tagBackgroundColor
                    ))
                    append("@$displayMention")
                    pop()
                }

                i = endIndex
                continue
            }
            // Bold: **text**
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val endIndex = text.indexOf("**", i + 2)
                if (endIndex != -1) {
                    val content = text.substring(i + 2, endIndex)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(content)
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
                    append(content)
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
                    append(content)
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

private fun String.addSpaceBeforeCapitals(): String {
    return this.mapIndexed { index, char ->
        if (index > 0 && char.isUpperCase()) {
            " $char"
        } else {
            char.toString()
        }
    }.joinToString("")
}

private fun findMentionEnd(text: String, startIndex: Int): Int {
    var i = startIndex
    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
        i++
    }
    return i
}

/**
 * Custom OutlinedTextField that displays formatted text while editing
 * Fully compatible with Material3 OutlinedTextField API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormattedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    colors: androidx.compose.material3.TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    label = label,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    colors = colors,
                    contentPadding = PaddingValues(16.dp),
                    container = {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = shape
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = shape
                                )
                        )
                    }
                )
            }
        )

        if (supportingText != null) {
            supportingText()
        }
    }
}

/**
 * Composable to display formatted text with proper styling
 * Fully compatible with Material3 Text API
 * Default behavior: clicking mentions opens the MentionModal
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
) {
    val defaultMentionHandler: (String) -> Unit = if (mentionModalState != null) {
        { mention -> mentionModalState.value.open(mention) }
    } else {
        { _ -> }
    }

    val handler = onMentionClick ?: defaultMentionHandler

    Text(
        text = parseFormattedText(text, tagColor, handler, tagBackgroundColor),
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
        style = style
    )
}

/**
 * Composable to display formatted text with proper styling
 * Overload for AnnotatedString with formatting support
 * Preserves inline content (icons, etc.) while parsing text formatting
 */
@Composable
fun FormattedText(
    // Plain text to be formatted
    text: String,
    // For additional formatting
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
) {
    val defaultMentionHandler: (String) -> Unit = if (mentionModalState != null) {
        { mention -> mentionModalState.value.open(mention) }
    } else {
        { _ -> }
    }

    val handler = onMentionClick ?: defaultMentionHandler
    // Parse formatting from AnnotatedString text and merge with existing spans
    val parsed = parseFormattedText(text, tagColor, handler, tagBackgroundColor)
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