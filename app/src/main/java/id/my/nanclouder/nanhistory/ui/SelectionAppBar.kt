package id.my.nanclouder.nanhistory.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionAppBar(
    selectedItems: List<HistoryEvent>,
    onCancel: (() -> Unit),
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit) = {}
) {
    val hasSelection = selectedItems.isNotEmpty()
//    val topBarText = if (hasSelection) {
//        "${selectedItems.size} items"
//    } else {
//        "List of items"
//    }
    val topBarText = selectedItems.size.toString()

    TopAppBar(
        title = {
            Text(topBarText, fontWeight = FontWeight.Medium)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Cancel")
            }
        },
        actions = actions,
        modifier = modifier
    )
}