package id.my.nanclouder.nanhistory.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import id.my.nanclouder.nanhistory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(isLoading: Boolean = false, onSearch: ((String) -> Unit), onCancel: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    TopAppBar(
        title = {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchQuery,
                placeholder = {
                    Text("Search events")
                },
                onValueChange = {
                    searchQuery = it
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RectangleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x00000000),
                    unfocusedContainerColor = Color(0x00000000),
                    focusedIndicatorColor = Color(0x00000000),
                    unfocusedIndicatorColor = Color(0x00000000),
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (!isLoading && searchQuery.isNotBlank()) onSearch(searchQuery)
                    }
                )
            )
        },
        actions = {
            if (isLoading) CircularProgressIndicator()
            IconButton(
                enabled = !isLoading && searchQuery.isNotBlank(),
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    if (!isLoading) onSearch(searchQuery)
                    else onCancel()
                }
            ) {
                /*if (!isLoading)*/ Icon(painterResource(R.drawable.ic_search), "Search")
//                else Icon(Icons.Rounded.Close, "Cancel")
            }
        }
    )
}