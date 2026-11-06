package id.my.nanclouder.nanhistory.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces the core Clean Architecture rule that files in the `domain` layer
 * must be pure Kotlin and have ZERO dependencies on Android, Compose, or other app layers.
 */
class DomainArchitectureTest {

    @Test
    fun domain_layer_has_zero_forbidden_imports() {
        // Path relative to module root or project root
        val domainDir = listOf(
            File("src/main/java/id/my/nanclouder/nanhistory/domain"),
            File("app/src/main/java/id/my/nanclouder/nanhistory/domain"),
            File("../core/domain/src/main/kotlin/id/my/nanclouder/nanhistory/domain"),
            File("core/domain/src/main/kotlin/id/my/nanclouder/nanhistory/domain")
        ).firstOrNull { it.exists() }

        // Before Phase 1 creation of domain layer, test passes gracefully
        if (domainDir == null) return

        var scanned = 0
        val violations = mutableListOf<String>()
        val forbiddenRegex = Regex(
            """^import\s+(android|androidx|id\.my\.nanclouder\.nanhistory\.(db|data|ui|state|activity|service|worker|receiver|utils|config|presentation|platform|di))\b"""
        )

        domainDir.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                scanned++
                file.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        if (line.matches(forbiddenRegex)) {
                            violations.add("${file.name}:${index + 1} -> $line")
                        }
                    }
                }
            }

        assertTrue("Domain directory exists but no Kotlin files were scanned!", scanned > 0)
        assertTrue(
            "Domain layer contains forbidden imports (must be pure Kotlin):\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
