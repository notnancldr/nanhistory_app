package id.my.nanclouder.nanhistory.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFormatTest {

    @Test
    fun testPreprocessMentionsBasic() {
        val input = "Hello @Alice and @Bob"
        val output = preprocessMentionsForMarkdown(input)
        assertEquals("Hello [@Alice](mention:Alice) and [@Bob](mention:Bob)", output)
    }

    @Test
    fun testPreprocessMentionsCamelCaseAndUnderscore() {
        val input = "Tagged: @CamelCaseTag and @snake_case_tag"
        val output = preprocessMentionsForMarkdown(input)
        val expected = "Tagged: [@Camel Case Tag](mention:CamelCaseTag) and [@snake_case_tag](mention:snake_case_tag)"
        assertEquals(expected, output)
    }

    @Test
    fun testPreprocessMentionsIgnoredInCode() {
        val input = "Outside @tag `inside @inline_code` and ```\ninside @code_block\n```"
        val output = preprocessMentionsForMarkdown(input)
        assertEquals(
            "Outside [@tag](mention:tag) `inside @inline_code` and ```\ninside @code_block\n```",
            output
        )
    }

    @Test
    fun testPreprocessMentionsEmailIgnored() {
        val input = "Contact user@example.com for help"
        val output = preprocessMentionsForMarkdown(input)
        assertEquals(input, output)
    }

    @Test
    fun testParseFormattedTextHeadings() {
        val input = "# Heading 1\n## Heading 2\n### Heading 3"
        val parsed = parseFormattedText(input)

        val boldSpans = parsed.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue(boldSpans.isNotEmpty())

        val h1Span = boldSpans.firstOrNull { it.item.fontSize == 24.sp }
        assertNotNull(h1Span)

        val h2Span = boldSpans.firstOrNull { it.item.fontSize == 20.sp }
        assertNotNull(h2Span)

        val h3Span = boldSpans.firstOrNull { it.item.fontSize == 18.sp }
        assertNotNull(h3Span)
    }

    @Test
    fun testParseFormattedTextBoldItalicStrikethrough() {
        val input = "**bold text** and *italic text* and ~~strike text~~"
        val parsed = parseFormattedText(input)

        val hasBold = parsed.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = parsed.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        val hasStrike = parsed.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough }

        assertTrue(hasBold)
        assertTrue(hasItalic)
        assertTrue(hasStrike)
    }

    @Test
    fun testParseFormattedTextInlineCodeAndLinks() {
        val input = "Run `code_command` or visit [Open](https://nanclouder.my.id)"
        val parsed = parseFormattedText(input)

        assertTrue(parsed.text.contains("code_command"))
        assertTrue(parsed.text.contains("Open"))
    }

    @Test
    fun testParseFormattedTextBlockquoteAndLists() {
        val input = "> quoted line\n- list item 1\n* list item 2\n1. ordered item"
        val parsed = parseFormattedText(input)

        assertTrue(parsed.text.contains("quoted line"))
        assertTrue(parsed.text.contains("list item 1"))
        assertTrue(parsed.text.contains("ordered item"))
    }

    @Test
    fun testMarkdownVisualTransformationIdentityMapping() {
        val transformation = MarkdownVisualTransformation()
        val original = AnnotatedString("# Title\n**bold** `code` @mention [link](url)")
        val transformed = transformation.filter(original)

        assertEquals(original.text, transformed.text.text)
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(5, transformed.offsetMapping.originalToTransformed(5))
        assertEquals(original.length, transformed.offsetMapping.originalToTransformed(original.length))
        assertEquals(5, transformed.offsetMapping.transformedToOriginal(5))
        assertTrue(transformed.text.spanStyles.isNotEmpty())
    }

    @Test
    fun testSplitMarkdownSegmentsMultiBlock() {
        val markdown = """
            # Intro
            Some description.
            ```kotlin
            val x = 10
            ```
            Middle text.
            ```python
            y = 20
            ```
            Conclusion text.
        """.trimIndent()

        val segments = splitMarkdownSegments(markdown)
        assertEquals(5, segments.size)

        assertTrue(segments[0] is MarkdownSegment.Text)
        assertTrue((segments[0] as MarkdownSegment.Text).content.contains("# Intro"))

        assertTrue(segments[1] is MarkdownSegment.Code)
        val code1 = segments[1] as MarkdownSegment.Code
        assertEquals("kotlin", code1.languageTag)
        assertEquals("val x = 10", code1.code)

        assertTrue(segments[2] is MarkdownSegment.Text)
        assertTrue((segments[2] as MarkdownSegment.Text).content.contains("Middle text."))

        assertTrue(segments[3] is MarkdownSegment.Code)
        val code2 = segments[3] as MarkdownSegment.Code
        assertEquals("python", code2.languageTag)
        assertEquals("y = 20", code2.code)

        assertTrue(segments[4] is MarkdownSegment.Text)
        assertTrue((segments[4] as MarkdownSegment.Text).content.contains("Conclusion text."))
    }

    @Test
    fun testSplitMarkdownSegmentsNoCodeBlocks() {
        val markdown = "Just plain markdown with *italics* and **bold**."
        val segments = splitMarkdownSegments(markdown)

        assertEquals(1, segments.size)
        assertTrue(segments[0] is MarkdownSegment.Text)
        assertEquals(markdown, (segments[0] as MarkdownSegment.Text).content)
    }

    @Test
    fun testSplitMarkdownSegmentsEmpty() {
        val segments = splitMarkdownSegments("")
        assertTrue(segments.isEmpty())
    }

    @Test
    fun testSplitMarkdownSegmentsUnclosed() {
        val markdown = "Header\n```rust\nfn main() {}"
        val segments = splitMarkdownSegments(markdown)

        assertEquals(2, segments.size)
        assertTrue(segments[0] is MarkdownSegment.Text)
        assertTrue(segments[1] is MarkdownSegment.Code)
        val code = segments[1] as MarkdownSegment.Code
        assertEquals("rust", code.languageTag)
        assertEquals("fn main() {}", code.code)
    }
}
