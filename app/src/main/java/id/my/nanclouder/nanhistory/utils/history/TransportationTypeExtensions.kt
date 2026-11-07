package id.my.nanclouder.nanhistory.utils.history

import id.my.nanclouder.nanhistory.core.ui.iconId
import id.my.nanclouder.nanhistory.domain.model.TransportationType

val TransportationType.iconId: Int?
    get() = (this as TransportationType).iconId
