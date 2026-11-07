package id.my.nanclouder.nanhistory.feature.maplocation

import id.my.nanclouder.nanhistory.core.common.location.calculateDistance
import id.my.nanclouder.nanhistory.core.common.location.calculateSpeed
import id.my.nanclouder.nanhistory.core.common.location.getLocationData
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import org.osmdroid.util.GeoPoint

fun Coordinate.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)


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

fun List<GeoPoint>.toBoundingBox(): org.osmdroid.util.BoundingBox? {
    if (isEmpty()) return null
    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    for (point in this) {
        if (point.latitude < minLat) minLat = point.latitude
        if (point.latitude > maxLat) maxLat = point.latitude
        if (point.longitude < minLon) minLon = point.longitude
        if (point.longitude > maxLon) maxLon = point.longitude
    }
    return org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon)
}

