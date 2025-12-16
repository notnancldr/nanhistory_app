package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import id.my.nanclouder.nanhistory.BuildConfig
import id.my.nanclouder.nanhistory.utils.getPackageInfo

class AboutActivity : SubSettingsActivity("About") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        var developerModeEnabled by remember { mutableStateOf(Config.developerModeEnabled.get(applicationContext)) }

        val context = LocalContext.current

        val packageInfo = getPackageInfo(context)
        val buildTime = BuildConfig.BUILD_TIME

        Column(Modifier.verticalScroll(scrollState)) {
            // Logo
            Image(
                modifier = Modifier
                    .height(120.dp)
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.nanhistory_logo_1),
                contentDescription = "NanHistory",
            )

            if (packageInfo != null) Text(
                text = "v${packageInfo.versionName} - build ${packageInfo.longVersionCode}",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (developerModeEnabled) Text(
                text = "BUILD TIME:\n$buildTime",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

             HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // Description
            Text(
                text = "About",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "App for storing history of events and activities. Also for trip location data recording.",
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Information Section
            InfoItem(
                icon = Icons.Filled.Info,
                title = "Developer",
                subtitle = "Nan, member of NaN"
            )

            InfoItem(
                icon = Icons.Filled.Code,
                title = "Source Code",
                subtitle = "Open Source Project"
            )

             HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // Credits
            Text(
                text = "Credits",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            CreditItem("Jetpack Compose", "UI Framework")
            CreditItem("Material Design 3", "Design System")
            CreditItem("Android Framework", "Platform")

            Spacer(modifier = Modifier.height(24.dp))

            // Build Info (shown in developer mode)
            if (developerModeEnabled) {
                 HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "Build Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )

                BuildInfoItem("Build Type", BuildConfig.BUILD_TYPE)
                BuildInfoItem("API Level", android.os.Build.VERSION.SDK_INT.toString())
                BuildInfoItem("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun InfoItem(
        icon: ImageVector,
        title: String,
        subtitle: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun CreditItem(title: String, description: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(40.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun BuildInfoItem(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}