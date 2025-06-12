package id.my.nanclouder.nanhistory.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import id.my.nanclouder.nanhistory.MainActivity
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.LogData
import id.my.nanclouder.nanhistory.lib.RecordStatus
import id.my.nanclouder.nanhistory.lib.ServiceBroadcast
import id.my.nanclouder.nanhistory.lib.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.createLocationFile
import id.my.nanclouder.nanhistory.lib.history.generateEventId
import id.my.nanclouder.nanhistory.lib.history.generateSignature
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.lib.history.updateModifiedTime
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import id.my.nanclouder.nanhistory.lib.history.writeToLocationFile
import id.my.nanclouder.nanhistory.lib.matchOrNull
import id.my.nanclouder.nanhistory.lib.readableTimeHours
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.roundToInt
import androidx.core.content.edit
import id.my.nanclouder.nanhistory.lib.history.getLocationFile

class RecordService : Service() {
    companion object {
        fun isRunning(context: Context): Boolean {
            val sharedPreferences =
                context.getSharedPreferences("recordEvent", Context.MODE_PRIVATE)
            return sharedPreferences.getBoolean("isRunning", false)
        }

        val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("'Event' dd MMM yyyy, HH:mm:ss")
    }

    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var notificationManager: NotificationManager

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
    private var eventPoint = false
    private var maxDuration = 0

    // Audio recording
    private var includeAudio = false
    private var mediaRecorder: MediaRecorder? = null
    private var outputAudio: String = ""
    private var audioFolder: String = ""

    private var startTime: Long = 0L
    private var validUpdates: Int = 0
    private var updates: Int = 0

    private lateinit var actionIntent: Intent
    private lateinit var actionPendingIntent: PendingIntent
    private lateinit var notificationPendingIntent: PendingIntent

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private lateinit var locationPath: String
    private lateinit var locationFile: File
    private val eventLocations: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf()

    private fun sendStatusBroadcast(status: Int) {
        val intent = Intent(ServiceBroadcast.ACTION_SERVICE_STATUS).apply {
            putExtra(ServiceBroadcast.EXTRA_STATUS, status)
            putExtra(ServiceBroadcast.EXTRA_EVENT_ID, event.id)
            putExtra(ServiceBroadcast.EXTRA_EVENT_PATH, getFilePathFromDate(event.time.toLocalDate()))
            putExtra(ServiceBroadcast.EXTRA_EVENT_POINT, eventPoint)
        }
        sendBroadcast(intent)
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        sharedPreferences =
            applicationContext.getSharedPreferences("recordEvent", Context.MODE_PRIVATE)
        val now = ZonedDateTime.now()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock")
        wakeLock.acquire()

        includeAudio = Config.experimentalAudioRecord.get(applicationContext) && Config.includeAudioRecord.get(applicationContext)
        if (includeAudio) {
            val path = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val folder = File(filesDir, "audio/$path")
            folder.mkdirs()
            val id = generateEventId()
            audioFolder = folder.parent!!
            outputAudio = "$path/$id.m4a"
        }

        startTime = Instant.now().toEpochMilli()

        event = EventRange(
            id = generateEventId(),
            title = now
                .format(titleFormatter),
            description = "",
            time = now,
            end = now,
        )

        locationFile = createLocationFile(applicationContext)
        locationPath = locationFile.absolutePath.removePrefix(File(filesDir, "locations").absolutePath + "/")
        event.locationPath = locationPath

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup Location Callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationUpdate(locationResult.locations)
            }
        }

        actionIntent = Intent(this, RecordService::class.java).apply {
            action = "STOP_RECORD" // Custom action
        }
        actionPendingIntent = PendingIntent.getService(
            this,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun onLocationUpdate(locations: List<Location>) {
        Log.d("NanHistoryDebug", "Location update!")
        Log.d("NanHistoryDebug", "Data: $locations")

        val accuracyThreshold = Config.locationAccuracyThreshold.get(applicationContext)
        val minimumDistance = Config.locationMinimumDistance.get(applicationContext)

        val eventData = event
        val now = ZonedDateTime.now()

        var locationSize = 0

        val locationMap = if (locations.size > 1) {
            val zoneId = Calendar.getInstance().timeZone.toZoneId()
            locations.associate {
                val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.time), zoneId)
                time to Coordinate(it.latitude, it.longitude)
            }
        } else null

//        when (eventData) {
//            is EventPoint -> {
//                if (eventData.location == null && locations.size == 1 && locations.first().accuracy <= accuracyThreshold) {
//                    eventData.location = Coordinate(
//                        locations.first().latitude, locations.first().longitude
//                    )
//                    event = eventData
//                    locationSize = 1
//                } else {
//                    event = EventRange(
//                        id = eventData.id,
//                        title = eventData.title,
//                        description = eventData.description,
//                        time = event.time,
//                        end = now,
//                        favorite = event.favorite,
//                        tags = event.tags,
//                        created = event.created,
//                        modified = event.modified,
//                        locations = locationMap?.toMutableMap() ?: mutableMapOf()
//                    )
//                    locationSize = locations.size
//                }
//            }
//
//            is EventRange -> {
//                val zoneId = Calendar.getInstance().timeZone.toZoneId()
//                for (location in locations) {
//                    if (location.latitude != 0.0 && location.longitude != 0.0) {
//                        Log.d(
//                            "NanHistoryDebug",
//                            "Location received: ${location.latitude}, ${location.longitude}"
//                        )
//                    } else {
//                        Log.d("NanHistoryDebug", "Invalid location data received")
//                        continue
//                    }
//
//                    if (location.accuracy > accuracyThreshold) continue
//
//                    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(location.time), zoneId)
//                    (event as EventRange).locations[time] = Coordinate(
//                        location.latitude, location.longitude
//                    )
//
//                    if ((event as EventRange).locations.isNotEmpty() && eventPoint) {
//                        stopSelf()
//                    }
//
//                    // TODO
//                }
//                (event as EventRange).end = now
//            }
//        }

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
            eventLocations[time] = Coordinate(
                location.latitude, location.longitude
            )

            if (eventLocations.isNotEmpty() && eventPoint) {
                stopSelf()
            }

            // TODO
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
                    }",
        )

        updateNotification()

        val signatureValidBeforeUpdate = event.validateSignature(applicationContext)

        event.metadata["record_updates"] = updates

        eventLocations.writeToLocationFile(locationFile)
        Log.d(
            "NanHistoryDebug",
            "Event signature valid: B: $signatureValidBeforeUpdate, A: ${event.validateSignature(applicationContext)}"
        )
        event.generateSignature(true, applicationContext)
        event.updateModifiedTime()
        event.save(applicationContext)

        if (Instant.now().toEpochMilli() - startTime > (1000L * maxDuration)) {
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
                    eventLocations.size.toString().padEnd(9) +
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
        val now = ZonedDateTime.now()

        handler = Handler(mainLooper)
        runnable = object : Runnable {
            override fun run() {
                if (!isRunning(applicationContext))
                    return

                val timeElapsed = Duration.between(event.time.toInstant(), Instant.now())
                updateNotification(readableTimeHours(timeElapsed))

                handler.postDelayed(this, 250) // Update every a quarter second
            }
        }
        handler.post(runnable)

        maxDuration = Config.serviceMaxDuration.get(applicationContext)

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
            event.locationPath?.let {
                locationPath = it
                locationFile = File(filesDir, "locations/$locationPath")
                eventLocations.putAll(
                    getLocationFile(locationPath, applicationContext).locations
                )
            }

            eventPoint = intent.getBooleanExtra("eventPoint", false)
        }

        if (event.audio != null)
            includeAudio = false

        val channelId = "nanhistory_record"
        val channelName = "Record Event"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val debugChannelId = "nanhistory_record_debug"
        val debugChannelName = "Service Debug"
        val debugImportance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Record service notifications"
            enableLights(true)
            enableVibration(true)
        }
        val debugChannel = NotificationChannel(debugChannelId, debugChannelName, debugImportance)
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        debugNotificationManager = getSystemService(NotificationManager::class.java)
        debugNotificationManager.createNotificationChannel(debugChannel)


//        notificationBuilder.addAction(R.drawable.ic_stop_filled, "Stop", PendingIntent.getService(
//            this,
//            0,
//            Intent(this, RecordService::class.java).apply {
//                action = "STOP_RECORD" // Custom action
//            },
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        ))

        debugNotificationBuilder = NotificationCompat.Builder(this, "nanhistory_record_debug")
            .setContentTitle("Service Debug")
            .setContentText("Starting...")
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(notificationPendingIntent)

        // Process metadata
        val recordServiceCount =
            matchOrNull<Double>(event.metadata["record_service_count"]) ?: 0.0
        val recordUpdates = matchOrNull<Double>(event.metadata["record_updates"]) ?: 0.0

        event.metadata["record_service_count"] = recordServiceCount + 1
        updates = recordUpdates.toInt()

        event.locationPath = locationPath

        event.generateSignature(true, applicationContext)
        event.save(applicationContext)

        notificationTitle = "00:00"
        notificationText = "Recording Event"

        if (includeAudio)
            startForeground(1, getNotification(notificationTitle, notificationText))
        else
            startForeground(1, getNotification(notificationTitle, notificationText), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        startLocationUpdates()

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

        sharedPreferences.edit {
            putBoolean("isRunning", true)
                .putString("eventId", event.id)
        }

        sendStatusBroadcast(RecordStatus.RUNNING)

        if (includeAudio) startAudioRecording()

        return START_STICKY
    }

    fun isMicAvailable(): Boolean {
        return try {
            MediaRecorder(applicationContext).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null") // dummy file
                prepare()
                start()
                stop()
                release()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun startAudioRecording() {
        mediaRecorder = MediaRecorder(applicationContext).apply {
            val path = "$audioFolder/$outputAudio"
            val stereoChannel = Config.audioStereoChannel.get(applicationContext)
            val encodingBitrate = Config.audioEncodingBitrate.get(applicationContext) * 1000
            val samplingRate = Config.audioSamplingRate.get(applicationContext) * 1000

            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(path)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(if (stereoChannel) 2 else 1)
            setAudioEncodingBitRate(encodingBitrate)
            setAudioSamplingRate(samplingRate)

            val maxDuration = Config.audioRecordMaxDuration.get(applicationContext)
            if (maxDuration > 0) setMaxDuration(maxDuration * 1000) // Based on user settings

            setOnInfoListener { _, what, extra ->
                Log.d("Recorder", "$what, $extra")
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopAudioRecording()
                    Log.d("Recorder", "Max duration reached. Recording stopped.")
                }
            }

            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("NanHistoryError", "$e")
            }
        }

    }

    private fun stopAudioRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        event.audio = outputAudio
        event.generateSignature(true, applicationContext)
        event.save(applicationContext)
    }

    private fun iterateLocations() {
        val minimumDistance = Config.locationMinimumDistance.get(applicationContext)

        if (event is EventRange) {
//            val eventRange: EventRange = event as EventRange
            val coordinateMap: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf()
            var prevLocation: Location? = null

//            eventRange.locations = eventRange.locations

//            val eventLocations = eventRange.locations

            for (key in eventLocations.keys.sorted()) {
                val currentLocation = Location("current").apply {
                    longitude = eventLocations[key]!!.longitude
                    latitude = eventLocations[key]!!.latitude
                    time = key.toInstant().toEpochMilli()
                }
                ZonedDateTime.now().toInstant()
                if (prevLocation != null) {
                    if (currentLocation.distanceTo(prevLocation) < minimumDistance &&
                        currentLocation.time.compareTo(prevLocation.time) < 900_000L
                    ) continue
                }
                prevLocation = currentLocation
                coordinateMap[key] = eventLocations[key]!!
            }

            eventLocations.clear()
            for (key in coordinateMap.keys) {
                eventLocations[key] = coordinateMap[key]!!
            }

            eventLocations.writeToLocationFile(locationFile)
        }
    }

    private fun getNotification(title: String, text: String, locations: String = ""): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, "nanhistory_record")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)

        val customView = RemoteViews(packageName, R.layout.record_notification).apply {
            setTextViewText(R.id.notification_title, title)
            setTextViewText(R.id.notification_message, text)
            if (locations.isNotBlank()) {
                setTextViewText(R.id.notification_locations, locations)
                setViewVisibility(R.id.notification_locations, View.VISIBLE)
            }
            else {
                setViewVisibility(R.id.notification_locations, View.INVISIBLE)
            }
            setOnClickPendingIntent(R.id.notification_button, actionPendingIntent)
            setOnClickPendingIntent(R.id.notification_linear_layout, notificationPendingIntent)
            if (eventPoint) setViewVisibility(R.id.notification_button, View.INVISIBLE)
        }

//        val customViewBig = RemoteViews(packageName, R.layout.record_notification_big).apply {
//            setTextViewText(R.id.notification_title, title)
//            setTextViewText(R.id.notification_message, text)
//            if (description.isNotBlank()) {
//                setTextViewText(R.id.notification_description, description)
//                setViewVisibility(R.id.notification_description, View.VISIBLE)
//            }
//            else {
//                setViewVisibility(R.id.notification_description, View.INVISIBLE)
//            }
//            setOnClickPendingIntent(R.id.notification_button, actionPendingIntent)
//            setOnClickPendingIntent(R.id.notification_linear_layout, notificationPendingIntent)
//            if (eventPoint) setViewVisibility(R.id.notification_button, View.INVISIBLE)
//        }

        notificationBuilder.setCustomContentView(customView)
//        notificationBuilder.setCustomBigContentView(customViewBig)
        notificationBuilder.setCustomBigContentView(customView)

        return notificationBuilder.build()
    }

    private fun updateNotification(
        title: String? = null,
        text: String? = null,
        description: String? = null,
        notify: Boolean = true
    ) {
        if (title != null) notificationTitle = title
        if (text != null) notificationText = text
        if (description != null) notificationDescription = description

        val locationSize = eventLocations.size

        if (notify) notificationManager.notify(
            1, getNotification(notificationTitle, notificationText, "($locationSize)")
        )
    }

    private fun debugNotification(title: String? = null, text: String? = null, separate: Boolean = false) {
        if (title != null) debugNotificationBuilder.setContentTitle(title)
        if (text != null) debugNotificationBuilder.setContentText(text)
//        if (!separate)
//            updateNotification(description = text)
//        else
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

        val secondsInterval = if (!eventPoint) Config.locationUpdateInterval.get(applicationContext)
            else 1

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            secondsInterval * 1000L
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
        handler.removeCallbacks(runnable)

        Log.d("NanHistoryDebug", "Service [onDestroy]")
        // Stop location updates when the service is destroyed
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        sharedPreferences.edit {
            putBoolean("isRunning", false)
                .remove("eventId")
        }

        iterateLocations()
        if (includeAudio && mediaRecorder != null) stopAudioRecording()

        if (
            event is EventRange && (
                Duration.between(event.time.toInstant(), Instant.now()) < Duration.ofSeconds(30)
                || eventPoint)
            ) {
            val eventData = event as EventRange
            event = EventPoint(
                id = eventData.id,
                title = eventData.title,
                description = eventData.description,
                time = eventData.time,
                favorite = eventData.favorite,
                tags = eventData.tags,
                created = eventData.created,
                modified = eventData.modified,
                metadata = eventData.metadata,
                audio = eventData.audio,
                locationPath = locationPath
            )
        }

        // Include audio path if audio recording is enabled

        if (event is EventRange) {
            (event as EventRange).end = ZonedDateTime.now()
        }

        if (eventLocations.isEmpty()) {
            event.locationPath = null
            locationFile.delete()
        }
        else {
            event.locationPath = locationPath
        }

        Log.d("NanHistoryDebug", "Applied: " + event.generateSignature(true, applicationContext))
        event.save(applicationContext)

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }


        debugNotification()
        notificationManager.cancel(1)

        if (continueService) {
            val restartIntent = Intent(this, this::class.java)
            restartIntent.putExtra("eventId", event.id)
            restartIntent.putExtra("path", getFilePathFromDate(event.time.toLocalDate()))

            val stopRunnable = object : Runnable {
                override fun run() {
                    startForegroundService(restartIntent)
                    handler.removeCallbacks(this)
                }
            }
            handler.postDelayed(stopRunnable, 500)
        }
        else {
            Toast.makeText(this, "Event recording stopped", Toast.LENGTH_SHORT).show()
            sendStatusBroadcast(RecordStatus.READY)
        }

    }
}