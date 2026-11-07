package id.my.nanclouder.nanhistory.feature.eventdetail.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.feature.eventdetail.R
import id.my.nanclouder.nanhistory.feature.eventdetail.markdown.CustomExpressionOutputTransformation
import id.my.nanclouder.nanhistory.core.ui.FormattedOutlinedTextField
import id.my.nanclouder.nanhistory.domain.logic.graph.GraphExpressionEvaluator

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
    val expressionState = rememberTextFieldState(initialExpression)
    var unit by remember(initialUnit) { mutableStateOf(initialUnit) }

    val expressionText = expressionState.text.toString()
    val validationResult = remember(expressionText) {
        GraphExpressionEvaluator.validate(expressionText)
    }
    val isValid = validationResult.isSuccess
    val errorMessage = validationResult.exceptionOrNull()?.message

    val expressionOutputTransformation = remember {
        CustomExpressionOutputTransformation()
    }

    fun insertToken(token: String) {
        val sel = expressionState.selection
        val start = sel.min
        val end = sel.max
        val textToInsert = if (start > 0 && expressionState.text[start - 1] != ' ' && !token.startsWith(" ")) {
            " $token"
        } else {
            token
        }
        expressionState.edit {
            replace(start, end, textToInsert)
            selection = TextRange(start + textToInsert.length)
        }
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
                FormattedOutlinedTextField(
                    state = expressionState,
                    label = { Text(stringResource(R.string.expression_field_label)) },
                    isError = expressionText.isNotBlank() && !isValid,
                    outputTransformation = expressionOutputTransformation,
                    supportingText = {
                        if (expressionText.isNotBlank()) {
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
                    val properties = listOf(
                        "speed", "altitude", "acceleration", "bearing",
                        "accuracy", "distance", "latitude", "longitude"
                    )
                    properties.forEach { prop ->
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
                    onApply(expressionText.trim(), unit.trim())
                }
            ) {
                Text(stringResource(R.string.expression_apply))
            }
        }
    )
}
