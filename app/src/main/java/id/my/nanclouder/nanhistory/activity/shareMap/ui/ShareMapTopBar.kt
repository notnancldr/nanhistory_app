package id.my.nanclouder.nanhistory.activity.shareMap.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R

/**
 * Top App Bar for the dedicated ShareMap studio screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareMapTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.title_activity_share_map),
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.share_undo)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = stringResource(R.string.share_redo)
                )
            }
            IconButton(onClick = onReset) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.share_reset)
                )
            }
            Button(
                onClick = onExport
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.share_export))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}
