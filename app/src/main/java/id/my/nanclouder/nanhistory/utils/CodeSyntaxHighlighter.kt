package id.my.nanclouder.nanhistory.utils

import androidx.collection.LruCache
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

/**
 * Cache key identifying tokenized code blocks.
 */
data class SyntaxCacheKey(
    val language: SyntaxLanguage,
    val isDarkTheme: Boolean,
    val code: String,
)

/**
 * In-memory LRU cache storing tokenized code highlights across typing passes.
 */
val syntaxHighlightCache = LruCache<SyntaxCacheKey, List<CodeHighlight>>(maxSize = 64)

/**
 * Resolves Markdown language tags and abbreviations to supported [SyntaxLanguage] enums.
 */
fun resolveSyntaxLanguage(tag: String?): SyntaxLanguage {
    if (tag.isNullOrBlank()) return SyntaxLanguage.DEFAULT
    return when (tag.trim().lowercase()) {
        "kt", "kotlin" -> SyntaxLanguage.KOTLIN
        "java" -> SyntaxLanguage.JAVA
        "py", "python" -> SyntaxLanguage.PYTHON
        "js", "javascript" -> SyntaxLanguage.JAVASCRIPT
        "ts", "typescript" -> SyntaxLanguage.TYPESCRIPT
        "rs", "rust" -> SyntaxLanguage.RUST
        "c" -> SyntaxLanguage.C
        "cpp", "c++", "cc", "cxx" -> SyntaxLanguage.CPP
        "cs", "csharp", "c#" -> SyntaxLanguage.CSHARP
        "sh", "bash", "zsh", "shell" -> SyntaxLanguage.SHELL
        "go", "golang" -> SyntaxLanguage.GO
        "rb", "ruby" -> SyntaxLanguage.RUBY
        "swift" -> SyntaxLanguage.SWIFT
        "dart" -> SyntaxLanguage.DART
        "coffee", "coffeescript" -> SyntaxLanguage.COFFEESCRIPT
        "perl", "pl" -> SyntaxLanguage.PERL
        else -> SyntaxLanguage.DEFAULT
    }
}

/**
 * Tokenizes [code] for the given [languageTag] and theme, retrieving from LRU cache when possible.
 */
fun highlightCode(
    code: String,
    languageTag: String?,
    isDarkTheme: Boolean,
): List<CodeHighlight> {
    if (code.isEmpty()) return emptyList()
    val language = resolveSyntaxLanguage(languageTag)
    val key = SyntaxCacheKey(language, isDarkTheme, code)

    syntaxHighlightCache.get(key)?.let { return it }

    val theme = SyntaxThemes.default(darkMode = isDarkTheme)
    val highlights = Highlights.Builder()
        .code(code)
        .language(language)
        .theme(theme)
        .build()
        .getHighlights()

    syntaxHighlightCache.put(key, highlights)
    return highlights
}
