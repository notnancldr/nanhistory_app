package id.my.nanclouder.nanhistory.ui.main

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.BackupActivity
import id.my.nanclouder.nanhistory.TrashActivity
import id.my.nanclouder.nanhistory.debug.DebugActivity
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.settings.SettingsActivity
import id.my.nanclouder.nanhistory.lib.getPackageInfo
import kotlinx.coroutines.delay

@Composable
fun DrawerContent() {
    val sectionModifier = Modifier.padding(PaddingValues(12.dp))
    val itemModifier = Modifier.height(56.dp)

    val context = LocalContext.current

    val drawerItemText = @Composable { text: String ->
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.W700)
    }

    var isDeveloper by remember {
        mutableStateOf(Config.developerModeEnabled.get(context))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            isDeveloper = Config.developerModeEnabled.get(context)
        }
    }

    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Column(
            modifier = sectionModifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Image(
                    modifier = Modifier
                        .height(88.dp)
                        .padding(16.dp),
                    painter = painterResource(id = R.drawable.nanhistory_logo_1),
                    contentDescription = "NanHistory",
                )
                NavigationDrawerItem(
                    modifier = itemModifier,
                    icon = { Icon(painterResource(R.drawable.ic_delete_filled), "Deleted") },
                    label = { drawerItemText("Trash") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, TrashActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                NavigationDrawerItem(
                    modifier = itemModifier,
                    icon = { Icon(painterResource(R.drawable.ic_cloud_upload_filled), "Backup") },
                    label = { drawerItemText("Backup") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, BackupActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                NavigationDrawerItem(
                    modifier = itemModifier,
                    icon = { Icon(painterResource(R.drawable.ic_settings_filled), "Settings") },
                    label = { drawerItemText("Settings") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                if (isDeveloper) NavigationDrawerItem(
                    modifier = itemModifier,
                    icon = { Icon(painterResource(R.drawable.ic_code), "Debug") },
                    label = { drawerItemText("Debug") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, DebugActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(16.dp)
            ) {
                var clickCount by remember { mutableIntStateOf(0) }
                val packageInfo = getPackageInfo(context)
                if (packageInfo != null) Text(
                    "v${packageInfo.versionName} - build ${packageInfo.longVersionCode}",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clickable {
                            clickCount++
                            if (clickCount == 10) {
                                Toast.makeText(context, "Developer mode enabled", Toast.LENGTH_SHORT).show()
                                Config.developerModeEnabled.set(context, true)
                                isDeveloper = true
                            }
                        }
                )
            }
        }
    }
}