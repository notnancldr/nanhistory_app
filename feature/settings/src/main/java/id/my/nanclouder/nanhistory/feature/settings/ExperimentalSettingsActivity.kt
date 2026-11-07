package id.my.nanclouder.nanhistory.feature.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import id.my.nanclouder.nanhistory.feature.settings.R
import id.my.nanclouder.nanhistory.config.Config

class ExperimentalSettingsActivity : SubSettingsActivity("Experimental") {
    @Composable
    override fun ColumnScope.Content() {
        CategoryHeader(
            icon = painterResource(R.drawable.ic_circle_filled),
            iconDescription = "Audio Record",
            title = "Audio Record"
        )
        SettingsSwitch(
            title = "Audio Record",
            description = "Activate audio record feature.",
            configValue = Config.experimentalAudioRecord
        )

        CategoryHeader(
            icon = painterResource(R.drawable.ic_circle_filled),
            iconDescription = stringResource(R.string.settings_experimental_category_markdown),
            title = stringResource(R.string.settings_experimental_category_markdown)
        )
        SettingsSwitch(
            title = stringResource(R.string.settings_experimental_markdown_input_title),
            description = stringResource(R.string.settings_experimental_markdown_input_desc),
            configValue = Config.experimentalMarkdownInputHighlighting
        )
        SettingsSwitch(
            title = stringResource(R.string.settings_experimental_markdown_viewer_title),
            description = stringResource(R.string.settings_experimental_markdown_viewer_desc),
            configValue = Config.experimentalMarkdownViewerSyntaxHighlighting
        )

        CategoryHeader(
            icon = painterResource(R.drawable.ic_circle_filled),
            iconDescription = stringResource(R.string.settings_experimental_category_map),
            title = stringResource(R.string.settings_experimental_category_map)
        )
        SettingsSwitch(
            title = stringResource(R.string.settings_experimental_maplibre_sharemap_title),
            description = stringResource(R.string.settings_experimental_maplibre_sharemap_desc),
            configValue = Config.experimentalMaplibreShareMap
        )
        SettingsDropdown(
            configValue = Config.shareMapExportEngine,
            enumClass = id.my.nanclouder.nanhistory.config.ShareMapExportEngine::class.java,
            title = stringResource(R.string.settings_experimental_share_export_engine_title),
            description = stringResource(R.string.settings_experimental_share_export_engine_desc)
        )
        SettingsSwitch(
            title = stringResource(R.string.settings_experimental_advanced_share_style_title),
            description = stringResource(R.string.settings_experimental_advanced_share_style_desc),
            configValue = Config.experimentalAdvancedShareStyle
        )
    }
}
