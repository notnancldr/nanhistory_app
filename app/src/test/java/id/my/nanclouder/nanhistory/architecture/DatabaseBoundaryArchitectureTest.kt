package id.my.nanclouder.nanhistory.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces database boundaries.
 * Non-data, non-db classes must NOT import [AppDatabase] or [AppDao] directly.
 * As vertical slices are migrated, files are removed from [allowedNonDataCallers]
 * until this allowlist is completely empty.
 */
class DatabaseBoundaryArchitectureTest {

    // Shrinking allowlist of legacy callers outside db/ and data/
    private val allowedNonDataCallers = mutableSetOf(
        // Debug
        "FilePlayerActivity.kt",
        // UI
        "MainView.kt",
        "EventListViewModel.kt",
        "YearlyMapLogic.kt",
        "YearlyMapScreen.kt",
        "EventListItem.kt",
        "EventListHeader.kt",
        "EventList.kt",
        "Components.kt",
        // Utils
        "Events.kt",
        "Repair.kt",
        "Model.kt",
        "Version2.kt",
        "Migrations.kt",
        "CheckUnlinked.kt",
        "LegacyImport.kt",
        // Activities
        "DetailContent.kt",
        "OldDetailContent.kt",
        "EventLocationActivity.kt",
        "EditEventActivity.kt",
        "EventLocationOldUi.kt",
        "ShareMapActivity.kt",
        // Workers & Services
        "AutoDeleteWorker.kt",
        "CorridorPrecomputeWorker.kt",
        "YearlyMapWorker.kt",
        "DataProcessService.kt",
        "RecordService.kt",
        // State
        "EventLocationViewModel.kt",
        "YearlyMapViewModel.kt",
        "YearlyMapViewModelFactory.kt",
        "ReplayController.kt",
        "ShareMapViewModel.kt"
    )

    @Test
    fun non_data_classes_only_access_database_if_allowlisted() {
        val srcDir = listOf(
            File("src/main/java/id/my/nanclouder/nanhistory"),
            File("app/src/main/java/id/my/nanclouder/nanhistory")
        ).firstOrNull { it.exists() } ?: return

        val dbImportRegex = Regex("""^import\s+id\.my\.nanclouder\.nanhistory\.db\.(AppDatabase|AppDao)\b""")
        val unallowedViolations = mutableListOf<String>()

        srcDir.walkTopDown()
            .filter { it.extension == "kt" }
            .filterNot {
                it.path.contains("/db/") ||
                it.path.contains("/data/") ||
                it.path.contains("/di/")
            }
            .forEach { file ->
                if (!allowedNonDataCallers.contains(file.name)) {
                    file.useLines { lines ->
                        lines.forEachIndexed { index, line ->
                            if (line.matches(dbImportRegex)) {
                                unallowedViolations.add("${file.name}:${index + 1} -> $line")
                            }
                        }
                    }
                }
            }

        assertTrue(
            "Non-data files importing AppDatabase/AppDao without allowlist entry:\n${unallowedViolations.joinToString("\n")}",
            unallowedViolations.isEmpty()
        )
    }
}
