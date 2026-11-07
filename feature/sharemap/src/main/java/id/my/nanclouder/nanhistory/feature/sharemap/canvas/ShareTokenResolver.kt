package id.my.nanclouder.nanhistory.feature.sharemap.canvas

import id.my.nanclouder.nanhistory.domain.logic.ShareCanvasLogic
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData

/**
 * Utility for resolving template tokens into dynamic event metadata.
 * Delegates to [ShareCanvasLogic].
 */
object ShareTokenResolver {

    fun calculateElevationGain(locations: List<LocationData>): Double? =
        ShareCanvasLogic.calculateElevationGain(locations)

    fun resolveTokens(
        template: String,
        eventTitle: String,
        dateString: String,
        eventData: HistoryEvent?,
        metricsText: Map<String, String>,
        elevationGainMeters: Double? = null
    ): String = ShareCanvasLogic.resolveTokens(
        template = template,
        eventTitle = eventTitle,
        dateString = dateString,
        eventData = eventData,
        metricsText = metricsText,
        elevationGainMeters = elevationGainMeters
    )
}
