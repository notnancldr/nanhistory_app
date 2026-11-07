package id.my.nanclouder.nanhistory.core.ui

import id.my.nanclouder.nanhistory.domain.model.TransportationType

val TransportationType.iconId: Int?
    get() = when (this) {
        TransportationType.Walk -> R.drawable.ic_directions_walk
        TransportationType.Bicycle -> R.drawable.ic_pedal_bike
        TransportationType.Motorcycle -> R.drawable.ic_motorcycle
        TransportationType.Car -> R.drawable.ic_directions_car
        TransportationType.Train -> R.drawable.ic_train
        TransportationType.Airplane -> R.drawable.ic_flight
        TransportationType.Ferry -> R.drawable.ic_directions_boat
        TransportationType.Unspecified -> null
    }
