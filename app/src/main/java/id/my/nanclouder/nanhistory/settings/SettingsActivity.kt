package id.my.nanclouder.nanhistory.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.NewUIComponentActivity

class SettingsActivity : NewUIComponentActivity() {
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
        val newUI = Config.appearanceNewUI.getCache()
        if (newUI) View_New()
        else View_Old()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun View_Old() {
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
                        Text("Location, shake to start, etc.")
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
                        Text("Storage usage, clear logs data")
                    }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, AutoDeleteSettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_delete_filled), "Auto-delete settings")
                    },
                    headlineContent = {
                        Text("Auto-delete")
                    },
                    supportingContent = {
                        Text("Event auto deletion")
                    }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, AppearanceSettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_settings_filled), "Experimental settings")
                    },
                    headlineContent = {
                        Text("Appearance")
                    },
                    supportingContent = {
                        Text("Appearance settings and new UI option")
                    }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, ExperimentalSettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_experiment_filled), "Experimental settings")
                    },
                    headlineContent = {
                        Text("Experimental")
                    },
                    supportingContent = {
                        Text("Experimental options")
                    }
                )
                if (Config.developerModeEnabled.get(applicationContext)) ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, DeveloperOptionsActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_code), "Developer Options")
                    },
                    headlineContent = {
                        Text("Developer Options")
                    },
                    supportingContent = {
                        Text("Options for developer")
                    }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        val intent = Intent(context, AboutActivity::class.java)
                        context.startActivity(intent)
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.Info, "About")
                    },
                    headlineContent = {
                        Text("About")
                    },
                    supportingContent = {
                        Text("About this app")
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun View_New() {
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsItemCard(
                        icon = painterResource(R.drawable.ic_stop_filled),
                        iconDescription = "Event recording settings",
                        title = "Event Recording",
                        description = "Location, permissions, and more",
                        onClick = {
                            val intent = Intent(context, RecordSettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    SettingsItemCard(
                        icon = painterResource(R.drawable.ic_storage),
                        iconDescription = "Storage settings",
                        title = "Storage",
                        description = "Storage usage and clear data",
                        onClick = {
                            val intent = Intent(context, StorageSettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    SettingsItemCard(
                        icon = painterResource(R.drawable.ic_delete_filled),
                        iconDescription = "Auto-delete settings",
                        title = "Auto-delete",
                        description = "Event auto deletion settings",
                        onClick = {
                            val intent = Intent(context, AutoDeleteSettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    SettingsItemCard(
                        icon = painterResource(R.drawable.ic_settings_filled),
                        iconDescription = "Appearance settings",
                        title = "Appearance",
                        description = "Theme and UI customization",
                        onClick = {
                            val intent = Intent(context, AppearanceSettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    SettingsItemCard(
                        icon = painterResource(R.drawable.ic_experiment_filled),
                        iconDescription = "Experimental settings",
                        title = "Experimental",
                        description = "Try new experimental features",
                        onClick = {
                            val intent = Intent(context, ExperimentalSettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                if (Config.developerModeEnabled.get(applicationContext)) {
                    item {
                        SettingsItemCard(
                            icon = painterResource(R.drawable.ic_code),
                            iconDescription = "Developer Options",
                            title = "Developer Options",
                            description = "Advanced options for developers",
                            onClick = {
                                val intent = Intent(context, DeveloperOptionsActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                item {
                    SettingsItemCard(
                        icon = painterResource(R.drawable.ic_info),
                        iconDescription = "About",
                        title = "About",
                        description = "About this app",
                        onClick = {
                            val intent = Intent(context, AboutActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun SettingsItemCard(
        icon: Painter,
        iconDescription: String,
        title: String,
        description: String,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = icon,
                            contentDescription = iconDescription,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Navigate",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
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