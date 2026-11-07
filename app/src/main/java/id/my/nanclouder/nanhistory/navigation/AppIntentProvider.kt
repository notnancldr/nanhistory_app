package id.my.nanclouder.nanhistory.navigation

import android.content.Context
import android.content.Intent
import id.my.nanclouder.nanhistory.activity.EditEventActivity
import id.my.nanclouder.nanhistory.feature.maplocation.EventLocationActivity
import id.my.nanclouder.nanhistory.feature.tag.TagDetailActivity
import id.my.nanclouder.nanhistory.feature.trash.TrashActivity
import id.my.nanclouder.nanhistory.feature.yearlymap.YearlyMapActivity
import id.my.nanclouder.nanhistory.activity.eventDetail.EventDetailActivity
import id.my.nanclouder.nanhistory.feature.sharemap.ShareMapActivity
import id.my.nanclouder.nanhistory.core.common.navigation.IntentProvider
import id.my.nanclouder.nanhistory.feature.timeline.MainActivity
import id.my.nanclouder.nanhistory.debug.DebugActivity
import id.my.nanclouder.nanhistory.debug.FilePlayerActivity
import id.my.nanclouder.nanhistory.debug.FilePreviewActivity
import id.my.nanclouder.nanhistory.feature.settings.BackupActivity
import id.my.nanclouder.nanhistory.feature.settings.SettingsActivity

class AppIntentProvider : IntentProvider {
    override fun createEventDetailIntent(context: Context, eventId: String, path: String?): Intent {
        return Intent(context, EventDetailActivity::class.java).apply {
            putExtra("eventId", eventId)
            path?.let { putExtra("path", it) }
        }
    }

    override fun createEditEventIntent(
        context: Context,
        eventId: String?,
        previousEventOf: String?,
        nextEventOf: String?
    ): Intent {
        return Intent(context, EditEventActivity::class.java).apply {
            eventId?.let { putExtra("eventId", it) }
            previousEventOf?.let { putExtra("previousEventOf", it) }
            nextEventOf?.let { putExtra("nextEventOf", it) }
        }
    }

    override fun createEventLocationIntent(context: Context, eventId: String, cutMode: Boolean): Intent {
        return Intent(context, EventLocationActivity::class.java).apply {
            putExtra("eventId", eventId)
            putExtra("cutMode", cutMode)
        }
    }

    override fun createShareMapIntent(context: Context, eventId: String): Intent {
        return Intent(context, ShareMapActivity::class.java).apply {
            putExtra("eventId", eventId)
        }
    }

    override fun createTagDetailIntent(context: Context, tagId: String): Intent {
        return Intent(context, TagDetailActivity::class.java).apply {
            putExtra("tagId", tagId)
        }
    }

    override fun createYearlyMapIntent(context: Context): Intent {
        return Intent(context, YearlyMapActivity::class.java)
    }

    override fun createTrashIntent(context: Context): Intent {
        return Intent(context, TrashActivity::class.java)
    }

    override fun createSettingsIntent(context: Context): Intent {
        return Intent(context, SettingsActivity::class.java)
    }

    override fun createBackupIntent(context: Context): Intent {
        return Intent(context, BackupActivity::class.java)
    }

    override fun createMainIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java)
    }

    override fun createFilePlayerIntent(context: Context, path: String): Intent {
        return Intent(context, FilePlayerActivity::class.java).apply {
            putExtra("path", path)
        }
    }

    override fun createFilePreviewIntent(context: Context, path: String): Intent {
        return Intent(context, FilePreviewActivity::class.java).apply {
            putExtra("path", path)
        }
    }

    override fun createDebugIntent(context: Context): Intent {
        return Intent(context, DebugActivity::class.java)
    }
}
