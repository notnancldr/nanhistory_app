package id.my.nanclouder.nanhistory.utils.map

import android.os.Trace
import android.util.Log

/**
 * Performance tracing and metrics logger for the Yearly Map corridor pipeline.
 * Wraps [Trace.beginSection] and [Trace.endSection] for Android system tracing
 * with safe fallbacks for non-mocked JVM unit testing.
 */
object YearlyMapPerfTracer {

    const val TAG: String = "YearlyMapPerf"

    data class StageMetric(
        val stage: String,
        val durationMs: Long,
        val details: String = ""
    )

    @Volatile
    var lastPipelineMetrics: List<StageMetric> = emptyList()
        private set

    fun beginSection(sectionName: String) {
        try {
            Trace.beginSection(sectionName.take(127))
        } catch (_: Throwable) {
            // Safe fallback for un-mocked JVM unit tests
        }
    }

    fun endSection() {
        try {
            Trace.endSection()
        } catch (_: Throwable) {
            // Safe fallback for un-mocked JVM unit tests
        }
    }

    fun <T> trace(sectionName: String, block: () -> T): T {
        beginSection(sectionName)
        val t0 = System.currentTimeMillis()
        try {
            return block()
        } finally {
            val elapsed = System.currentTimeMillis() - t0
            endSection()
            safeLog("[$sectionName] took ${elapsed}ms")
        }
    }

    fun <T> measureStage(stage: String, detailsProvider: (T) -> String = { "" }, block: () -> T): T {
        beginSection(stage)
        val t0 = System.currentTimeMillis()
        val result = block()
        val elapsed = System.currentTimeMillis() - t0
        endSection()
        val details = detailsProvider(result)
        safeLog("Stage [$stage] completed in ${elapsed}ms. $details")
        return result
    }

    fun recordMetrics(metrics: List<StageMetric>) {
        lastPipelineMetrics = metrics
        val summary = metrics.joinToString(" | ") { "${it.stage}: ${it.durationMs}ms (${it.details})" }
        safeLog("Pipeline execution: $summary")
    }

    private fun safeLog(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {
            // Fallback for JVM tests where Log is not mocked
        }
    }
}
