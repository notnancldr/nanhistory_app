package id.my.nanclouder.nanhistory.ui.map

import org.osmdroid.util.BoundingBox
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

/**
 * Topologic network edge between nodes [u] and [v] referencing events by indices in the string pool.
 */
data class NetworkEdge(
    val u: Int,
    val v: Int,
    val intermediateNodeIndices: IntArray = IntArray(0),
    val eventIndices: IntArray,
    val count: Int = eventIndices.size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NetworkEdge) return false
        return u == other.u && v == other.v &&
            intermediateNodeIndices.contentEquals(other.intermediateNodeIndices) &&
            eventIndices.contentEquals(other.eventIndices) &&
            count == other.count
    }

    override fun hashCode(): Int {
        var result = u
        result = 31 * result + v
        result = 31 * result + intermediateNodeIndices.contentHashCode()
        result = 31 * result + eventIndices.contentHashCode()
        result = 31 * result + count
        return result
    }
}

/**
 * Pre-computed, immutable snapshot of a merged corridor network for an entire time range.
 */
class CorridorNetwork(
    val nodes: List<MapPoint>,
    val edges: List<NetworkEdge>,
    val eventIds: Array<String>,
    val maxCount: Int,
    val bounds: BoundingBox,
    val totalDistanceMeters: Double
) {

    /**
     * Serializes this network snapshot into a compact, fast binary stream.
     */
    fun writeTo(output: OutputStream) {
        val dos = DataOutputStream(output)
        dos.writeInt(MAGIC)
        dos.writeInt(VERSION)

        // Bounding box & stats
        dos.writeDouble(bounds.latNorth)
        dos.writeDouble(bounds.lonEast)
        dos.writeDouble(bounds.latSouth)
        dos.writeDouble(bounds.lonWest)
        dos.writeDouble(totalDistanceMeters)
        dos.writeInt(maxCount)

        // Event ID pool
        dos.writeInt(eventIds.size)
        for (id in eventIds) {
            dos.writeUTF(id)
        }

        // Nodes
        dos.writeInt(nodes.size)
        for (n in nodes) {
            dos.writeDouble(n.lat)
            dos.writeDouble(n.lon)
        }

        // Edges
        dos.writeInt(edges.size)
        for (e in edges) {
            dos.writeInt(e.u)
            dos.writeInt(e.v)
            dos.writeInt(e.intermediateNodeIndices.size)
            for (idx in e.intermediateNodeIndices) {
                dos.writeInt(idx)
            }
            dos.writeInt(e.eventIndices.size)
            for (idx in e.eventIndices) {
                dos.writeInt(idx)
            }
            dos.writeInt(e.count)
        }
        dos.flush()
    }

    /**
     * Generates a [CorridorRender] applying global time-range normalization across all features.
     */
    fun toCorridorRender(version: Long): CorridorRender {
        if (edges.isEmpty()) {
            return CorridorRender(
                version = version,
                geoJson = CorridorRender.EMPTY_FEATURE_COLLECTION,
                eventIdPool = eventIds,
                edgeEventIndices = emptyArray()
            )
        }

        val edgeEventIndices = Array(edges.size) { edges[it].eventIndices }
        val sb = StringBuilder(edges.size * 180 + 64)
        sb.append("""{"type":"FeatureCollection","features":[""")

        for (i in edges.indices) {
            val edge = edges[i]
            val edgeId = "e$i"
            val t = CorridorColorScale.calculateT(edge.count, maxCount)

            if (i > 0) sb.append(',')

            val tFormatted = String.format(Locale.US, "%.3f", t)
            sb.append("""{"type":"Feature","id":"""")
                .append(edgeId)
                .append("""","properties":{"id":"""")
                .append(edgeId)
                .append("""","t":""")
                .append(tFormatted)
                .append("""},"geometry":{"type":"LineString","coordinates":[""")

            val uPt = nodes[edge.u]
            sb.append('[').append(formatCoord(uPt.lon)).append(',').append(formatCoord(uPt.lat)).append(']')

            for (intermIdx in edge.intermediateNodeIndices) {
                val pt = nodes[intermIdx]
                sb.append(",[").append(formatCoord(pt.lon)).append(',').append(formatCoord(pt.lat)).append(']')
            }

            val vPt = nodes[edge.v]
            sb.append(",[").append(formatCoord(vPt.lon)).append(',').append(formatCoord(vPt.lat)).append(']')

            sb.append("]}}")
        }
        sb.append("]}")

        return CorridorRender(
            version = version,
            geoJson = sb.toString(),
            eventIdPool = eventIds,
            edgeEventIndices = edgeEventIndices
        )
    }

    private fun formatCoord(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    companion object {
        private const val MAGIC = 0x4E414E48 // "NANH"
        private const val VERSION = 1

        /**
         * Reads and reconstructs a [CorridorNetwork] from a binary stream.
         */
        fun readFrom(input: InputStream): CorridorNetwork {
            val dis = DataInputStream(input)
            val magic = dis.readInt()
            require(magic == MAGIC) { "Invalid binary magic: $magic" }
            val version = dis.readInt()
            require(version == VERSION) { "Unsupported binary version: $version" }

            val latNorth = dis.readDouble()
            val lonEast = dis.readDouble()
            val latSouth = dis.readDouble()
            val lonWest = dis.readDouble()
            val totalDistance = dis.readDouble()
            val maxCount = dis.readInt()

            val poolSize = dis.readInt()
            val eventIds = Array(poolSize) { dis.readUTF() }

            val nodeCount = dis.readInt()
            val nodes = ArrayList<MapPoint>(nodeCount)
            for (i in 0 until nodeCount) {
                nodes.add(MapPoint(dis.readDouble(), dis.readDouble()))
            }

            val edgeCount = dis.readInt()
            val edges = ArrayList<NetworkEdge>(edgeCount)
            for (i in 0 until edgeCount) {
                val u = dis.readInt()
                val v = dis.readInt()
                val intermSize = dis.readInt()
                val intermediateIndices = IntArray(intermSize) { dis.readInt() }
                val eventSize = dis.readInt()
                val eventIndices = IntArray(eventSize) { dis.readInt() }
                val count = dis.readInt()
                edges.add(NetworkEdge(u, v, intermediateIndices, eventIndices, count))
            }

            return CorridorNetwork(
                nodes = nodes,
                edges = edges,
                eventIds = eventIds,
                maxCount = maxCount,
                bounds = BoundingBox(latNorth, lonEast, latSouth, lonWest),
                totalDistanceMeters = totalDistance
            )
        }
    }
}
