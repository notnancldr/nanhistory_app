package id.my.nanclouder.nanhistory.activity.shareMap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.R

data class ExportResolutionPreset(val name: String, val baseDimensionPx: Int)

val EXPORT_RESOLUTIONS = listOf(
    ExportResolutionPreset("1080p (FHD)", 1080),
    ExportResolutionPreset("1440p (2K)", 1440),
    ExportResolutionPreset("2160p (4K)", 2160)
)

/**
 * Bottom Sheet for triggering image export and direct sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareExportSheet(
    isExporting: Boolean,
    onSaveToPictures: (format: String, baseDimensionPx: Int) -> Unit,
    onShareDirectly: (format: String, baseDimensionPx: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFormat by remember { mutableStateOf("PNG") }
    var selectedResolution by remember { mutableStateOf(EXPORT_RESOLUTIONS[0]) }

    ModalBottomSheet(
        onDismissRequest = { if (!isExporting) onDismiss() },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.share_export),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss, enabled = !isExporting) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Image Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.share_format), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PNG", "JPEG").forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format) }
                        )
                    }
                }
            }

            // Resolution Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.share_resolution), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EXPORT_RESOLUTIONS.forEach { res ->
                        FilterChip(
                            selected = selectedResolution == res,
                            onClick = { selectedResolution = res },
                            label = { Text(res.name, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            if (isExporting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Rendering Image...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveToPictures(selectedFormat, selectedResolution.baseDimensionPx) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share_save_gallery))
                    }
                    Button(
                        onClick = { onShareDirectly(selectedFormat, selectedResolution.baseDimensionPx) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share_share_image))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
