package id.my.nanclouder.nanhistory.state

import id.my.nanclouder.nanhistory.domain.logic.ReplayMath
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.LocationData
import java.time.ZonedDateTime

typealias ReplayTelemetry = id.my.nanclouder.nanhistory.domain.logic.ReplayTelemetry

/**
 * Immutable state representing the route replay engine.
 */
data class ReplayUiState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentVirtualTime: ZonedDateTime? = null,
    val currentTelemetry: ReplayTelemetry? = null,
    val speedMultiplier: Float = 1.0f,
    val isFollowCamera: Boolean = true,
    val isImmersive: Boolean = false,
    val allLocations: List<LocationData> = emptyList(),
    val traveledLocations: List<LocationData> = emptyList(),
    val dwellPoints: List<LocationData> = emptyList()
)

typealias ReplayInterpolator = ReplayMath
