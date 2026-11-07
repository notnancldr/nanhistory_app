package id.my.nanclouder.nanhistory.utils

import android.location.Location
import id.my.nanclouder.nanhistory.core.common.location.calculateDistance
import id.my.nanclouder.nanhistory.core.common.location.calculateSpeed
import id.my.nanclouder.nanhistory.core.common.location.getLocationData as coreGetLocationData
import org.osmdroid.util.GeoPoint
import java.time.ZonedDateTime

fun calculateDistance(startPoint: GeoPoint, endPoint: GeoPoint): Float =
    id.my.nanclouder.nanhistory.core.common.location.calculateDistance(
        startPoint.latitude,
        startPoint.longitude,
        endPoint.latitude,
        endPoint.longitude
    )

fun calculateSpeed(startPoint: GeoPoint, endPoint: GeoPoint, timeHours: Float): Float =
    id.my.nanclouder.nanhistory.core.common.location.calculateDistance(
        startPoint.latitude,
        startPoint.longitude,
        endPoint.latitude,
        endPoint.longitude
    ) / 1000f / timeHours

typealias HistoryLocationData = id.my.nanclouder.nanhistory.domain.model.HistoryLocationData

fun List<id.my.nanclouder.nanhistory.domain.model.LocationData>.getLocationData(): List<HistoryLocationData> =
    this.coreGetLocationData()
