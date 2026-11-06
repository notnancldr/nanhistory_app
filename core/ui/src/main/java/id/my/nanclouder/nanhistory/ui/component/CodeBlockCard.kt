package id.my.nanclouder.nanhistory.ui.component

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import id.my.nanclouder.nanhistory.core.ui.R
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.highlightCode
import kotlinx.coroutines.delay

/**
 * Interactive card displaying a Markdown code block with language title, copy button,
 * line numbers, and syntax highlighting.
 */
@Composable
fun CodeBlockCard(
    code: String,
    languageTag: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {
    var isCopied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    if (isCopied) {
        LaunchedEffect(Unit) {
            delay(2000L)
            isCopied = false
        }
    }

    val lines = remember(code) { code.lines() }
    val lineCount = lines.size
    val lineNumbersText = remember(lineCount) {
        (1..lineCount).joinToString("\n")
    }

    val highlights = remember(code, languageTag, isDarkTheme) {
        highlightCode(code, languageTag, isDarkTheme)
    }

    val styledCode = remember(code, highlights) {
        buildAnnotatedString {
            append(code)
            for (h in highlights) {
                val start = h.location.start.coerceIn(0, code.length)
                val end = h.location.end.coerceIn(0, code.length)
                if (end > start) {
                    when (h) {
                        is ColorHighlight -> {
                            val opaqueColor = Color(h.rgb or -0x1000000)
                            addStyle(SpanStyle(color = opaqueColor), start, end)
                        }
                        is BoldHighlight -> {
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        }
                    }
                }
            }
        }
    }

    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column {
            // Header bar with language title and copy action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatLanguageLabel(languageTag),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        isCopied = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Crossfade(targetState = isCopied, label = "copyCrossfade") { copied ->
                        if (copied) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.code_block_copied),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.code_block_copy),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Code content with line numbers and horizontal scrolling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Line numbers gutter
                Text(
                    text = lineNumbersText,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Vertical gutter divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Highlighted code
                Text(
                    text = styledCode,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false
                )
            }
        }
    }
}

private fun formatLanguageLabel(tag: String): String = when (tag.lowercase().trim()) {
    "kt", "kotlin" -> "Kotlin"
    "py", "python" -> "Python"
    "js", "javascript" -> "JavaScript"
    "ts", "typescript" -> "TypeScript"
    "rs", "rust" -> "Rust"
    "cpp", "c++" -> "C++"
    "c" -> "C"
    "cs", "csharp", "c#" -> "C#"
    "java" -> "Java"
    "go", "golang" -> "Go"
    "swift" -> "Swift"
    "rb", "ruby" -> "Ruby"
    "sh", "bash", "shell", "zsh" -> "Shell"
    "json" -> "JSON"
    "xml" -> "XML"
    "html" -> "HTML"
    "sql" -> "SQL"
    "md", "markdown" -> "Markdown"
    "" -> "Code"
    else -> tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Preview(name = "Light Mode")
@Composable
private fun CodeBlockCardLightPreview() {
    NanHistoryTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            CodeBlockCard(
                code = "fun main() {\n    val greeting = \"Hello, World!\"\n    println(greeting)\n}",
                languageTag = "kt",
                isDarkTheme = false
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CodeBlockCardDarkPreview() {
    NanHistoryTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            CodeBlockCard(
                code = "def calculate_total(items):\n    return sum(item.price for item in items)",
                languageTag = "py",
                isDarkTheme = true
            )
        }
    }
}
