package id.my.nanclouder.nanhistory.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToggleButton(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val buttonPadding = PaddingValues(horizontal = 8.dp)
    if (active) Button(
        onClick = onClick,
        content = content,
        contentPadding = buttonPadding
    )
    else OutlinedButton(
        onClick = onClick,
        content = content,
        contentPadding = buttonPadding
    )
}