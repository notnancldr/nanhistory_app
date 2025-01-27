package id.my.nanclouder.nanhistory.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.LogData
import id.my.nanclouder.nanhistory.lib.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.generateEventId
import id.my.nanclouder.nanhistory.lib.history.generateSignature
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.roundToInt

class RecordService : Service() {
    companion object {
        fun isRunning(context: Context): Boolean {
            val sharedPreferences =
                context.getSharedPreferences("recordEvent", Context.MODE_PRIVATE)
            return sharedPreferences.getBoolean("isRunning", false)
        }
    }

    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var debugNotificationManager: NotificationManager
    private lateinit var debugNotificationBuilder: NotificationCompat.Builder

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var event: HistoryEvent
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var notificationTitle = ""
    private var notificationText = ""
    private var notificationDescription = ""

    private val logData = LogData()

    private var continueService = false

    private var startTime: Long = 0L
    private var validUpdates: Int = 0
    private var updates: Int = 0

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        sharedPreferences =
            applicationContext.getSharedPreferences("recordEvent", Context.MODE_PRIVATE)
        val now = ZonedDateTime.now()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock")
        wakeLock.acquire()

        startTime = Instant.now().toEpochMilli()

        event = EventRange(
            id = generateEventId(),
            title = now
                .format(DateTimeFormatter.ofPattern("'Event' dd MMM yyyy, hh:mm:ss")),
            description = "",
            time = now,
            end = now,
        )

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup Location Callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
//                for (location: Location in locationResult.locations) {
//                    // Handle location updates
//                    Log.d("NanHistoryDebug", "Location: ${location.latitude}, ${location.longitude}")
//                }
                onLocationUpdate(locationResult.locations)
            }
        }

        logData.append("Title: ${event.title}\n")
        logData.append("Start: $now")
        logData.append("\n")
        logData.append(
            "TIME".padEnd(10) +
                    "valid".padEnd(7) +
                    "updates".padEnd(9) +
                    "got".padEnd(5) +
                    "current".padEnd(9) +
                    "elapsed".padEnd(9) +
                    "acc".padEnd(18)
        )

        logData.save(applicationContext)
    }

    private fun onLocationUpdate(locations: List<Location>) {
        Log.d("NanHistoryDebug", "Location update!")
        Log.d("NanHistoryDebug", "Data: $locations")

        val accuracyThreshold = Config.locationAccuracyThreshold.get(applicationContext)
        val minimumDistance = Config.locationMinimumDistance.get(applicationContext)

        val eventData = event
        val now = ZonedDateTime.now()

        val locationMap = if (locations.size > 1) {
            val zoneId = Calendar.getInstance().timeZone.toZoneId()
            locations.associate {
                val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.time), zoneId)
                time to Coordinate(it.latitude, it.longitude)
            }
        } else null

        when (eventData) {
            is EventPoint -> {
                if (eventData.location == null && locations.size == 1 && locations.first().accuracy <= accuracyThreshold) {
                    eventData.location = Coordinate(
                        locations.first().latitude, locations.first().longitude
                    )
                    event = eventData
                } else {
                    event = EventRange(
                        id = eventData.id,
                        title = eventData.title,
                        description = eventData.description,
                        time = event.time,
                        end = now,
                        favorite = event.favorite,
                        tags = event.tags,
                        created = event.created,
                        modified = event.modified,
                        locations = locationMap?.toMutableMap() ?: mutableMapOf()
                    )
                }
            }

            is EventRange -> {
                val zoneId = Calendar.getInstance().timeZone.toZoneId()
                for (location in locations) {
                    if (location.latitude != 0.0 && location.longitude != 0.0) {
                        Log.d(
                            "NanHistoryDebug",
                            "Location received: ${location.latitude}, ${location.longitude}"
                        )
                    } else {
                        Log.d("NanHistoryDebug", "Invalid location data received")
                        continue
                    }

                    if (location.accuracy > accuracyThreshold) continue

                    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(location.time), zoneId)
                    (event as EventRange).locations[time] = Coordinate(
                        location.latitude, location.longitude
                    )
                }
                (event as EventRange).end = now
            }
        }

        updates++
        validUpdates += locations.filterNot { it.latitude == 0.0 || it.longitude == 0.0 }.size

        val updateInterval = (Instant.now().toEpochMilli() - startTime) / validUpdates

        debugNotification(
            text =
            "I: ${updateInterval}ms\n" +
                    "S: ${
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(startTime),
                            ZoneId.of("Asia/Jakarta")
                        )
                    }"
        )

        val signatureValidBeforeUpdate = event.validateSignature()
        event.generateSignature(true)
        Log.d(
            "NanHistoryDebug",
            "Event signature valid: B: $signatureValidBeforeUpdate, A: ${event.validateSignature()}"
        )
        event.save(applicationContext)

        if (Instant.now().toEpochMilli() - startTime > 900000L) {
            continueService = true
            stopSelf()
        }

//        "TIME".padEnd(10) +
//        "valid".padEnd(7) +
//        "updates".padEnd(9) +
//        "got".padEnd(5) +
//        "current".padEnd(9) +
//        "elapsed".padEnd(9) +
//        "acc".padEnd(18)

        logData.append(
            TimeFormatterWithSecond.format(ZonedDateTime.now()).padEnd(10) +
                    "$validUpdates".padEnd(7) +
                    "$updates".padEnd(9) +
                    "${locations.size}".padEnd(5) +
                    (
                            when (event) {
                                is EventPoint -> {
                                    1
                                }

                                is EventRange -> {
                                    (event as EventRange).locations.size
                                }

                                else -> 0
                            }
                            ).toString().padEnd(9) +
                    ((Instant.now().toEpochMilli() - startTime) / 1000).toInt().toString()
                        .padEnd(9) +
                    locations.map { it.accuracy.roundToInt() }.joinToString(",")
        )
//        logData.data["history"] = historyData.toMap()
        logData.save(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) Log.d("NanHistoryDebug", "Received null intent")
        Log.d("NanHistoryDebug", "Starting foreground service...")
        Log.d("NanHistoryDebug", "Recording event...")

        when (intent?.action) {
            "STOP_RECORD" -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_STICKY
            }
        }

        if (intent != null) {
            val eventId = intent.getStringExtra("eventId")
            val path = intent.getStringExtra("path")
            val fileData = HistoryFileData.get(applicationContext, path ?: "")
            event = fileData?.events?.first { it.id == (eventId ?: "") } ?: event
        }

        val channelId = "nanhistory_record"
        val channelName = "Record Event"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val debugChannelId = "nanhistory_record_debug"
        val debugChannelName = "Service Debug"
        val debugImportance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(channelId, channelName, importance)
        val debugChannel = NotificationChannel(debugChannelId, debugChannelName, debugImportance)

        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        debugNotificationManager = getSystemService(NotificationManager::class.java)
        debugNotificationManager.createNotificationChannel(debugChannel)

        notificationBuilder = NotificationCompat.Builder(this, "nanhistory_record")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(resources.getColor(R.color.record_notification_bg, theme))
            .setColorized(true)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        debugNotificationBuilder = NotificationCompat.Builder(this, "nanhistory_record_debug")
            .setContentTitle("Service Debug")
            .setContentText("Starting...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)

        event.generateSignature(true)
        event.save(applicationContext)
        updateNotification("Recording Event", event.title)
        debugNotification()

        startForeground(1, notificationBuilder.build())
        startLocationUpdates()

        sharedPreferences.edit().putBoolean("isRunning", true).apply()
        return START_STICKY
    }

    private fun iterateLocations() {
        val minimumDistance = Config.locationMinimumDistance.get(applicationContext)

        if (event is EventRange) {
            val eventRange: EventRange = event as EventRange
            val coordinateMap: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf()
            var prevLocation: Location? = null

            eventRange.locations = eventRange.locations

            val eventLocations = eventRange.locations

            for (key in eventLocations.keys.sorted()) {
                val currentLocation = Location("current").apply {
                    longitude = eventLocations[key]!!.longitude
                    latitude = eventLocations[key]!!.latitude
                    time = key.toInstant().toEpochMilli()
                }
                ZonedDateTime.now().toInstant()
                if (prevLocation != null) {
                    if (currentLocation.distanceTo(prevLocation) < minimumDistance &&
                        currentLocation.time.compareTo(prevLocation.time) < 900000L
                    ) continue
                }
                prevLocation = currentLocation
                coordinateMap[key] = eventLocations[key]!!
            }

            (event as EventRange).locations = coordinateMap
        }
    }

    private fun updateNotification(
        title: String? = null,
        text: String? = null,
        description: String? = null
    ) {
        val actionIntent = Intent(this, RecordService::class.java).apply {
            action = "STOP_RECORD" // Custom action
        }
        val actionPendingIntent = PendingIntent.getService(
            this,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (title != null) notificationTitle = title
        if (text != null) notificationText = text
        if (description != null) notificationDescription = description

        val customView = RemoteViews(packageName, R.layout.record_notification).apply {
            setTextViewText(R.id.notification_title, notificationTitle)
            setTextViewText(R.id.notification_message, notificationText)
            setOnClickPendingIntent(R.id.notification_button, actionPendingIntent)
        }

        val customViewBig = RemoteViews(packageName, R.layout.record_notification_big).apply {
            setTextViewText(R.id.notification_title, notificationTitle)
            setTextViewText(R.id.notification_message, notificationText)
            if (notificationDescription.isNotBlank()) {
                setTextViewText(R.id.notification_description, notificationDescription)
                setViewVisibility(R.id.notification_description, View.VISIBLE)
            }
            else {
                setViewVisibility(R.id.notification_description, View.INVISIBLE)
            }
            setOnClickPendingIntent(R.id.notification_button, actionPendingIntent)
        }

        notificationBuilder.setCustomContentView(customView)
        notificationBuilder.setCustomBigContentView(customViewBig)
        notificationBuilder.setOngoing(true)
        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun debugNotification(title: String? = null, text: String? = null, separate: Boolean = false) {
        if (title != null) debugNotificationBuilder.setContentTitle(title)
        if (text != null) debugNotificationBuilder.setContentText(text)
        if (!separate)
            updateNotification(description = text)
        else
            debugNotificationManager.notify(2, debugNotificationBuilder.build())
    }

    private fun startLocationUpdates() {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NanHistoryDebug", "Access not granted! Stopping service...")
            stopSelf() // Stop service if permissions are missing
            return
        }

        Log.d("NanHistoryDebug", "Requesting location updates")

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Config.locationUpdateInterval.get(applicationContext) * 1000L
        ).build()

        val task = fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )

        task.addOnCanceledListener {
            Log.d("NanHistoryDebug", "Task cancelled")
        }

        task.addOnFailureListener {
            Log.d("NanHistoryDebug", "Task failure: $it")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("NanHistoryDebug", "Service [onDestroy]")
        // Stop location updates when the service is destroyed
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        sharedPreferences.edit().putBoolean("isRunning", false).apply()

        iterateLocations()

        if (event is EventRange) {
            (event as EventRange).end = ZonedDateTime.now()
            event.generateSignature(true)
            event.save(applicationContext)
        }

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }

        debugNotification(separate = true)
        notificationManager.cancel(1)

        if (continueService) {
            val intent = Intent(this, this::class.java)
            intent.putExtra("eventId", event.id)
            intent.putExtra("path", getFilePathFromDate(event.time.toLocalDate()))
            startForegroundService(intent)
        }
    }
}