package id.my.nanclouder.nanhistory.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.DateFormatter
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import id.my.nanclouder.nanhistory.lib.history.HistoryDay
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.ui.TagsView
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import java.time.ZonedDateTime

//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun LazyListScope.EventListDay(
//    historyDay: HistoryDay,
//    modifier: Modifier = Modifier,
//    onFavoriteChanged: (Boolean) -> Unit,
//) {
//    var isExpanded by rememberSaveable { mutableStateOf(true) }
//    stickyHeader {
//        EventListHeader(
//            historyDay = historyDay,
//            modifier = modifier,
//            onFavoriteChanged = onFavoriteChanged
//        )
//    }
//
//}