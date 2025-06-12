package id.my.nanclouder.nanhistory.debug

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileListView(
    appBarTitle: String,
    child: String? = null,
    deleteButton: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    var dirTree by rememberSaveable {
        if (child != null)
            mutableStateOf(listOf(File(context.dataDir, child)))
        else
            mutableStateOf(listOf(context.dataDir))
    }

    var files by remember {
        mutableStateOf<List<File>>(listOf())
    }

    val updateView = {
        Log.d("NanHistoryDebug", "updateView")
        files = dirTree.lastOrNull()?.listFiles()?.apply {
            sortByDescending { it.isFile }
        }?.sortedByDescending { it.name } ?: mutableListOf()
    }

    val backHandler = {
        if (dirTree.size > 1) {
            dirTree = dirTree.dropLast(1)
            true
        }
        else false
    }

    BackHandler(
        dirTree.size > 1
    ) { backHandler() }

    updateView()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (dirTree.size <= 1)
                        Text(appBarTitle)
                    else
                        Text(dirTree.last().name)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!backHandler()) context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (files.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues),
                state = lazyListState
            ) {
                stickyHeader(key = 9999999) {
                    ListItem(
                        headlineContent = {
                            Text(dirTree.joinToString("/") { it.name })
                        }
                    )
                }
                itemsIndexed(files, key = { _, item -> item.absolutePath }) { index, item ->
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val audioExtensions = listOf(
                                    "m4a", "mp3", "aac", "3gp"
                                )
                                if (item.isFile && item.extension in audioExtensions) {
                                    val intent = Intent(context, FilePlayerActivity::class.java)
                                    intent.putExtra("path", item.absolutePath)
                                    context.startActivity(intent)
                                }
                                else if (item.isFile) {
                                    val intent = Intent(context, FilePreviewActivity::class.java)
                                    intent.putExtra("path", item.absolutePath)
                                    context.startActivity(intent)
                                }
                                else if (item.isDirectory) {
                                    dirTree = dirTree.toMutableList().apply {
                                        add(item)
                                    }
                                    updateView()
                                }
                            }
                            .animateItem(),
                        leadingContent = {
                            if (item.isDirectory) Icon(painterResource(R.drawable.ic_folder), "Directory")
                            else if (item.isFile) Icon(painterResource(R.drawable.ic_description), "File")
                            else Icon(Icons.Rounded.Warning, "Unknown")
                        },
                        headlineContent = {
                            Text(item.name)
                        },
                        overlineContent = {
                            item.parentFile?.absolutePath?.let {
                                Text(it.replace(context.filesDir.path, ""))
                            }
                        },
                        supportingContent = {
                            if (item.isFile)
                                Text("${item.length() / 1000.0} KB")
                            else if (item.isDirectory) {
                                val contents = item.list()?.size ?: 0
                                Text(
                                    if (contents <= 0) "No item"
                                    else if (contents == 1) "$contents item"
                                    else "$contents items"
                                )
                            }
                        },
                        trailingContent = {
                            if (deleteButton) IconButton(
                                onClick = {
                                    item.delete()
                                    files = files.drop(index)
                                    updateView()
                                }
                            ) {
                                Icon(painterResource(R.drawable.ic_delete), "delete")
                            }
                        }
                    )
                }
            }
        }
        else {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Directory is empty")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FileListPreview() {
    NanHistoryTheme {
        FileListView("App Filesystem")
    }
}