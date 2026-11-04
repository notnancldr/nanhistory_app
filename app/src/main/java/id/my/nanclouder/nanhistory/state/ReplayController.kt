package id.my.nanclouder.nanhistory.state

import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.toLocationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Manages route playback state, coordinate interpolation, and ticker loop.
 */
class ReplayController(
    private val dao: AppDao,
    private val eventId: String,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val _replayState = MutableStateFlow(ReplayUiState())
    val replayState: StateFlow<ReplayUiState> = _replayState.asStateFlow()

    private var replayTickerJob: Job? = null
    private var cumulativeDistances: DoubleArray = DoubleArray(0)

    fun startReplay() {
        scope.launch(ioDispatcher) {
            val allLocs = dao.getLocationsByEventId(eventId).first().map { it.toLocationData() }
            if (allLocs.size < 2) return@launch

            cumulativeDistances = ReplayInterpolator.computeCumulativeDistances(allLocs)
            val dwells = ReplayInterpolator.detectDwellPoints(allLocs)
            val firstLoc = allLocs.first()
            val initialTelemetry = ReplayInterpolator.interpolateAt(allLocs, firstLoc.time, cumulativeDistances)

            _replayState.value = ReplayUiState(
                isActive = true,
                isPlaying = false,
                progress = 0f,
                currentVirtualTime = firstLoc.time,
                currentTelemetry = initialTelemetry,
                allLocations = allLocs,
                traveledLocations = listOf(firstLoc),
                dwellPoints = dwells
            )
        }
    }

    fun stopReplay() {
        replayTickerJob?.cancel()
        _replayState.value = ReplayUiState(isActive = false)
    }

    fun togglePlayPause() {
        val current = _replayState.value
        if (!current.isActive || current.allLocations.size < 2) return

        if (current.isPlaying) {
            replayTickerJob?.cancel()
            _replayState.update { it.copy(isPlaying = false) }
        } else {
            if (current.progress >= 1.0f) {
                seekReplay(0f)
            }
            _replayState.update { it.copy(isPlaying = true) }
            startTicker()
        }
    }

    fun seekReplay(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val current = _replayState.value
        val locations = current.allLocations
        if (locations.size < 2) return

        val firstMillis = locations.first().time.toInstant().toEpochMilli()
        val lastMillis = locations.last().time.toInstant().toEpochMilli()
        val targetMillis = firstMillis + (clampedProgress * (lastMillis - firstMillis)).toLong()
        val targetTime = locations.first().time.plus(Duration.ofMillis(targetMillis - firstMillis))

        val telemetry = ReplayInterpolator.interpolateAt(locations, targetTime, cumulativeDistances)
        val traveled = locations.filter { it.time.toInstant().toEpochMilli() <= targetMillis }

        _replayState.update {
            it.copy(
                progress = clampedProgress,
                currentVirtualTime = targetTime,
                currentTelemetry = telemetry,
                traveledLocations = traveled
            )
        }
    }

    fun stepTime(seconds: Long) {
        val current = _replayState.value
        val locations = current.allLocations
        if (locations.size < 2) return

        val firstMillis = locations.first().time.toInstant().toEpochMilli()
        val lastMillis = locations.last().time.toInstant().toEpochMilli()
        val totalSpan = (lastMillis - firstMillis).coerceAtLeast(1L)

        val currentMillis = current.currentVirtualTime?.toInstant()?.toEpochMilli() ?: firstMillis
        val targetMillis = (currentMillis + seconds * 1000).coerceIn(firstMillis, lastMillis)
        val progress = ((targetMillis - firstMillis).toDouble() / totalSpan).toFloat()

        seekReplay(progress)
    }

    fun stepWaypoint(direction: Int) {
        val current = _replayState.value
        val locations = current.allLocations
        if (locations.size < 2) return

        val currentMillis = current.currentVirtualTime?.toInstant()?.toEpochMilli()
            ?: locations.first().time.toInstant().toEpochMilli()

        var currentIdx = 0
        for (i in locations.indices) {
            if (locations[i].time.toInstant().toEpochMilli() <= currentMillis) {
                currentIdx = i
            } else {
                break
            }
        }

        val targetIdx = (currentIdx + direction).coerceIn(0, locations.size - 1)
        val firstMillis = locations.first().time.toInstant().toEpochMilli()
        val lastMillis = locations.last().time.toInstant().toEpochMilli()
        val totalSpan = (lastMillis - firstMillis).coerceAtLeast(1L)
        val targetMillis = locations[targetIdx].time.toInstant().toEpochMilli()
        val progress = ((targetMillis - firstMillis).toDouble() / totalSpan).toFloat()

        seekReplay(progress)
    }

    fun setSpeedMultiplier(speed: Float) {
        _replayState.update { it.copy(speedMultiplier = speed) }
    }

    fun toggleFollowCamera() {
        _replayState.update { it.copy(isFollowCamera = !it.isFollowCamera) }
    }

    fun setFollowCamera(enabled: Boolean) {
        _replayState.update { it.copy(isFollowCamera = enabled) }
    }

    fun toggleImmersive() {
        _replayState.update { it.copy(isImmersive = !it.isImmersive) }
    }

    private fun startTicker() {
        replayTickerJob?.cancel()
        replayTickerJob = scope.launch(ioDispatcher) {
            val frameDelayMs = 33L
            var lastWallTime = System.currentTimeMillis()

            while (true) {
                delay(frameDelayMs)
                val currentWallTime = System.currentTimeMillis()
                val wallDeltaMs = currentWallTime - lastWallTime
                lastWallTime = currentWallTime

                val currentState = _replayState.value
                if (!currentState.isActive || !currentState.isPlaying) break
                val locations = currentState.allLocations
                if (locations.size < 2) break

                val firstMillis = locations.first().time.toInstant().toEpochMilli()
                val lastMillis = locations.last().time.toInstant().toEpochMilli()
                val totalDurationMs = (lastMillis - firstMillis).coerceAtLeast(1L)

                val currentVirtualMillis = currentState.currentVirtualTime?.toInstant()?.toEpochMilli() ?: firstMillis
                val advanceMs = (wallDeltaMs * currentState.speedMultiplier).toLong()
                val newVirtualMillis = currentVirtualMillis + advanceMs

                if (newVirtualMillis >= lastMillis) {
                    val endTelemetry = ReplayInterpolator.interpolateAt(
                        locations, locations.last().time, cumulativeDistances
                    )
                    _replayState.update {
                        it.copy(
                            isPlaying = false,
                            progress = 1.0f,
                            currentVirtualTime = locations.last().time,
                            currentTelemetry = endTelemetry,
                            traveledLocations = locations
                        )
                    }
                    break
                } else {
                    val newVirtualTime = locations.first().time.plus(
                        Duration.ofMillis(newVirtualMillis - firstMillis)
                    )
                    val newProgress = ((newVirtualMillis - firstMillis).toDouble() / totalDurationMs).toFloat()
                    val telemetry = ReplayInterpolator.interpolateAt(
                        locations, newVirtualTime, cumulativeDistances
                    )
                    val traveled = locations.filter { it.time.toInstant().toEpochMilli() <= newVirtualMillis }

                    _replayState.update {
                        it.copy(
                            progress = newProgress,
                            currentVirtualTime = newVirtualTime,
                            currentTelemetry = telemetry,
                            traveledLocations = traveled
                        )
                    }
                }
            }
        }
    }
}
