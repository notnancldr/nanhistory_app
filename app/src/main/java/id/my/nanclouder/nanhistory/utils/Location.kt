package id.my.nanclouder.nanhistory.utils

import id.my.nanclouder.nanhistory.domain.logic.CorridorLogic

fun simplifyPoints(
    points: List<Coordinate>,
    epsilon: Double = 0.00005
): List<Coordinate> = CorridorLogic.simplifyCoordinates(points, epsilon)
