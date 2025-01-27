package id.my.nanclouder.nanhistory.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                DebugView()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugView() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Debug Options")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {

                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
        ) {
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_folder_filled), "Filesystem")
                },
                headlineContent = {
                    Text("App Filesystem")
                },
                modifier = Modifier.clickable {
                    val intent = Intent(context, FileSystemActivity::class.java)
                    context.startActivity(intent)
                }
            )
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_description_filled), "Logs")
                },
                headlineContent = {
                    Text("App Logs")
                },
                modifier = Modifier.clickable {
                    val intent = Intent(context, LogListActivity::class.java)
                    context.startActivity(intent)
                }
            )
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_settings_filled), "Configs")
                },
                headlineContent = {
                    Text("App Configs")
                },
                modifier = Modifier.clickable {
                    val intent = Intent(context, ConfigViewActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DebugPreview() {
    NanHistoryTheme {
        DebugView()
    }
}