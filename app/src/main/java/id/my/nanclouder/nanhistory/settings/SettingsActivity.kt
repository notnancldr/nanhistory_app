package id.my.nanclouder.nanhistory.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                // A surface container using the 'background' color from the theme
                View()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun View() {
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text("Settings")
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
            ) {
                ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, RecordSettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                         Icon(painterResource(R.drawable.ic_stop_filled), "Event recording settings")
                    },
                    headlineContent = {
                        Text("Event Recording")
                    },
                    supportingContent = {
                        Text("Location, etc.")
                    }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, StorageSettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                         Icon(painterResource(R.drawable.ic_storage), "Storage settings")
                    },
                    headlineContent = {
                        Text("Storage")
                    },
                    supportingContent = {
                        Text("Storage usage, clear logs data.")
                    }
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun SettingsPreview() {
        NanHistoryTheme {
            View()
        }
    }
}

