package id.my.nanclouder.nanhistory.utils

import dev.snipme.highlights.model.SyntaxLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeSyntaxHighlighterTest {

    @Test
    fun testResolveSyntaxLanguageAliases() {
        assertEquals(SyntaxLanguage.KOTLIN, resolveSyntaxLanguage("kt"))
        assertEquals(SyntaxLanguage.KOTLIN, resolveSyntaxLanguage("kotlin"))
        assertEquals(SyntaxLanguage.PYTHON, resolveSyntaxLanguage("py"))
        assertEquals(SyntaxLanguage.PYTHON, resolveSyntaxLanguage("python"))
        assertEquals(SyntaxLanguage.JAVASCRIPT, resolveSyntaxLanguage("js"))
        assertEquals(SyntaxLanguage.JAVASCRIPT, resolveSyntaxLanguage("javascript"))
        assertEquals(SyntaxLanguage.TYPESCRIPT, resolveSyntaxLanguage("ts"))
        assertEquals(SyntaxLanguage.TYPESCRIPT, resolveSyntaxLanguage("typescript"))
        assertEquals(SyntaxLanguage.RUST, resolveSyntaxLanguage("rs"))
        assertEquals(SyntaxLanguage.RUST, resolveSyntaxLanguage("rust"))
        assertEquals(SyntaxLanguage.CPP, resolveSyntaxLanguage("cpp"))
        assertEquals(SyntaxLanguage.CPP, resolveSyntaxLanguage("c++"))
        assertEquals(SyntaxLanguage.CSHARP, resolveSyntaxLanguage("cs"))
        assertEquals(SyntaxLanguage.CSHARP, resolveSyntaxLanguage("c#"))
        assertEquals(SyntaxLanguage.SHELL, resolveSyntaxLanguage("sh"))
        assertEquals(SyntaxLanguage.SHELL, resolveSyntaxLanguage("bash"))
        assertEquals(SyntaxLanguage.GO, resolveSyntaxLanguage("go"))
        assertEquals(SyntaxLanguage.SWIFT, resolveSyntaxLanguage("swift"))
        assertEquals(SyntaxLanguage.DEFAULT, resolveSyntaxLanguage("unknown_lang"))
        assertEquals(SyntaxLanguage.DEFAULT, resolveSyntaxLanguage(null))
        assertEquals(SyntaxLanguage.DEFAULT, resolveSyntaxLanguage(""))
    }

    @Test
    fun testHighlightCodeTokenizationAndCaching() {
        val code = "val count: Int = 42\n// A comment\nval text = \"hello\""
        val highlights1 = highlightCode(code, "kt", isDarkTheme = true)

        assertFalse(highlights1.isEmpty())

        // Ensure subsequent call hits LRU cache returning the exact same instance
        val highlights2 = highlightCode(code, "kt", isDarkTheme = true)
        assertSame(highlights1, highlights2)
    }

    @Test
    fun testFindCodeBlocksParsing() {
        val transformation = MarkdownOutputTransformation()
        val markdown = """
            # Heading
            ```kotlin
            val a = 1
            ```
            Some text
            ```python
            b = 2
            ```
        """.trimIndent()

        val blocks = transformation.findCodeBlocks(markdown)
        assertEquals(2, blocks.size)

        val block1 = blocks[0]
        assertEquals("kotlin", block1.languageTag)
        assertEquals("val a = 1", block1.codeText)
        assertNotNull(block1.closeFenceRange)

        val block2 = blocks[1]
        assertEquals("python", block2.languageTag)
        assertEquals("b = 2", block2.codeText)
        assertNotNull(block2.closeFenceRange)
    }

    @Test
    fun testFindCodeBlocksUnclosed() {
        val transformation = MarkdownOutputTransformation()
        val markdown = "```rust\nfn main() {}"
        val blocks = transformation.findCodeBlocks(markdown)

        assertEquals(1, blocks.size)
        assertEquals("rust", blocks[0].languageTag)
        assertEquals("fn main() {}", blocks[0].codeText)
        assertEquals(null, blocks[0].closeFenceRange)
    }

    @Test
    fun testFindCodeBlocksOpeningOnly() {
        val transformation = MarkdownOutputTransformation()
        val markdown = "```"
        val blocks = transformation.findCodeBlocks(markdown)

        assertEquals(1, blocks.size)
        assertEquals("", blocks[0].languageTag)
        assertEquals("", blocks[0].codeText)
        assertEquals(null, blocks[0].closeFenceRange)
        assertEquals(0 until 3, blocks[0].openFenceRange)
        assertEquals(0 until 3, blocks[0].fullRange)
    }

    @Test
    fun testFindCodeBlocksEmpty() {
        val transformation = MarkdownOutputTransformation()
        val blocks = transformation.findCodeBlocks("")
        assertTrue(blocks.isEmpty())
    }
}
