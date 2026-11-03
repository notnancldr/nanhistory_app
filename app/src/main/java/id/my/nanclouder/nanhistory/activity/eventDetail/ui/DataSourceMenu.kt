package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.graph.StandardProperty

/**
 * Dropdown menu for choosing a standard telemetry property or initiating custom expression.
 */
@Composable
fun DataSourceMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectStandardProperty: (property: StandardProperty) -> Unit,
    onCustomClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        val standardItems = listOf(
            StandardProperty.SPEED to (R.string.property_speed to Icons.Rounded.Speed),
            StandardProperty.ACCELERATION to (R.string.property_acceleration to Icons.Rounded.Speed),
            StandardProperty.ALTITUDE to (R.string.property_altitude to Icons.Rounded.Landscape),
            StandardProperty.BEARING to (R.string.property_bearing to Icons.Rounded.Navigation),
            StandardProperty.DISTANCE to (R.string.property_distance to Icons.Rounded.Straighten)
        )

        standardItems.forEach { (prop, meta) ->
            val (nameRes, icon) = meta
            DropdownMenuItem(
                text = { Text(stringResource(nameRes), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    onSelectStandardProperty(prop)
                    onDismissRequest()
                }
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.property_custom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onCustomClick()
            }
        )
    }
}
