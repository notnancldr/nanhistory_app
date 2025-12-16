package id.my.nanclouder.nanhistory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.ui.main.EventListViewModel
import id.my.nanclouder.nanhistory.utils.backgroundTagColor
import id.my.nanclouder.nanhistory.utils.borderTagColor
import id.my.nanclouder.nanhistory.utils.textTagColor
import kotlin.math.max

@Composable
fun SearchAppBar(
    viewModel: EventListViewModel,
    onSearch: ((String, List<String>) -> Unit),
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showTags: Boolean = true,
    onChange: ((String, List<String>) -> Unit)? = null
) {
    val newUI = Config.appearanceNewUI.get(LocalContext.current)
    if (newUI) {
        SearchAppBar_New(
            viewModel = viewModel,
            onSearch = onSearch,
            modifier = modifier,
            isLoading = isLoading,
            onChange = onChange,
            showTags = showTags
        )
    }
    else {
        SearchAppBar_Old(
            isLoading = isLoading,
            onSearch = {
                onSearch(it, emptyList())
            },
            onCancel = onCancel,
            onChange = {
                onChange?.invoke(it, emptyList())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchAppBar_New(
    viewModel: EventListViewModel,
    onSearch: ((String, List<String>) -> Unit),
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showTags: Boolean = true,
    onChange: ((String, List<String>) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedTagIds = remember { mutableStateListOf<String>() }

    val availableTags by viewModel.tags.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    val darkMode = isSystemInDarkTheme()

    val maxShownTags = 8 + max(selectedTagIds.size - 5, 0)

    val filteredTags = availableTags.filter { tag ->
        val selected = selectedTagIds.contains(tag.id)
        !(!selected && !tag.name.contains(searchQuery, ignoreCase = true))
    }
    val remaining = max(filteredTags.size - maxShownTags, 0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Modern Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 24.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                TextField(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    value = searchQuery,
                    placeholder = {
                        Text(
                            "Search events...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onValueChange = {
                        searchQuery = it
                        onChange?.invoke(it, selectedTagIds)
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RectangleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (!isLoading && searchQuery.isNotBlank()) {
                                onSearch(searchQuery, selectedTagIds)
                            }
                        }
                    )
                )

                // Action buttons
                AnimatedVisibility(visible = searchQuery.isNotBlank() || selectedTagIds.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            selectedTagIds.clear()
                            onChange?.invoke("", selectedTagIds)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Filter Tags Section
        AnimatedVisibility(
            visible = filteredTags.isNotEmpty() && showTags,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {

                filteredTags.sortedByDescending { selectedTagIds.contains(it.id) }.take(maxShownTags).forEach { tag ->
                    val selected = selectedTagIds.contains(tag.id)

                    Surface(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!selectedTagIds.remove(tag.id)) {
                                    selectedTagIds.add(tag.id)
                                    searchQuery = ""
                                }
                                onChange?.invoke(searchQuery, selectedTagIds)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) tag.tint.backgroundTagColor(darkMode) else Color.Transparent,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) tag.tint.borderTagColor(darkMode) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else null,
                                color = if (selected) tag.tint.textTagColor(darkMode) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                if (remaining > 0) Text(
                    text = "$remaining more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar_Old(
    isLoading: Boolean = false,
    onSearch: ((String) -> Unit),
    onCancel: () -> Unit,
    onChange: ((String) -> Unit)? = null
) {
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
                    onChange?.invoke(it)
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