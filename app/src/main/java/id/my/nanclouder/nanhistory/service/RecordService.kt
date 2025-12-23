package id.my.nanclouder.nanhistory.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.location.Location
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.quicksettings.TileService
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
import id.my.nanclouder.nanhistory.config.LocationIterationLogic
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.LogData
import id.my.nanclouder.nanhistory.utils.RecordStatus
import id.my.nanclouder.nanhistory.utils.ServiceBroadcast
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.createLocationFile
import id.my.nanclouder.nanhistory.utils.history.generateEventId
import id.my.nanclouder.nanhistory.utils.history.generateSignature
import id.my.nanclouder.nanhistory.utils.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.utils.history.updateModifiedTime
import id.my.nanclouder.nanhistory.utils.history.writeToLocationFile
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.readableTimeHours
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.roundToInt
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.AccelerometerChange
import id.my.nanclouder.nanhistory.utils.HumanShakeDetector
import id.my.nanclouder.nanhistory.utils.average
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.EventLocationDigest
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.appendToLocationFile
import id.my.nanclouder.nanhistory.utils.history.generateSignatureV1
import id.my.nanclouder.nanhistory.utils.history.getLocationFile
import id.my.nanclouder.nanhistory.utils.history.toLocationPath
import id.my.nanclouder.nanhistory.utils.readLineWithNewline
import id.my.nanclouder.nanhistory.utils.transportModel.TransportMode
import id.my.nanclouder.nanhistory.utils.transportModel.detectTransportMode
import id.my.nanclouder.nanhistory.utils.transportModel.loadCalibrationModels
import id.my.nanclouder.nanhistory.utils.transportModel.toTransportationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class RecordService : Service() {
    companion object {
        val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("'Event' dd MMM yyyy, HH:mm:ss")

        const val ACTION_RECORD_START = "RECORD_START"
        const val ACTION_RECORD_STOP = "RECORD_STOP"
        const val ACTION_SERVICE_IDLE = "SERVICE_IDLE"
        const val ACTION_SERVICE_RESTART = "SERVICE_RESTART"
        const val ACTION_SERVICE_STOP = "SERVICE_STOP"
    }

    object RecordState {
        val isRecording = MutableStateFlow(false)
        val isRunning = MutableStateFlow(false)
        val eventId = MutableStateFlow<String?>(null)
        val status = MutableStateFlow(RecordStatus.READY)
        val currentLogic = MutableStateFlow(LocationIterationLogic.Default)
    }

    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var notificationManager: NotificationManager

    private lateinit var debugNotificationManager: NotificationManager
    private lateinit var debugNotificationBuilder: NotificationCompat.Builder

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var event: HistoryEvent
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var vibrator: Vibrator

    private var notificationTitle = ""
    private var notificationText = ""
    private var notificationDescription = ""

    private val locationDigest = EventLocationDigest()
    private var locationFileReader: RandomAccessFile? = null

    private lateinit var locationLogData: LogData
    private lateinit var iterationLogData: LogData
    private lateinit var serviceLogData: LogData
    private lateinit var serviceErrorLogData: LogData

    private var continueService = false
    private var eventPoint = false
    private var restartEnabled = false
    private var maxDuration = 0
    private var startedAsIdle = false

    // Audio recording
    private var includeAudio = false
    private var mediaRecorder: MediaRecorder? = null
    private var outputAudio: String = ""
    private var audioFolder: String = ""

    // Vibration
    private var vibrateEnabled = false
    private var vibrateEveryUpdate = false
    private var vibrateInterval = 5
    private var vibrateDuration = 0L
    private var lastVibrateTime = 0L

    private var startTime: Long = 0L
    private var validUpdates: Int = 0
    private var updates: Int = 0

    // Location iteration tracking
    private var lastIterationIndex: Int = 0

    private lateinit var actionIntent: Intent
    private lateinit var actionPendingIntent: PendingIntent
    private lateinit var notificationPendingIntent: PendingIntent

    private lateinit var handler: Handler
    private var runnable: Runnable? = null
    private var vibrationJob: Job? = null
    private var signatureJob: Job? = null

    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao

    // Shake detection
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private val shakeDetector = HumanShakeDetector()
    private var shakeDetectionEnabled = false
    private var shakeToStartEnabled = false
    private var recordingStarted = false
    private var startTriggered = false
    private var shakeDetectionRegistered = false
    private var lastShakeDetected: Instant = Instant.now().minusSeconds(5)
    private var currentShakeCooldown = 4000L

    private lateinit var accelDataFile: File

    // Track if we've received both sensor types at least once
    private var hasAccelData = false
    private var hasGyroData = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var locationPath: String
    private lateinit var locationFile: File
    private val eventLocations: MutableList<LocationData> = mutableListOf()
    private val locationBuffer: MutableList<LocationData> = mutableListOf()
    private val iteratedLocationBuffer: MutableList<LocationData> = mutableListOf()

    // Location updates retry
    private var locationRetryCount = 0
    private val locationRetryDelayMs = 2000L
    private var locationTaskHandled = false

    private fun sendStatusBroadcast(status: Int) {
        val intent = Intent(ServiceBroadcast.ACTION_SERVICE_STATUS).apply {
            putExtra(ServiceBroadcast.EXTRA_STATUS, status)
            putExtra(ServiceBroadcast.EXTRA_EVENT_ID, event.id)
            putExtra(ServiceBroadcast.EXTRA_EVENT_PATH, getFilePathFromDate(event.time.toLocalDate()))
            putExtra(ServiceBroadcast.EXTRA_EVENT_POINT, eventPoint)
        }
        sendBroadcast(intent)
    }

    private fun logService(msg: String) {
        serviceLogData.append("${
            DateTimeFormatter.ofPattern("HH:mm:ss").format(ZonedDateTime.now())
        }   $msg")
        serviceLogData.save(applicationContext)
    }

    private fun logServiceError(msg: String) {
        serviceErrorLogData.append("${
            DateTimeFormatter.ofPattern("HH:mm:ss").format(ZonedDateTime.now())
        }   $msg")
        serviceErrorLogData.save(applicationContext)
    }

    private fun showIdleNotification() {
        notificationTitle = "Waiting for trigger"
        notificationText = "Ready to record event"
        updateNotification()
    }

    private val sensorEventListener = object : SensorEventListener {
        private var lastAccelValues = FloatArray(3)
        private var lastGyroValues = FloatArray(3)

        private var accelData = mutableListOf<AccelerometerChange>()
        private var lastUpdateMinute = 0
        private var lastUpdateSecond = 0

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return

            val collectAccelerometerData =
                Config.developerCollectAccelerometer.get(applicationContext) &&
                Config.developerModeEnabled.get(applicationContext) &&
                RecordState.isRecording.value

            // Ignore if current status is BUSY or below (RESTARTING)
            if (RecordState.status.value <= RecordStatus.BUSY) return

            val recordDuration = Duration.of(
                ZonedDateTime.now().toInstant().toEpochMilli() - this@RecordService.event.time.toInstant().toEpochMilli(),
                ChronoUnit.MILLIS
            )

            if (collectAccelerometerData &&
                event.sensor.type == Sensor.TYPE_ACCELEROMETER &&
                recordDuration.toSecondsPart() < 30) {
                val currentData = event.values.copyOf()

                // TODO
                // TODO
                // TODO
                // TODO
                // TODO

                if (lastUpdateMinute != recordDuration.toMinutesPart()) {
                    lastUpdateMinute = recordDuration.toMinutesPart()
                    accelDataFile.appendText("===\n")
                }

                // Save average accelerometer change every single second
                if (recordDuration.toSecondsPart() == lastUpdateSecond) {
                    val newAccelData = AccelerometerChange(
                        x = abs(currentData[0] - lastAccelValues[0]),
                        y = abs(currentData[1] - lastAccelValues[1]),
                        z = abs(currentData[2] - lastAccelValues[2]),
                    )
                    accelData.add(newAccelData)
                }
                else if (accelData.isNotEmpty()) {
                    lastUpdateSecond = recordDuration.toSecondsPart()

                    val averageAccelData = accelData.average()

                    accelDataFile.appendText("$averageAccelData\n")
                    accelData.clear()
                }
            }

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccelValues = event.values.copyOf()
                    hasAccelData = true
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyroValues = event.values.copyOf()
                    hasGyroData = true
                }
            }

            // Only check for shake when we have received both sensor types
            if (shakeDetectionEnabled && hasAccelData && hasGyroData) {
                val isShakeDetected = shakeDetector.update(
                    ax = lastAccelValues[0],
                    ay = lastAccelValues[1],
                    az = lastAccelValues[2],
                    gx = lastGyroValues[0],
                    gy = lastGyroValues[1],
                    gz = lastGyroValues[2],
                    timestampMs = System.currentTimeMillis()
                )

                val intervalFromPrevious = Instant.now().toEpochMilli() - lastShakeDetected.toEpochMilli()

                // When shake detected and no ongoing cooldown
                if (isShakeDetected && intervalFromPrevious > currentShakeCooldown && RecordState.status.value != RecordStatus.BUSY) {
                    // Update cooldown
                    lastShakeDetected = Instant.now()

                    // Default cooldown
                    currentShakeCooldown = 4000L

                    // Handle shake-to-start
                    if (!RecordState.isRecording.value && shakeToStartEnabled && !eventPoint) {
                        Log.d("RecordService", "First shake detected! Starting recording...")
                        startTriggered = true

                        // Use default cooldown

                        // Perform confirmation vibration
                        val timings = longArrayOf(
                            0L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                            500L, 500L,
                        )
                        val amplitudes = intArrayOf(
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                            0, VibrationEffect.DEFAULT_AMPLITUDE,
                        )
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)

                        // Mark in metadata that shake started recording
                        // this@RecordService.event.metadata["shake_to_start"] = true
                        // this@RecordService.event.time = ZonedDateTime.now()
                        // this@RecordService.event.generateSignature(true, this@RecordService)
                        // serviceScope.launch { dao.insertEvent(this@RecordService.event.toEventEntity()) }

                        startEventRecording(shakeToStart = true)

                        // Change status to RUNNING
                        RecordState.status.value = RecordStatus.RUNNING
                        requestTileListeningState()
                        updateNotification()
                    }
                    // Handle shake-to-stop (only if recording already started)
                    else if (RecordState.isRecording.value && !eventPoint) {
                        Log.d("RecordService", "Shake detected! Stopping recording...")

                        // Cooldown after stopping recording (override default)
                        currentShakeCooldown = 10_000L

                        val timings = longArrayOf(
                            0L,    // no delay before start
                            200L,  // vibrate #1
                            100L,  // gap #1
                            200L,  // vibrate #2
                            100L,  // gap #2
                            200L,  // vibrate #3
                            100L,  // gap #3
                            200L,  // vibrate #4
                            100L,  // gap #4
                            200L,  // vibrate #5
                        )

                        val amplitudes = intArrayOf(
                            0,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                            0,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                            0,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                            0,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                            0,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                        )

                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)

                        this@RecordService.event.metadata["shake_to_stop"] = true
                        serviceScope.launch {
                            dao.insertEvent(this@RecordService.event.toEventEntity())

                            Log.d("RecordService", "STOP RECORD FROM SHAKE")
                            RecordState.status.value = RecordStatus.BUSY
                            requestTileListeningState()

                            stopEventRecording()

                            // Don't keep the service running if shake-to-start is disabled
                            if (!shakeToStartEnabled || !startedAsIdle)
                                stopRecordService()
                            else
                                showIdleNotification()
                        }

                        // stopSelf()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No-op
        }
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        when {
            accelerometerSensor == null -> {
                Log.w("RecordService", "Accelerometer sensor not available on this device")
            }
            gyroscopeSensor == null -> {
                Log.w("RecordService", "Gyroscope sensor not available on this device")
            }
            else -> {
                Log.d("RecordService", "Sensors initialized successfully")
            }
        }
    }

    private fun startShakeDetection() {
        // Check if shake detection is already initialized
        if (shakeDetectionRegistered)
            return

        if (!shakeDetectionEnabled) {
            Log.d("RecordService", "Shake detection is disabled in config")
            return
        }

        if (accelerometerSensor == null || gyroscopeSensor == null) {
            Log.w("RecordService", "Cannot start shake detection: required sensors not available")
            return
        }

        shakeDetector.reset()
        hasAccelData = false
        hasGyroData = false

        // Configure shake detector based on sensitivity config (1-5 scale)
        // Lower sensitivity value = lower accel threshold = more sensitive
        // Higher sensitivity value = higher accel threshold = less sensitive
        val sensitivity = Config.recordShakeSensitivity.get(applicationContext)

        // Map config sensitivity (1-5) to thresholds
        // Sensitivity 1: very sensitive (15m/s²)
        // Sensitivity 2: sensitive (18m/s²)
        // Sensitivity 3: medium (22m/s²)
        // Sensitivity 4: less sensitive (28m/s²)
        // Sensitivity 5: very insensitive (35m/s²)
        shakeDetector.accelThreshold = (10f - sensitivity * 4.5f).coerceIn(12f, 40f)

        // Gyro threshold scales with sensitivity too
        shakeDetector.gyroThreshold = (10f - sensitivity * 0.5f).coerceIn(2.5f, 5.0f)

        // Duration and direction changes also scale
        shakeDetector.minDurationMs = (400L - sensitivity * 5).coerceAtLeast(150L)
        shakeDetector.minDirectionChanges = (4 - (sensitivity - 1) / 2).coerceAtLeast(2)

        Log.d(
            "RecordService",
            "Starting shake detection (sensitivity: $sensitivity) with " +
                    "accelThreshold=${shakeDetector.accelThreshold}, " +
                    "gyroThreshold=${shakeDetector.gyroThreshold}, " +
                    "minDurationMs=${shakeDetector.minDurationMs}, " +
                    "minDirectionChanges=${shakeDetector.minDirectionChanges}"
        )

        sensorManager.registerListener(
            sensorEventListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            sensorEventListener,
            gyroscopeSensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        Log.d("RecordService", "Shake detection started successfully")
        shakeDetectionRegistered = true
    }

    private fun stopShakeDetection() {
        try {
            sensorManager.unregisterListener(sensorEventListener)
            shakeDetector.reset()
            hasAccelData = false
            hasGyroData = false
            Log.d("RecordService", "Shake detection stopped")
            shakeDetectionRegistered = false
        } catch (e: Exception) {
            Log.e("RecordService", "Error stopping shake detection: ${e.message}")
        }
    }

    private fun performVibration(doubleVibrate: Boolean = false) {
        if (!vibrateEnabled || vibrateDuration <= 0) return

        if (vibrateEveryUpdate) {
            val effect = VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE)
            val timings = longArrayOf(
                0L,
                vibrateDuration,
                100L,  // gap
                vibrateDuration,
            )

            val amplitudes = intArrayOf(
                0,
                VibrationEffect.DEFAULT_AMPLITUDE,
                0,
                VibrationEffect.DEFAULT_AMPLITUDE,
            )

            val doubleEffect = VibrationEffect.createWaveform(timings, amplitudes, -1)

            if (doubleVibrate) vibrator.vibrate(doubleEffect)
            else vibrator.vibrate(effect)
        }
    }

    private fun startIntervalVibration() {
        if (!vibrateEnabled || vibrateDuration <= 0 || vibrateEveryUpdate) return

        vibrationJob = serviceScope.launch {
            while (isActive) {
                delay(vibrateInterval * 1000L)
                if (isActive) {
                    val effect = VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                }
            }
        }
    }

    private fun stopIntervalVibration() {
        vibrationJob?.cancel()
        vibrationJob = null
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()

        initNotification()
        initSensors() // Add this line

        // TODO
        serviceScope.launch {
            RecordState.status.collect {
                Log.d("RecordService", "Status changed: $it")
            }
        }

        sharedPreferences =
            applicationContext.getSharedPreferences("recordEvent", MODE_PRIVATE)
        val now = ZonedDateTime.now()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock")
        wakeLock.acquire()

        // Initialize vibrator
        vibrator = let {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        }

        restartEnabled = Config.serviceRestartEnabled.get(applicationContext)

        includeAudio = Config.experimentalAudioRecord.get(applicationContext) && Config.includeAudioRecord.get(applicationContext)
        if (includeAudio) {
            val path = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val folder = File(filesDir, "audio/$path")
            folder.mkdirs()
            val id = generateEventId()
            audioFolder = folder.parent!!
            outputAudio = "$path/$id.m4a"
        }

        db = AppDatabase.getInstance(applicationContext)
        dao = db.appDao()

        startTime = Instant.now().toEpochMilli()

        initializeRecording()

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup Location Callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                try {
                    onLocationUpdate(locationResult.locations)
                } catch (e: Exception) {
                    handleGlobalError(e, "onLocationUpdate")
                }
            }
        }

        // Setup global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleGlobalError(throwable, "UncaughtException in thread: ${thread.name}")
        }
    }

    private fun initializeRecording() {
        val now = ZonedDateTime.now()
        event = EventRange(
            id = generateEventId(),
            title = now
                .format(titleFormatter),
            description = "",
            time = now,
            end = now,
        )

        locationFile = createLocationFile(applicationContext)
        locationPath = locationFile.toLocationPath(applicationContext)

        locationDigest.reset()
        locationFileReader = null

        event.locationPath = locationPath
        eventLocations.clear()

        validUpdates = 0
        updates = 0

        locationLogData = LogData(
            path = "${LocalDate.now()}/${DateTimeFormatter.ofPattern("HH-mm-ss").format(now)}_LOCATION.log"
        )
        iterationLogData = LogData(
            path = "${LocalDate.now()}/${DateTimeFormatter.ofPattern("HH-mm-ss").format(now)}_ITERATION.log"
        )
        serviceLogData = LogData(
            path = "${LocalDate.now()}/${DateTimeFormatter.ofPattern("HH-mm-ss").format(now)}_SERVICE.log"
        )
        serviceErrorLogData = LogData(
            path = "error/ServiceError ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(now)}.log"
        )

        // Init accel data collection
        val time = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .format(ZonedDateTime.now())

        // Make sure directory exists
        File(filesDir, "accelerometer_data").let {
            if (!it.exists()) {
                it.mkdir()
            }
        }

        accelDataFile = File(filesDir, "accelerometer_data/$time.accel")

        Log.d("RecordService", "Location file initialized: $locationPath")
    }

    private fun startEventRecording(eventPoint: Boolean = false, eventId: String? = null, shakeToStart: Boolean = false) {
        if (RecordState.isRecording.value && RecordState.status.value != RecordStatus.RESTARTING) {
            Log.d("RecordService", "Event recording already started and it's not a restart!")
            return
        }

        initializeRecording()
        event.metadata["shake_to_start"] = shakeToStart

        // Only assign runnable when it isn't initialized
        runnable = runnable ?: object : Runnable {
            private var lastBoundary = -1L
            override fun run() {
                if (!RecordState.isRecording.value) {
                    Log.d("Looper", "Service no is longer running, runnable returned")
                    return
                }

                val instantNow = Instant.now()
                val currentBoundary = instantNow.toEpochMilli() / 500
                Log.d("Looper", "Time counter loop")

                // Only update every 500ms, and align precisely to 500ms boundary
                if (currentBoundary == lastBoundary) {
                    val nextBoundaryMs = (currentBoundary + 1) * 500
                    val delayToNextBoundary = nextBoundaryMs - instantNow.toEpochMilli()

                    handler.postDelayed(
                        this,
                        if (delayToNextBoundary > 100) 100L else delayToNextBoundary
                    )
                    return
                }

                val timeElapsed = Duration.between(event.time.toInstant(), instantNow)
                val currentNotificationTitle = readableTimeHours(timeElapsed)
                val currentNotificationText =
                    if (RecordState.status.value == RecordStatus.IDLE) "Waiting for Trigger"
                    else "Recording Event"


                Log.d("Looper", "Updating notification...")
                updateNotification(currentNotificationTitle, currentNotificationText)
                lastBoundary = currentBoundary

                handler.postDelayed(this, 100)
            }
        }

        // Runnable should've been initialized at this point
        handler.post(runnable!!)

        val now = ZonedDateTime.now()

        RecordState.currentLogic.value = Config.locationIterationLogic.get(this@RecordService).let {
            if (it == LocationIterationLogic.Default) LocationIterationLogic.LookBehindAhead_v1
            else it
        }

        iterationLogData.append("CURRENT ITERATION LOGIC: ${RecordState.currentLogic.value}")
        iterationLogData.append("")
        iterationLogData.save(this)

        if (!(shakeToStartEnabled && !eventPoint)) {
            startIntervalVibration()
        }

        recordingStarted = true

        serviceScope.launch {
            this@RecordService.eventPoint = eventPoint

            RecordState.status.value = RecordStatus.BUSY
            requestTileListeningState()

            vibrateEnabled = Config.recordVibrateEnabled.get(this@RecordService)
            vibrateEveryUpdate = Config.recordVibrateEveryUpdate.get(this@RecordService)
            vibrateInterval = Config.recordVibrateInterval.get(this@RecordService)
            vibrateDuration = Config.recordVibrateDuration.get(this@RecordService).toLong()

            if (eventId != null) {
                event = dao.getEventById(eventId)?.toHistoryEvent() ?: event
                event.locationPath?.let {
                    locationPath = it
                    locationFile = File(filesDir, "locations/$locationPath")
                    locationFileReader = if (locationFile.exists()) RandomAccessFile(locationFile, "r") else null
                    eventLocations.addAll(
                        // TODO: handle new format
                        getLocationFile(locationPath, applicationContext).locations
                    )
                    // TODO: unused
                    lastIterationIndex = eventLocations.size - 1
                }
            }

            val collectAccel =
                Config.developerCollectAccelerometer.get(applicationContext) &&
                Config.developerModeEnabled.get(applicationContext)

            if (collectAccel) {
                val accelMetadata = this@RecordService.event.metadata["accel_data"] as? String ?: ""

                this@RecordService.event.metadata["accel_data"] = accelMetadata
                    .split(",")
                    .filter { it.isNotBlank() }
                    .let {
                        it + accelDataFile.absolutePath
                    }
                    .joinToString(",")
            }

            AppDatabase.ensureDayExists(dao, event.time.toLocalDate())
            dao.insertEvent(event.toEventEntity())

            // Start location updates
            // Stopped from stopEventRecording
            startLocationUpdates()

            RecordState.eventId.value = event.id

            locationLogData.append("Title: ${event.title}\n")
            locationLogData.append("Start: $now")
            locationLogData.append("\n")
            locationLogData.append(
                "TIME".padEnd(10) +
                        "valid".padEnd(7) +
                        "updates".padEnd(9) +
                        "got".padEnd(5) +
                        "current".padEnd(9) +
                        "elapsed".padEnd(9) +
                        "acc".padEnd(18)
            )
            locationLogData.save(applicationContext)

            val recordServiceCount = event.metadata["record_service_count"] as? Double ?: 0.0
            val recordUpdates = event.metadata["record_updates"] as? Double ?: 0.0

            event.metadata["record_service_count"] = recordServiceCount + 1
            updates = recordUpdates.toInt()

            event.locationPath = locationPath

            // TODO: new signing
            event.generateSignature(applicationContext, true)

            if (includeAudio) startAudioRecording()
            // else if (!eventPoint && !shakeToStartEnabled) RecordState.status.value = RecordStatus.RUNNING

            requestTileListeningState()

            Log.d("RecordService", "Shake detection enabled: $shakeDetectionEnabled, Shake to start: $shakeToStartEnabled, eventPoint: $eventPoint")

            delay(500)
            if (!eventPoint) RecordState.status.value = RecordStatus.RUNNING
        }

        RecordState.isRecording.value = true
    }

    private suspend fun stopEventRecording() {
        runnable?.let {
            handler.removeCallbacks(it)
        }
        stopIntervalVibration()

        if (!continueService) {
            RecordState.isRecording.value = false
            RecordState.status.value = RecordStatus.BUSY
        }
        else {
            RecordState.status.value = RecordStatus.RESTARTING
        }
        requestTileListeningState()

        notificationTitle = "Processing"
        notificationText = "Saving event..."
        updateNotification(recording = false)

        Log.d("RecordService", "Service [stopEventRecording]")
        resetLocationRetry()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

        // iterateLocations()
        if (includeAudio && mediaRecorder != null) stopAudioRecording()

        if (
            event is EventRange && (
                    Duration.between(event.time.toInstant(), Instant.now()) < Duration.ofSeconds(5)
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

        if (event is EventRange) {
            (event as EventRange).end = ZonedDateTime.now()
        }

        if (eventLocations.isEmpty()) {
            event.locationPath = null
            locationFile.delete()
        } else {
            event.locationPath = locationPath
        }

        // if (!recordingStarted) {
        //     event.metadata["recording_not_started"] = true
        // }

        // Stop previous signature generation logic
        if (signatureJob != null && signatureJob?.isActive == true) try {
            val message = "Signature took way too long to be generated"
            Log.w("RecordService", message)
            logService("PROCESS TOO LONG: (SIGN + INSERT) in locationUpdate, STOPPING")
            signatureJob?.cancel(message)
        } catch (e: Throwable) {
            Log.w("RecordService", e.message ?: "Unknown error")
            Log.w("RecordService", e.stackTraceToString())
        }

        // Add 'unnamed metadata'
        event.metadata["unnamed"] = true

        // Add detected transport mode
        val minimumLocationCount = 3
        if (eventLocations.size > minimumLocationCount) {
            val models = loadCalibrationModels(applicationContext)
            val detected = detectTransportMode(
                history = eventLocations.getLocationData(),
                modelMap = models,
            )

            if (detected != TransportMode.UNKNOWN) {
                event.metadata["transport_detection_unconfirmed"] = true
                (event as? EventRange)?.transportationType = detected.toTransportationType()
            }
        }

        withContext(Dispatchers.IO) {
            val t1 = Instant.now()

            iterateLocations(final = true)

            val t2 = Instant.now()

            // TODO: new signing
            val plusWait = signatureJob != null && signatureJob?.isActive == true
            signatureJob?.join()

            processSignature(funcLabel = "stopEventRecording", noLimit = true)
            event.updateModifiedTime()

            dao.insertEvent(event.toEventEntity())
            val t3 = Instant.now()

            val d1 = t2.toEpochMilli() - t1.toEpochMilli()
            val d2 = t3.toEpochMilli() - t2.toEpochMilli()

            logService("(ITERATE_LOCATION) in stopEventRecording: ${d1}ms")
            logService("(SIGN + INSERT) in stopEventRecording: ${d2}ms ${if (plusWait) "(+JOIN)" else ""}")

            Log.d("RecordService", "Signing and insertion done in ${d2}ms")

            signatureJob = null
        }

        if (!continueService && !RecordState.isRecording.value) withContext(Dispatchers.Main) {
            Toast.makeText(this@RecordService, "Event recording stopped", Toast.LENGTH_SHORT)
                .show()
        }

        if (!continueService) {
            RecordState.status.value = RecordStatus.IDLE
            RecordState.eventId.value = null
        }

        // RecordState.isRunning.value is still true at this point,
        // set to false in stopRecordService()
    }

    private fun startRecordService(intent: Intent?) {
        Log.d("RecordService", "Starting service...")
        // Log.d("RecordService", "Recording event...")

        shakeDetectionEnabled = Config.recordShakeEnabled.get(this@RecordService)
        shakeToStartEnabled = Config.recordShakeToStart.get(this@RecordService) && shakeDetectionEnabled
        recordingStarted = false

        if (shakeDetectionEnabled && intent?.action != ACTION_RECORD_STOP) {
            Log.d("RecordService", "Attempting to start shake detection...")
            startShakeDetection()
        } else {
            Log.d("RecordService", "Shake detection not started - enabled: $shakeDetectionEnabled")
        }

        // General state update
        requestTileListeningState()

        // Preserve status if it is RESTARTING
        RecordState.status.apply {
            if (value != RecordStatus.RESTARTING) {
                value = RecordStatus.BUSY
            }
        }
        RecordState.isRunning.value = true

        // Handle command from intent action
        when (intent?.action) {
            // Stop event recording
            ACTION_RECORD_STOP -> {
                Log.d("RecordService", "ACTION: ACTION_RECORD_STOP")
                RecordState.status.value = RecordStatus.BUSY
                requestTileListeningState()

                // Stop event recording in service coroutine and keep the service running
                serviceScope.launch {
                    // Main logic
                    stopEventRecording()

                    // If shake-to-start is disabled or not started as idle,
                    // stop the service immediately
                    if (!shakeToStartEnabled || !startedAsIdle)
                        stopRecordService()
                    else
                        showIdleNotification()
                }
            }

            // Start event recording
            ACTION_RECORD_START -> {
                Log.d("RecordService", "ACTION: ACTION_RECORD_START")
                includeAudio = intent.getBooleanExtra("includeAudio", false)
                startedAsIdle = intent.getBooleanExtra("startedAsIdle", false)

                if (includeAudio) {
                    startForeground(1, getNotification(notificationTitle, notificationText))
                } else {
                    startForeground(1, getNotification(notificationTitle, notificationText), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                }

                // Main logic
                startEventRecording(
                    intent.getBooleanExtra("eventPoint", false),
                    intent.getStringExtra("eventId")
                )
            }

            // Start service as idle
            ACTION_SERVICE_IDLE -> {
                if (RecordState.status.value != RecordStatus.READY) {
                    Log.w("RecordService", "Invalid record status to start idle: ${RecordState.status.value}")
                }

                includeAudio = false
                startForeground(1, getNotification(notificationTitle, notificationText), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

                showIdleNotification()
                RecordState.status.value = RecordStatus.IDLE
                startedAsIdle = true
                Log.d("RecordService", "ACTION: ACTION_SERVICE_IDLE")
            }

            // Stop service
            ACTION_SERVICE_STOP -> {
                Log.d("RecordService", "ACTION: ACTION_SERVICE_STOP")

                // Launch coroutine
                serviceScope.launch {
                    // If the recording is ongoing, stop the recording
                    if (RecordState.isRecording.value) {
                        // Wait until event recording is fully stopped
                        stopEventRecording()
                    }
                    stopRecordService()
                }
            }

            // Restart service
            ACTION_SERVICE_RESTART -> {
                Log.d("RecordService", "ACTION: ACTION_SERVICE_RESTART")
                stopRecordService(restart = true)
            }

            else -> {
                Log.d("RecordService", "Action is not defined properly!")
            }
        }
    }

    private fun stopRecordService(restart: Boolean = false) {
        Log.d("RecordService", "Service [stopService]")

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }

        runnable?.let {
            handler.removeCallbacks(it)
        }

        // Stop shake detection only when the service is stopped
        stopShakeDetection()

        debugNotification()
        notificationManager.cancel(1)

        recordingStarted = false
        shakeDetectionEnabled = Config.recordShakeEnabled.get(applicationContext)
        shakeToStartEnabled = Config.recordShakeToStart.get(applicationContext) && shakeDetectionEnabled

        // If restart == true, continue service = true
        continueService = continueService || (restart && RecordState.isRecording.value)

        try {
            serviceJob.cancel()
        } catch (e: Exception) {
            Log.d("RecordService", "Service scope already cancelled")
        }
        // TODO


        // Restart when continueService is true
        if (RecordState.isRecording.value && continueService && !eventPoint) {
            val restartIntent = Intent(this, this::class.java)
            restartIntent.putExtra("eventId", event.id)
            restartIntent.putExtra("path", getFilePathFromDate(event.time.toLocalDate()))
            restartIntent.putExtra("includeAudio", false)
            restartIntent.putExtra("startedAsIdle", startedAsIdle)
            RecordState.status.value = RecordStatus.RESTARTING
            requestTileListeningState()

            CoroutineScope(Dispatchers.Default).launch {
                stopEventRecording()
                restartIntent.action = ACTION_RECORD_START

                delay(200)

                startForegroundService(restartIntent)
            }
        }
        // Restart when the status is currently IDLE
        else if (restart && shakeToStartEnabled) {
            Log.d("RecordService", "Restarting...")

            val restartIntent = Intent(this, this::class.java)
            restartIntent.action = ACTION_SERVICE_IDLE

            // Set to busy and update tile state
            RecordState.status.value = RecordStatus.RESTARTING
            requestTileListeningState()

            CoroutineScope(Dispatchers.Default).launch {
                delay(200)

                startForegroundService(restartIntent)
            }
        }
        else {
            RecordState.isRecording.value = false
            RecordState.isRunning.value = false
            RecordState.eventId.value = null
            RecordState.status.value = RecordStatus.READY
            requestTileListeningState()
        }

        stopSelf()
    }

    private suspend fun processSignature(funcLabel: String = "UNKNOWN", noLimit: Boolean = false) = withContext(Dispatchers.IO) {
        var numberOfLines = 0
        val t1 = Instant.now()

        // Data is appended to the list on every iteration

        if (locationFileReader == null && locationFile.exists()) {
            locationFileReader = RandomAccessFile(locationFile, "r")
        }

        if (locationFileReader != null) {
            var data = ""
            var currentLoop = 0
            while (true) { // limit 50 lines read to reduce overhead
                if (currentLoop >= 50 && !noLimit) break

                val line = locationFileReader?.readLineWithNewline()?.toString(Charsets.UTF_8)

                // If read operation returned null (indicate EOF), break the loop
                data += line ?: break

                currentLoop++
                numberOfLines++
            }

            locationDigest.update(data.toByteArray())
        }

        val t2 = Instant.now()

        // TODO: new signing
        event.signature = event.generateSignatureV1(
            context = applicationContext,
            locationDigest =
                if (locationFile.exists()) locationDigest
                else null
        ).joinToString("") { "%02x".format(it) }
        event.updateModifiedTime()

        dao.insertEvent(event.toEventEntity())
        val t3 = Instant.now()

        val d1 = t2.toEpochMilli() - t1.toEpochMilli()
        val d2 = t3.toEpochMilli() - t2.toEpochMilli()

        logService("(LOCATION_DIGEST) in $funcLabel: ${d1}ms, $numberOfLines lines")
        logService("(SIGN + INSERT) in $funcLabel: ${d2}ms")

        Log.d("RecordService", "Signing and insertion done in ${d2}ms")

        // signatureJob = null
    }

    private fun onLocationUpdate(locations: List<Location>) {
        // USELESS CHECK? No, it's not useless, if I just use !RecordService.isRecording.value
        // If shake-to-start is enabled and recording hasn't started yet, ignore location updates (NOPE)
        if (!RecordState.isRecording.value) {
             Log.w("RecordService", "locationUpdate when not recording")
             logService("UPDATE WHEN NOT RECORDING in onLocationUpdate, SKIPPING")
             return
        }

        val recordStatus = RecordState.status.value

        // (BUSY and !eventPoint) or (lower than BUSY despite everything)
        if ((recordStatus == RecordStatus.BUSY && !eventPoint) || recordStatus < RecordStatus.BUSY) {
            Log.w("RecordService", "locationUpdate while the status is BUSY or below")
            logService("UPDATE WHILE " + when (RecordState.status.value) {
                RecordStatus.RESTARTING -> "RESTARTING"
                RecordStatus.BUSY -> "BUSY"
                else -> "[BELOW RESTARTING]"
            })
            return
        }

        Log.d("RecordService", "Location update!")
        Log.d("RecordService", "Data: $locations")

        val accuracyThreshold = Config.locationAccuracyThreshold.get(applicationContext)

        var hasValidUpdate = false

        val zoneId = Calendar.getInstance().timeZone.toZoneId()
        for (location in locations) {
            if (location.latitude != 0.0 && location.longitude != 0.0) {
                Log.d(
                    "RecordService",
                    "Location received: ${location.latitude}, ${location.longitude}"
                )
            } else {
                Log.d("RecordService", "Invalid location data received")
                continue
            }

            if (location.accuracy > accuracyThreshold) continue

            hasValidUpdate = true

            val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(location.time), zoneId)
            locationBuffer.add(
                LocationData(
                    time = time,
                    location = Coordinate(location.latitude, location.longitude),
                    speed = if (location.hasSpeed()) location.speed else null,
                    bearing = if (location.hasBearing()) location.bearing else null,
                    altitude = if (location.hasAltitude()) location.altitude else null,

                    accuracy = if (location.hasAccuracy()) location.accuracy else null,
                    speedAccuracy = if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond else null,
                    bearingAccuracy = if (location.hasBearingAccuracy()) location.bearingAccuracyDegrees else null,
                    verticalAccuracy = if (location.hasVerticalAccuracy()) location.verticalAccuracyMeters else null
                )
            )

            // TODO: move this check
            if (locationBuffer.isNotEmpty() && eventPoint) {
                locationBuffer.appendToLocationFile(locationFile)
                eventLocations.addAll(locationBuffer)
                locationBuffer.clear()
                serviceScope.launch { stopEventRecording() }
                return
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
                        }",
        )

        // Perform vibration on location update
        performVibration(hasValidUpdate)

        // Use notification update from time counter update (every 500ms)
        // updateNotification()

        // val signatureValidBeforeUpdate = event.validateSignature(applicationContext)

        event.metadata["record_updates"] = updates

        event.locationPath = locationPath
        // Log.d(
        //     "RecordService",
        //     "Event signature valid: B: $signatureValidBeforeUpdate, A: ${event.validateSignature(applicationContext)}"
        // )

        // Stop previous signature generation logic
        // if (signatureJob != null && signatureJob?.isActive == true) try {
        //     val message = "Signature took way too long to be generated"
        //     Log.w("RecordService", message)
        //     logService("PROCESS TOO LONG: (SIGN + INSERT) in locationUpdate")
        //     signatureJob?.cancel(message)
        // } catch (e: Throwable) {
        //     Log.w("RecordService", e.message ?: "Unknown error")
        //     Log.w("RecordService", e.stackTraceToString())
        // }

        if (signatureJob != null && signatureJob?.isActive == true) {
            val message = "Event data checkpointing still in progress, skipping new checkpointing"
            Log.w("RecordService", message)
            logService("LONG PROCESS: (WRITE_LOCATION + SIGN + INSERT) in locationUpdate, SKIPPING")
        }

        // Only run this when signature is not being generated
        signatureJob = signatureJob ?: serviceScope.launch {
            processSignature("onLocationUpdate")
        }

        // Iterate locations every 10 updates
        if (updates % 10 == 0) {
            iterateLocations()
        }

        if (Instant.now().toEpochMilli() - startTime > (1000L * maxDuration) && restartEnabled) {
            continueService = true
            stopRecordService()
        }

        locationLogData.append(
            TimeFormatterWithSecond.format(ZonedDateTime.now()).padEnd(10) +
                    "$validUpdates".padEnd(7) +
                    "$updates".padEnd(9) +
                    "${locations.size}".padEnd(5) +
                    eventLocations.size.toString().padEnd(9) +
                    ((Instant.now().toEpochMilli() - startTime) / 1000).toInt().toString()
                        .padEnd(9) +
                    locations.map { it.accuracy.roundToInt() }.joinToString(",")
        )

        locationLogData.save(applicationContext)
    }

    private fun requestTileListeningState() {
        val component = ComponentName(this, RecordTileService::class.java)
        TileService.requestListeningState(this, component)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Make sure the handler doesn't get initialized more than once
        if (!::handler.isInitialized) {
            handler = Handler(mainLooper)
        }

        maxDuration = Config.serviceMaxDuration.get(applicationContext)

        if (intent == null) Log.d("RecordService", "Received null intent")

        startRecordService(intent)

        return when (intent?.action) {
            ACTION_RECORD_STOP, ACTION_RECORD_START -> START_STICKY
            ACTION_SERVICE_IDLE -> START_STICKY
            else -> START_NOT_STICKY
        }
    }

    private fun initNotification() {
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

        actionIntent = Intent(this, RecordService::class.java).apply {
            action = ACTION_RECORD_STOP
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

        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        debugNotificationManager = getSystemService(NotificationManager::class.java)
        debugNotificationManager.createNotificationChannel(debugChannel)

        debugNotificationBuilder = NotificationCompat.Builder(this, "nanhistory_record_debug")
            .setContentTitle("Service Debug")
            .setContentText("Starting...")
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(notificationPendingIntent)

        notificationTitle = "00:00"
        notificationText = "Recording Event"
    }

    private fun startRecording(eventPoint: Boolean = false, eventId: String? = null) {
        val now = ZonedDateTime.now()

        RecordState.currentLogic.value = Config.locationIterationLogic.get(this@RecordService).let {
            if (it == LocationIterationLogic.Default) LocationIterationLogic.LookBehindAhead_v1
            else it
        }

        iterationLogData.append("CURRENT ITERATION LOGIC: ${RecordState.currentLogic.value}")
        iterationLogData.append("")

        iterationLogData.save(this)


        // If shake-to-start is enabled and not an event point, don't start vibration interval yet
        if (!(shakeToStartEnabled && !eventPoint)) {
            startIntervalVibration()
        }

        recordingStarted = true

        serviceScope.launch {
            this@RecordService.eventPoint = eventPoint

            RecordState.status.value = if (shakeToStartEnabled && !eventPoint) RecordStatus.IDLE else RecordStatus.BUSY
            requestTileListeningState()

            // Load vibration settings
            vibrateEnabled = Config.recordVibrateEnabled.get(this@RecordService)
            vibrateEveryUpdate = Config.recordVibrateEveryUpdate.get(this@RecordService)
            vibrateInterval = Config.recordVibrateInterval.get(this@RecordService)
            vibrateDuration = Config.recordVibrateDuration.get(this@RecordService).toLong()

            if (eventId != null) {
                event = dao.getEventById(eventId)?.toHistoryEvent() ?: event
                event.locationPath?.let {
                    locationPath = it
                    locationFile = File(filesDir, "locations/$locationPath")
                    eventLocations.addAll(
                        // TODO: handle new format
                        getLocationFile(locationPath, applicationContext).locations
                    )

                    // TODO: unused
                    lastIterationIndex = eventLocations.size - 1
                }
            }

            AppDatabase.ensureDayExists(dao, event.time.toLocalDate())
            dao.insertEvent(event.toEventEntity())

            startLocationUpdates()

            RecordState.eventId.value = event.id

            locationLogData.append("Title: ${event.title}\n")
            locationLogData.append("Start: $now")
            locationLogData.append("\n")
            locationLogData.append(
                "TIME".padEnd(10) +
                        "valid".padEnd(7) +
                        "updates".padEnd(9) +
                        "got".padEnd(5) +
                        "current".padEnd(9) +
                        "elapsed".padEnd(9) +
                        "acc".padEnd(18)
            )

            locationLogData.save(applicationContext)

            val recordServiceCount =
                matchOrNull<Double>(event.metadata["record_service_count"]) ?: 0.0
            val recordUpdates = matchOrNull<Double>(event.metadata["record_updates"]) ?: 0.0

            event.metadata["record_service_count"] = recordServiceCount + 1
            updates = recordUpdates.toInt()

            event.locationPath = locationPath

            // TODO: new signing
            event.generateSignature(applicationContext, true)

            if (includeAudio) startAudioRecording()
            else if (!eventPoint && !shakeToStartEnabled) RecordState.status.value = RecordStatus.RUNNING

            requestTileListeningState()

            // Log shake detection status
            Log.d("RecordService", "Shake detection enabled: $shakeDetectionEnabled, Shake to start: $shakeToStartEnabled, eventPoint: $eventPoint")
        }

        RecordState.isRecording.value = true
    }

    fun isMicAvailable(): Boolean {
        return try {
            MediaRecorder(applicationContext).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null")
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
        if (!eventPoint) serviceScope.launch {
            delay(1000)
            RecordState.status.value = RecordStatus.RUNNING
            requestTileListeningState()
        }

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
            if (maxDuration > 0) setMaxDuration(maxDuration * 1000)

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

        // TODO: new signing
        event.generateSignature(applicationContext, true)

        serviceScope.launch { dao.insertEvent(event.toEventEntity()) }
    }

    private fun iterateLocations(final: Boolean = false) = runBlocking(Dispatchers.IO) {
        val minimumDistance = Config.locationMinimumDistance.get(applicationContext)

        val appendToLog = { text: String ->
            val now = DateTimeFormatter.ofPattern("HH:mm:ss").format(ZonedDateTime.now())

            iterationLogData.append("$now    $text") // Use space 4 for readability
        }

        if (event is EventRange) {
            // val coordinateMap: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf()
            var prevLocation: Location? = null

            // Start from 5 updates before last iteration
            val startIndex = maxOf(0, lastIterationIndex - 5)
            var currentIndex = 0

            val currentBuffer = locationBuffer.toList()

            // take from sorted locationBuffer
            val items = currentBuffer.sortedBy { it.time }

            Log.d("RecordService", "Iterating from $startIndex to ${eventLocations.size} (${items.size})")
            appendToLog("ITERATE 0 - ${locationBuffer.size.toString().padStart(4)} (${items.size})")

            // TODO: unused
            lastIterationIndex = eventLocations.size - 1

            // Iterate-remove (2 steps)

            for ((index, item) in items.withIndex()) {
                val currentLocation = Location("current").apply {
                    longitude = item.location.longitude
                    latitude = item.location.latitude
                    time = item.time.toInstant().toEpochMilli()
                }

                if (prevLocation != null) {

                    val check = when (RecordState.currentLogic.value) {
                        // Legacy Logic (only look behind and time)
                        // Cons: One direction check making it less accurate
                        LocationIterationLogic.Legacy -> {
                            val time = currentLocation.time.compareTo(prevLocation.time)
                            val distance = currentLocation.distanceTo(prevLocation)
                            (!(distance < minimumDistance && time < 900_000L)).apply {
                                if (!this) {
                                    appendToLog(
                                        "REMOVE ${
                                            DateTimeFormatter.ofPattern("HH:mm:ss").format(item.time)
                                        } BUFFER_SIZE: ${
                                            locationBuffer.size.toString().padStart(3)
                                        } DIST: ${
                                            distance.roundToInt().toString().padStart(4)
                                        } TIME: $time"
                                    )
                                }
                            }
                        }

                        // LookBehindAhead_v1 (compare distance index behind and ahead)
                        // Pros: Two directions check for better accuracy
                        LocationIterationLogic.LookBehindAhead_v1 -> {
                            val distanceBehind = currentLocation.distanceTo(prevLocation)
                            val distanceAhead = items.getOrNull(index + 1)?.let {
                                val aheadLocation = Location("ahead").apply {
                                    longitude = it.location.longitude
                                    latitude = it.location.latitude
                                    time = it.time.toInstant().toEpochMilli()
                                }
                                currentLocation.distanceTo(aheadLocation)
                            }

                            // At least one of behind or ahead distance satisfies the minimum distance
                            if (distanceAhead != null) {
                                (distanceBehind >= minimumDistance || distanceAhead >= minimumDistance).apply {
                                    if (!this) {
                                        appendToLog(
                                            "REMOVE ${
                                                DateTimeFormatter.ofPattern("HH:mm:ss").format(item.time)
                                            } BUFFER_SIZE: ${
                                                locationBuffer.size.toString().padStart(3)
                                            } BEHIND: ${
                                                distanceBehind.roundToInt().toString().padStart(4)
                                            } AHEAD: ${
                                                distanceAhead.roundToInt().toString().padStart(4)
                                            }"
                                        )
                                    }
                                }
                            }
                            else true // If this the last location, then keep it
                        }
                        else -> false // None of them -> no location will be saved
                    }


                    // Remove key if it's too close to the previous location
                    if (!check) {
                        locationBuffer.remove(item)
                        lastIterationIndex--
                        Log.d("RecordService", "Location doesn't satisfy the condition, removed $item (lastIteration: $lastIterationIndex)")
                        continue
                    }
                }
                prevLocation = currentLocation
                // coordinateMap[key] = eventLocations[key]!!
                currentIndex++
            }

            // // Iterate-buffer-replace
            // // This system has been replaced by iterate-remove system
            // eventLocations.clear()
            // for (key in coordinateMap.keys) {
            //     eventLocations[key] = coordinateMap[key]!!
            // }

            val moved = items.let {
                // Move all if it's final iteration
                if (final) it
                // Move processed buffer (except for the last 5 items for re-iteration)
                else it.dropLast(5)
            }
            locationBuffer.removeAll(moved)
            iteratedLocationBuffer.addAll(moved)

            appendToLog(
                "MOVED ${
                    moved.size.toString().padStart(3)
                } (BUFFER: ${
                    locationBuffer.size.toString().padStart(3)
                } ITERATED BUFFER: ${
                    iteratedLocationBuffer.size.toString().padStart(3)
                })"
            )

            iterationLogData.save(this@RecordService)

            // Append data from iterated location buffer
            iteratedLocationBuffer.appendToLocationFile(locationFile)

            processSignature("iterateLocations")

            // Append to main location list
            eventLocations.addAll(iteratedLocationBuffer)

            // Clear iterated location buffer
            iteratedLocationBuffer.clear()
        }
    }

    private fun getNotification(
        title: String,
        text: String,
        locations: String = "",
        recording: Boolean = RecordState.isRecording.value,
        iconActive: Boolean? = false
    ): Notification {
        val iconActive = iconActive ?: false

        val notificationBuilder = NotificationCompat.Builder(this, "nanhistory_record")
            // .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)

        val iconId =
            if (!recording) R.drawable.ic_my_location
            else if (!iconActive) R.drawable.ic_empty
            else R.drawable.ic_my_location_filled

        val bitmap = BitmapFactory.decodeResource(resources, iconId)

        notificationBuilder.setSmallIcon(iconId)
        notificationBuilder.setLargeIcon(bitmap)

        if (recording) {
            val customView = RemoteViews(packageName, R.layout.record_notification).apply {

                setTextViewText(R.id.notification_title, title)
                setTextViewText(R.id.notification_message, text)
                if (locations.isNotBlank()) {
                    setTextViewText(R.id.notification_locations, locations)
                    setViewVisibility(R.id.notification_locations, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.notification_locations, View.INVISIBLE)
                }
                setOnClickPendingIntent(R.id.notification_button, actionPendingIntent)
                setOnClickPendingIntent(R.id.notification_linear_layout, notificationPendingIntent)
                if (eventPoint) setViewVisibility(R.id.notification_button, View.INVISIBLE)
            }

            notificationBuilder.setCustomContentView(customView)
            notificationBuilder.setCustomBigContentView(customView)
        }
        else {
            val stopIntent = Intent(this, this::class.java).apply {
                action = ACTION_SERVICE_STOP
            }
            val restartIntent = Intent(this, this::class.java).apply {
                action = ACTION_SERVICE_RESTART
            }

            val stopPendingIntent = PendingIntent.getForegroundService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val restartPendingIntent = PendingIntent.getForegroundService(
                this,
                0,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.apply {
                setContentTitle(title)
                setContentText(text)
                addAction(R.drawable.ic_stop_filled, "Stop", stopPendingIntent)
                addAction(R.drawable.ic_replay, "Restart", restartPendingIntent)
            }
        }

        return notificationBuilder.build()
    }

    private fun updateNotification(
        title: String? = null,
        text: String? = null,
        description: String? = null,
        notify: Boolean = true,
        recording: Boolean = RecordState.isRecording.value
    ) {
        if (title != null) notificationTitle = title
        if (text != null) notificationText = text
        if (description != null) notificationDescription = description

        val devMode = Config.developerModeEnabled.get(applicationContext)

        val locationSize = eventLocations.size
        val bufferSize = locationBuffer.size

        val notification = getNotification(
            notificationTitle,
            notificationText,
            locations = if (devMode) "($locationSize, $bufferSize)" else "($locationSize)",
            recording = recording,
            iconActive = Instant.now().toEpochMilli() % 1000 >= 500
        )

        if (notify) notificationManager.notify(
            1, notification
        )
    }

    private fun debugNotification(title: String? = null, text: String? = null, separate: Boolean = false) {
        if (title != null) debugNotificationBuilder.setContentTitle(title)
        if (text != null) debugNotificationBuilder.setContentText(text)

        val enabled = Config.developerServiceDebug.get(applicationContext)

        if (enabled) {
            debugNotificationManager.notify(2, debugNotificationBuilder.build())
        }
    }

    private fun startLocationUpdates() {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("RecordService", "Access not granted! Stopping service...")
            stopSelf()
            return
        }

        Log.d("RecordService", "Requesting location updates (retry attempt: $locationRetryCount)")

        val secondsInterval = if (!eventPoint) Config.locationUpdateInterval.get(applicationContext)
        else 1

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            secondsInterval * 1000L
        ).build()

        locationTaskHandled = false

        val task = fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )

        task.addOnSuccessListener {
            if (!locationTaskHandled) {
                locationTaskHandled = true
                locationRetryCount = 0
                Log.d("RecordService", "Location updates started successfully")
            }
        }

        task.addOnFailureListener { exception ->
            if (!locationTaskHandled) {

                logServiceError(exception.message ?: "Unknown error")
                logServiceError(exception.stackTraceToString())

                logService("")
                logService("=================================================")
                logService("ERROR: ${exception.message ?: "Unknown error"}")
                logService("=================================================")
                logService("")

                locationTaskHandled = true
                Log.e("RecordService", "Location update failed: $exception")
                scheduleLocationRestart()
            }
        }

        task.addOnCanceledListener {
            if (!locationTaskHandled) {
                locationTaskHandled = true
                Log.d("RecordService", "Location updates canceled")
                scheduleLocationRestart()
            }
        }
    }

    private fun scheduleLocationRestart() {
        // Only restart if recording is still active
        if (!RecordState.isRecording.value) {
            Log.d("RecordService", "Recording not active, not restarting location updates")
            return
        }


        locationRetryCount++
        val delayMs = locationRetryDelayMs * locationRetryCount

        Log.d("RecordService", "Scheduling location update restart in ${delayMs}ms (attempt $locationRetryCount)")

        serviceScope.launch {
            delay(delayMs)
            if (RecordState.isRecording.value) {
                Log.d("RecordService", "Restarting location updates...")
                startLocationUpdates()
            }
        }
    }

    private fun resetLocationRetry() {
        locationRetryCount = 0
        locationTaskHandled = false
    }

    private fun handleGlobalError(throwable: Throwable, source: String) {
        Log.e("RecordService", "Global error caught from $source: ${throwable.message}", throwable)

        logService("")
        logService("=================================================")
        logService("ERROR: ${throwable.message ?: "Unknown error"}")
        logService("=================================================")
        logService("")

        logServiceError("ERROR from $source: ${throwable.message}")
        logServiceError(throwable.stackTraceToString())

        // Only restart if recording is active
        if (RecordState.isRecording.value) {
            Log.e("RecordService", "Error during recording. Attempting restart...")
            serviceScope.launch {
                try {
                    continueService = true
                    stopEventRecording()
                } catch (e: Exception) {
                    Log.e("RecordService", "Error during stopEventRecording: ${e.message}")
                }

                // Schedule service restart
                scheduleServiceRestart()
            }
        } else if (RecordState.isRunning.value) {
            Log.e("RecordService", "Error in idle service. Restarting...")
            scheduleServiceRestart()
        }
    }

    private fun scheduleServiceRestart() {
        Log.d("RecordService", "Scheduling service restart...")

        serviceScope.launch {
            delay(1000) // Wait 1 second before restart

            try {
                // val restartIntent = Intent(this@RecordService, this@RecordService::class.java)

                // // Determine which action to use
                // restartIntent.action = when {
                //     RecordState.isRecording.value -> ACTION_RECORD_START
                //     shakeToStartEnabled && startedAsIdle -> ACTION_SERVICE_IDLE
                //     else -> ACTION_SERVICE_STOP
                // }

                // // If restarting recording, pass event ID
                // if (RecordState.isRecording.value && ::event.isInitialized) {
                //     restartIntent.putExtra("eventId", event.id)
                //     restartIntent.putExtra("includeAudio", false)
                // }

                Log.d("RecordService", "Starting service with stopRecordService(restart = true)")
                // startForegroundService(restartIntent)
                stopRecordService(restart = true)
            } catch (e: Exception) {
                Log.e("RecordService", "Failed to restart service: ${e.message}")
                logServiceError("FAILED TO RESTART SERVICE:")
                logServiceError("Message: ${e.message ?: "no message"}")
                logServiceError(e.stackTraceToString())
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (RecordState.status.value != RecordStatus.READY && RecordState.status.value != RecordStatus.RESTARTING) {
            stopRecordService()
        }

        // stopEventRecording()
        // stopRecordService() // stopSelf() called from stopRecordService()
    }
}