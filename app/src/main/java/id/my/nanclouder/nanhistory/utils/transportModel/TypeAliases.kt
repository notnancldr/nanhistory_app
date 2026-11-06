package id.my.nanclouder.nanhistory.utils.transportModel

typealias TransportMode = id.my.nanclouder.nanhistory.domain.logic.transport.TransportMode
typealias ScoringStrategy = id.my.nanclouder.nanhistory.domain.logic.transport.ScoringStrategy
typealias CalibrationModel = id.my.nanclouder.nanhistory.domain.logic.transport.CalibrationModel

fun computeBatchMetrics(history: List<id.my.nanclouder.nanhistory.utils.HistoryLocationData>) =
    id.my.nanclouder.nanhistory.domain.logic.transport.computeBatchMetrics(history)

fun mergeModels(old: CalibrationModel, new: CalibrationModel, weightCap: Float? = null) =
    id.my.nanclouder.nanhistory.domain.logic.transport.mergeModels(old, new, weightCap)

fun computeCoastingMetrics(history: List<id.my.nanclouder.nanhistory.utils.HistoryLocationData>) =
    id.my.nanclouder.nanhistory.domain.logic.transport.computeCoastingMetrics(history)
