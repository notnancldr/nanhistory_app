package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.my.nanclouder.nanhistory.ui.map.maplibre.TripPreviewMap
import id.my.nanclouder.nanhistory.utils.history.LocationData

/**
 * Modern route preview composable for Event Detail screen, powered by MapLibre Compose.
 */
@Composable
fun MapHistoryPreview(
    locations: List<LocationData>,
    modifier: Modifier = Modifier
) {
    TripPreviewMap(
        locations = locations,
        modifier = modifier.fillMaxWidth()
    )
}
