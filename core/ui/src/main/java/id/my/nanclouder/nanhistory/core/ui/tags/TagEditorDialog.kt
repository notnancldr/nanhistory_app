package id.my.nanclouder.nanhistory.core.ui.tags

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.di.TagDetailContainer
import id.my.nanclouder.nanhistory.core.ui.tags.TagsView
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.model.generateTagId
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.history.HistoryTag
import id.my.nanclouder.nanhistory.utils.history.tint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@Composable
fun TagPreview(
    tag: HistoryTag,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        NanHistoryTheme(
            darkTheme = darkTheme,
            dynamicColor = false
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TagsView(tags = listOf(tag), darkTheme = darkTheme)
                }
            }
        }
    }
}

@Composable
fun TagEditorDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    val useOldUi = Config.appearanceOldUi.get(LocalContext.current)
    if (!useOldUi) {
        TagEditorDialog_New(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            tagId = tagId
        )
    } else {
        TagEditorDialog_Old(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            tagId = tagId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog_New(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    if (state) BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier.fillMaxWidth(0.9f)
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val container = (context.applicationContext as CoreContainerProvider).coreContainer as TagDetailContainer
        val tagRepository = container.tagRepository

        var tag by remember { mutableStateOf<HistoryTag?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var tint by remember { mutableStateOf("#ffffff") }

        val colorController = rememberColorPickerController()

        if (tagId != null) {
            LaunchedEffect(Unit) {
                val loaded = tagRepository.observeTagById(tagId).first()
                tag = loaded
                if (loaded != null) {
                    name = loaded.name
                    description = loaded.description
                    tint = "#" + loaded.tint.toArgb().toUInt().toString(16).padStart(6, '0').takeLast(6)
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }

        val strToColor = {
            tint.removePrefix("#").toLongOrNull(16)?.let {
                Color(0xff000000 + it)
            }
        }

        val isColorValid = strToColor() != null

        var isProcessing by rememberSaveable { mutableStateOf(false) }
        var showColorPicker by remember { mutableStateOf(false) }

        BackHandler {
            onDismissRequest()
        }

        Surface(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier
                    .fillMaxHeight(0.9f)
                    .fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        if (tagId == null) "Create Tag" else "Edit Tag",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        val dummyTag = HistoryTag(
                            id = "dummy",
                            name = name.ifBlank { "Tag name" },
                            description = description,
                            tint = strToColor() ?: Color.Transparent
                        )
                        TagPreview(
                            tag = dummyTag,
                            darkTheme = false,
                            modifier = Modifier.weight(1f)
                        )
                        TagPreview(
                            tag = dummyTag,
                            darkTheme = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tag Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tint,
                            onValueChange = {
                                tint = it.take(1).filter { c -> c == '#' } + it.removePrefix("#").take(6)
                                strToColor()?.let { c ->
                                    colorController.selectByColor(c, false)
                                }
                            },
                            leadingIcon = {
                                if (isColorValid) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = strToColor()!!,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Close,
                                        "Invalid Color",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showColorPicker = !showColorPicker },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Color Picker",
                                        modifier = Modifier.rotate(if (showColorPicker) 180f else 0f),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            label = { Text("Color (Hex)") },
                            supportingText = if (!isColorValid) ({
                                Text(
                                    "Invalid hex color",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }) else null,
                            isError = !isColorValid,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && !showColorPicker,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        AnimatedVisibility(
                            visible = showColorPicker,
                            modifier = Modifier.fillMaxWidth(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    HsvColorPicker(
                                        controller = colorController,
                                        modifier = Modifier.size(100.dp),
                                        onColorChanged = {
                                            if (showColorPicker) tint = "#" + it.hexCode.takeLast(6)
                                        },
                                        initialColor = strToColor()
                                    )
                                    BrightnessSlider(
                                        controller = colorController,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp),
                                        borderRadius = 100.dp,
                                        borderSize = 0.dp,
                                        initialColor = strToColor()
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isProcessing,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        enabled = (
                            name.isNotBlank() &&
                                description.isNotBlank() &&
                                isColorValid &&
                                !isLoading &&
                                !isProcessing
                        ),
                        onClick = {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val newTag = HistoryTag(
                                    id = tag?.id ?: generateTagId(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    created = ZonedDateTime.now(),
                                    tint = strToColor()!!
                                )
                                tagRepository.insertTag(newTag)
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(if (tagId == null) "Create" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog_Old(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    if (state) BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val container = (context.applicationContext as CoreContainerProvider).coreContainer as TagDetailContainer
        val tagRepository = container.tagRepository

        var tag by remember { mutableStateOf<HistoryTag?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var tint by remember { mutableStateOf("#ffffff") }

        val colorController = rememberColorPickerController()

        if (tagId != null) {
            LaunchedEffect(Unit) {
                val loaded = tagRepository.observeTagById(tagId).first()
                tag = loaded
                if (loaded != null) {
                    name = loaded.name
                    description = loaded.description
                    tint = "#" + loaded.tint.toArgb().toUInt().toString(16).padStart(6, '0').takeLast(6)
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }

        val strToColor = {
            tint.removePrefix("#").toLongOrNull(16)?.let {
                Color(0xff000000 + it)
            }
        }

        val isColorValid = strToColor() != null

        var isProcessing by rememberSaveable { mutableStateOf(false) }
        var showColorPicker by remember { mutableStateOf(false) }

        BackHandler {
            onDismissRequest()
        }

        Surface(
            Modifier.clip(RoundedCornerShape(32.dp))
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .fillMaxHeight(0.8f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, "Close")
                    }
                    Text(if (tagId == null) "Add New Tag" else "Edit Tag", style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val textFieldShape = RoundedCornerShape(16.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(96.dp)
                    ) {
                        val dummyTag = HistoryTag(
                            id = "dummy",
                            name = name.ifBlank { "Tag name" },
                            description = description,
                            tint = strToColor() ?: Color.Transparent
                        )
                        TagPreview(
                            tag = dummyTag,
                            darkTheme = false,
                            modifier = Modifier.weight(1f)
                        )
                        TagPreview(
                            tag = dummyTag,
                            darkTheme = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = textFieldShape
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = textFieldShape
                    )
                    OutlinedTextField(
                        value = tint,
                        onValueChange = {
                            tint = it.take(1).filter { c -> c == '#' } + it.removePrefix("#").take(6)
                            strToColor()?.let { c ->
                                colorController.selectByColor(c, false)
                            }
                        },
                        leadingIcon = {
                            if (isColorValid) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = strToColor()!!,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Invalid Color",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { showColorPicker = !showColorPicker }) {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Color Picker",
                                    modifier = Modifier.rotate(if (showColorPicker) 180f else 0f)
                                )
                            }
                        },
                        label = { Text("Color") },
                        supportingText = if (!isColorValid) ({
                            Text("Invalid hex color!")
                        }) else null,
                        isError = !isColorValid,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !showColorPicker,
                        shape = textFieldShape
                    )
                    AnimatedVisibility(
                        visible = showColorPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.height(200.dp)) {
                            Box(Modifier.height(8.dp))
                            HsvColorPicker(
                                controller = colorController,
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.CenterHorizontally),
                                onColorChanged = {
                                    if (showColorPicker) tint = "#" + it.hexCode.takeLast(6)
                                },
                                initialColor = strToColor()
                            )
                            Box(Modifier.height(8.dp))
                            BrightnessSlider(
                                controller = colorController,
                                modifier = Modifier
                                    .height(24.dp),
                                borderRadius = 100.dp,
                                borderSize = 0.dp,
                                initialColor = strToColor()
                            )
                        }
                    }

                    Box(Modifier.height(8.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Button(
                        enabled = (
                            name.isNotBlank() &&
                                description.isNotBlank() &&
                                isColorValid &&
                                !isLoading
                        ),
                        onClick = {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val newTag = HistoryTag(
                                    id = tag?.id ?: generateTagId(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    created = ZonedDateTime.now(),
                                    tint = strToColor()!!
                                )
                                tagRepository.insertTag(newTag)
                                onDismissRequest()
                            }
                        }
                    ) {
                        Text(if (tagId == null) "Add" else "Edit")
                    }
                }
            }
        }
    }
}
