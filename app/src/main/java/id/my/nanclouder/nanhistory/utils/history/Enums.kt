package id.my.nanclouder.nanhistory.utils.history

import id.my.nanclouder.nanhistory.R

enum class EventTypes {
    Point, Range
}

enum class TransportationType(val iconId: Int? = null) {
    Unspecified,
    Walk(R.drawable.ic_directions_walk),
    Bicycle(R.drawable.ic_pedal_bike),
    Motorcycle(R.drawable.ic_motorcycle),
    Car(R.drawable.ic_directions_car),
    Train(R.drawable.ic_train),
    Airplane(R.drawable.ic_flight),
    Ferry(R.drawable.ic_directions_boat);
}