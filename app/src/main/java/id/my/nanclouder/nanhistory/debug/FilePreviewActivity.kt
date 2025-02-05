package id.my.nanclouder.nanhistory.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import java.io.File

class FilePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                FilePreview(intent)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreview(intent: Intent) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var isJson by rememberSaveable { mutableStateOf(false) }

    val content = rememberSaveable {
        intent.getStringExtra("path")?.let {
            val file = File(it)
            if (file.isFile) {
                val fileContent = file.readText()
                isJson = true
                try {
                    val string = Gson().fromJson<Any>(
                        file.readText(),
                        object : TypeToken<Any>() {}.type
                    )
                } catch (_: JsonSyntaxException) {
                    isJson = false
                }
                fileContent
            } else null
        } ?: ""
    }

    var prettyPrint by rememberSaveable { mutableStateOf(false) }

    val lines = (
        if (isJson && prettyPrint) {
            val string = Gson().fromJson<Any>(
                content,
                object : TypeToken<Any>() {}.type
            )

            val gson = GsonBuilder().setPrettyPrinting().create()

            gson.toJson(string)
        } else content
    ).split("\n")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val path = intent.getStringExtra("path")
                    val loadError = @Composable {
                        Text("LoadError")
                    }
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.isFile) {
                            Text(
                                file.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else loadError()
                    } else loadError()
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(PaddingValues(8.dp))
                .fillMaxSize()
        ) {
            if (lines.isNotEmpty()) {
                Column {
                    if (isJson) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pretty print")
                        Switch(
                            checked = prettyPrint,
                            onCheckedChange = {
                                prettyPrint = it
                            }
                        )
                    }
                }
            }
            else {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("File empty", fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextContainer(lines: List<String>, modifier: Modifier = Modifier) {
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    val sliderState = remember { SliderState(value = 0.5f) }

    Column(
        modifier = modifier
    ) {
        SelectionContainer(
            modifier = Modifier
                .padding(bottom = 64.dp)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            LazyColumn(
                modifier = Modifier
                    .horizontalScroll(horizontalScrollState)
                    .fillMaxSize(),
                state = lazyListState
            ) {
                if (lines.isNotEmpty()) {
                    val size = sliderState.value * 1.5 + 0.5
                    items(lines.size) { index ->
                        Text(
                            lines[index],
                            fontFamily = FontFamily.Monospace,
                            fontSize = (size * 2).em,
                            lineHeight = 1.5.em,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(88.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 64.dp)
        ) {
            Text("Font size: ")
            Slider(
                modifier = Modifier
                    .fillMaxSize(),
                state = sliderState
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilePreviewPreview() {
    NanHistoryTheme {
        FilePreview(Intent())
    }
}