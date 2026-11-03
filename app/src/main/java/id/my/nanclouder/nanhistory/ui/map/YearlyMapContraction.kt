package id.my.nanclouder.nanhistory.ui.map

/**
 * Topologic contraction of degree-2 vertices in corridor network graphs.
 */
object YearlyMapContraction {

    /**
     * Contracts sequences of degree-2 vertices that share the identical [eventIds] set into single polyline edges.
     */
    fun contractDegree2Nodes(
        rawEdges: Map<Pair<MapPoint, MapPoint>, Set<String>>
    ): List<CorridorSegment> {
        if (rawEdges.isEmpty()) return emptyList()

        val adjacency = LinkedHashMap<MapPoint, MutableList<Pair<MapPoint, Set<String>>>>()
        for ((edge, eventIds) in rawEdges) {
            adjacency.getOrPut(edge.first) { mutableListOf() }.add(Pair(edge.second, eventIds))
            adjacency.getOrPut(edge.second) { mutableListOf() }.add(Pair(edge.first, eventIds))
        }

        fun isDegree2Contractible(pt: MapPoint): Boolean {
            val incident = adjacency[pt] ?: return false
            if (incident.size != 2) return false
            val (n1, ids1) = incident[0]
            val (n2, ids2) = incident[1]
            return n1 != n2 && ids1 == ids2
        }

        val visitedDirectedEdges = HashSet<Pair<MapPoint, MapPoint>>()
        val contractedSegments = mutableListOf<CorridorSegment>()

        // 1. Start traversals from non-degree-2 anchor nodes (intersections or dead-ends)
        for ((node, incident) in adjacency) {
            if (isDegree2Contractible(node)) continue

            for ((neighbor, eventIds) in incident) {
                val dirEdge = Pair(node, neighbor)
                if (visitedDirectedEdges.contains(dirEdge)) continue

                visitedDirectedEdges.add(dirEdge)
                visitedDirectedEdges.add(Pair(neighbor, node))

                val polyline = mutableListOf(node, neighbor)
                var prev = node
                var curr = neighbor

                while (isDegree2Contractible(curr)) {
                    val nextEdges = adjacency[curr]!!
                    val nextPair = if (nextEdges[0].first == prev) nextEdges[1] else nextEdges[0]
                    val nextNode = nextPair.first

                    val stepDir = Pair(curr, nextNode)
                    visitedDirectedEdges.add(stepDir)
                    visitedDirectedEdges.add(Pair(nextNode, curr))

                    polyline.add(nextNode)
                    prev = curr
                    curr = nextNode
                    if (curr == node) break
                }

                contractedSegments.add(
                    CorridorSegment(
                        start = polyline.first(),
                        end = polyline.last(),
                        eventIds = eventIds,
                        count = eventIds.size,
                        intermediatePoints = polyline.subList(1, polyline.size - 1)
                    )
                )
            }
        }

        // 2. Handle isolated closed cycles where every vertex is degree 2
        for ((node, incident) in adjacency) {
            for ((neighbor, eventIds) in incident) {
                val dirEdge = Pair(node, neighbor)
                if (visitedDirectedEdges.contains(dirEdge)) continue

                visitedDirectedEdges.add(dirEdge)
                visitedDirectedEdges.add(Pair(neighbor, node))

                val polyline = mutableListOf(node, neighbor)
                var prev = node
                var curr = neighbor

                while (curr != node && isDegree2Contractible(curr)) {
                    val nextEdges = adjacency[curr] ?: break
                    val nextPair = if (nextEdges[0].first == prev) nextEdges[1] else nextEdges[0]
                    val nextNode = nextPair.first

                    val stepDir = Pair(curr, nextNode)
                    visitedDirectedEdges.add(stepDir)
                    visitedDirectedEdges.add(Pair(nextNode, curr))

                    polyline.add(nextNode)
                    prev = curr
                    curr = nextNode
                }

                contractedSegments.add(
                    CorridorSegment(
                        start = polyline.first(),
                        end = polyline.last(),
                        eventIds = eventIds,
                        count = eventIds.size,
                        intermediatePoints = polyline.subList(1, polyline.size - 1)
                    )
                )
            }
        }

        return contractedSegments
    }
}
