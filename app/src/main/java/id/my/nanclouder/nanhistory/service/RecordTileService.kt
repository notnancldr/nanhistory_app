package id.my.nanclouder.nanhistory.service

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.edit
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.RecordStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordTileService : TileService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("qsTile", MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()

        Log.d("RecordTileService", "onStartListening")
        // Set initial state or update icon/label
        qsTile?.apply {
            label = "Record Event"
            icon = Icon.createWithResource(this@RecordTileService, R.drawable.ic_event_record)
            updateTile()
        }

        coroutineScope.launch {
            RecordService.RecordState.status.collect { status ->
                Log.d("RecordTileService", "status: $status")
                qsTile?.apply {
                    state = when (status) {
                        RecordStatus.RUNNING -> Tile.STATE_ACTIVE
                        RecordStatus.READY -> Tile.STATE_INACTIVE
                        else -> Tile.STATE_UNAVAILABLE
                    }
                    contentDescription = if (status == RecordStatus.RUNNING) {
                        "Recording..."
                    } else {
                        null
                    }
                    updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()

        Log.d("RecordTileService", "onStopListening")
        // Called when tile is no longer visible
        // qsTile?.apply {
        //     state = Tile.STATE_UNAVAILABLE
        //     updateTile()
        // }
    }

    override fun onClick() {
        super.onClick()

        Log.d("RecordTileService", "onClick")

        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_INACTIVE) {
            val intent = Intent(this, RecordService::class.java)
            intent.putExtra("includeAudio", false)
            intent.setAction(RecordService.ACTION_RECORD_START)
            startForegroundService(intent)
        } else {
            val intent = Intent(this, RecordService::class.java)
            intent.setAction(RecordService.ACTION_RECORD_STOP)
            startService(intent)
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()

        Log.d("RecordTileService", "onTileAdded")
        // Called when tile is added
        qsTile.apply {
            state = Tile.STATE_INACTIVE
            label = "Record Event"
            icon = Icon.createWithResource(this@RecordTileService, R.drawable.ic_event_record)
            updateTile()
        }

        sharedPreferences.edit {
            putBoolean("tileAdded", true)
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()

        sharedPreferences.edit {
            putBoolean("tileAdded", false)
        }
        // Called when tile is removed
    }
}