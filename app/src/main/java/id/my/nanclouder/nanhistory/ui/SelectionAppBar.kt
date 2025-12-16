package id.my.nanclouder.nanhistory.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config

@Composable
fun SelectionAppBar(
    selectedItems: Int,
    onCancel: () -> Unit,
    actions: @Composable (RowScope.() -> Unit)
) {
    val newUI = Config.appearanceNewUI.get(LocalContext.current)
    if (newUI) {
        ModernSelectionAppBar(selectedItems, onCancel, actions)
    } else {
        SelectionAppBar_Old(selectedItems, onCancel, actions = actions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionAppBar_Old(
    selectedItems: Int,
    onCancel: (() -> Unit),
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit) = {}
) {
    val hasSelection = selectedItems > 0
//    val topBarText = if (hasSelection) {
//        "${selectedItems.size} items"
//    } else {
//        "List of items"
//    }
    val topBarText = selectedItems.toString()

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSelectionAppBar(
    selectedCount: Int,
    onResetSelection: () -> Unit,
    actions: @Composable (RowScope.() -> Unit)
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    )

    TopAppBar(
        title = {
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        navigationIcon = {
            IconButton(onClick = onResetSelection) {
                Icon(
                    Icons.Rounded.Close,
                    "Close selection",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        actions = {
            actions(this)
        },
        colors = colors,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
}