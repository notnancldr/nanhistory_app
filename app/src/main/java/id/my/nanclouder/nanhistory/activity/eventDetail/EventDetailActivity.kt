package id.my.nanclouder.nanhistory.activity.eventDetail

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

typealias EventDetailActivity = id.my.nanclouder.nanhistory.feature.eventdetail.EventDetailActivity

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
