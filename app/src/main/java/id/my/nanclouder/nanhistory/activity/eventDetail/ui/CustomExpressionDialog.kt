package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.graph.GraphExpressionEvaluator

/**
 * Dialog for entering and validating custom expressions for computed data series.
 */
@Composable
fun CustomExpressionDialog(
    initialExpression: String,
    initialUnit: String,
    onDismissRequest: () -> Unit,
    onApply: (expression: String, unit: String) -> Unit
) {
    var expression by remember(initialExpression) { mutableStateOf(initialExpression) }
    var unit by remember(initialUnit) { mutableStateOf(initialUnit) }

    val validationResult = remember(expression) {
        GraphExpressionEvaluator.validate(expression)
    }
    val isValid = validationResult.isSuccess
    val errorMessage = validationResult.exceptionOrNull()?.message

    fun insertToken(token: String) {
        expression = if (expression.isBlank()) token else "$expression $token"
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.expression_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it },
                    label = { Text(stringResource(R.string.expression_field_label)) },
                    isError = expression.isNotBlank() && !isValid,
                    supportingText = {
                        if (expression.isNotBlank()) {
                            if (isValid) {
                                Text(
                                    text = stringResource(R.string.expression_syntax_valid),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Text(
                                    text = errorMessage ?: stringResource(R.string.expression_syntax_invalid),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.expression_unit_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(R.string.expression_helper_properties),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("speed", "altitude", "acceleration", "bearing", "accuracy", "distance", "latitude", "longitude").forEach { prop ->
                        FilterChip(
                            selected = false,
                            onClick = { insertToken(prop) },
                            label = { Text(prop) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.expression_helper_siblings),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("[-1]", "[+1]", "[-2]", "[+2]", "property(\"\")").forEach { tok ->
                        FilterChip(
                            selected = false,
                            onClick = { insertToken(tok) },
                            label = { Text(tok) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.expression_helper_operators),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("+", "-", "*", "/", "^", "avg(, 5)", "abs()", "sqrt()", "min(,)", "max(,)").forEach { op ->
                        FilterChip(
                            selected = false,
                            onClick = { insertToken(op) },
                            label = { Text(op) }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.color_cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                onClick = {
                    onApply(expression.trim(), unit.trim())
                }
            ) {
                Text(stringResource(R.string.expression_apply))
            }
        }
    )
}
