package id.my.nanclouder.nanhistory.core.common.utils

import id.my.nanclouder.nanhistory.domain.logic.CorridorLogic
import id.my.nanclouder.nanhistory.domain.model.Coordinate

fun simplifyPoints(
    points: List<Coordinate>,
    epsilon: Double = 0.00005
): List<Coordinate> = CorridorLogic.simplifyCoordinates(points, epsilon)
