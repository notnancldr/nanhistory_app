package id.my.nanclouder.nanhistory.feature.yearlymap

import androidx.compose.runtime.Immutable

/**
 * Immutable rendering contract for aggregated corridor paths.
 * Equality is determined by instance identity so [version] governs Compose updates.
 *
 * Uses compact indexed arrays for event sets:
 * - [eventIdPool]: Deduplicated pool of event ID strings.
 * - [edgeEventIndices]: Array of IntArray mapping each edge index to indices in [eventIdPool].
 */
@Immutable
class CorridorRender(
    val version: Long = 0L,
    val geoJson: String = EMPTY_FEATURE_COLLECTION,
    val eventIdPool: Array<String> = emptyArray(),
    val edgeEventIndices: Array<IntArray> = emptyArray(),
    private val legacyEventMap: Map<String, Set<String>>? = null
) {
    /**
     * Constructor for legacy callers and tests passing a Map<String, Set<String>> directly.
     */
    constructor(
        version: Long,
        geoJson: String,
        eventIdsByEdge: Map<String, Set<String>>
    ) : this(
        version = version,
        geoJson = geoJson,
        eventIdPool = emptyArray(),
        edgeEventIndices = emptyArray(),
        legacyEventMap = eventIdsByEdge
    )

    /**
     * Resolves the set of event IDs associated with the specified edge index.
     */
    fun getEventIds(edgeIndex: Int): Set<String> {
        if (edgeIndex !in edgeEventIndices.indices) return emptySet()
        val indices = edgeEventIndices[edgeIndex]
        val set = LinkedHashSet<String>(indices.size)
        for (idx in indices) {
            if (idx in eventIdPool.indices) {
                set.add(eventIdPool[idx])
            }
        }
        return set
    }

    /**
     * Resolves the set of event IDs for an edge identifier (e.g. "e0", "e1", or legacy "edge_0").
     */
    fun getEventIdsForEdge(edgeId: String): Set<String> {
        legacyEventMap?.get(edgeId)?.let { return it }
        val idx = edgeId.removePrefix("edge_").removePrefix("e").toIntOrNull()
        return if (idx != null) getEventIds(idx) else emptySet()
    }

    /**
     * Backwards-compatible map view for callers or tests expecting Map<String, Set<String>>.
     */
    val eventIdsByEdge: Map<String, Set<String>> by lazy {
        if (legacyEventMap != null) return@lazy legacyEventMap
        val map = LinkedHashMap<String, Set<String>>(edgeEventIndices.size * 2)
        for (i in edgeEventIndices.indices) {
            val set = getEventIds(i)
            map["e$i"] = set
            map["edge_$i"] = set
        }
        map
    }

    companion object {
        const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""
    }
}
