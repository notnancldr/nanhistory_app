package id.my.nanclouder.nanhistory.domain.logic.transport

data class CalibrationModel(
    // --- Range-based scoring (original) ---
    val avgSpeedMin: Float,
    val avgSpeedMax: Float,

    val topSpeedMin: Float,
    val topSpeedMax: Float,

    val avgAccelMin: Float,
    val avgAccelMax: Float,

    val stopDurationMin: Float,
    val stopDurationMax: Float,

    val distanceMin: Float,
    val distanceMax: Float,

    val speedVarianceMin: Float,
    val speedVarianceMax: Float,

    val accelVarianceMin: Float,
    val accelVarianceMax: Float,

    val pathComplexityMin: Float,
    val pathComplexityMax: Float,

    // --- Coasting duration parameters (new) ---
    // Maximum coasting duration (no acceleration) at different thresholds
    val maxCoastingDuration05Min: Float,      // Threshold: 0.5 m/s²
    val maxCoastingDuration05Max: Float,

    val maxCoastingDuration10Min: Float,      // Threshold: 1.0 m/s²
    val maxCoastingDuration10Max: Float,

    val maxCoastingDuration20Min: Float,      // Threshold: 2.0 m/s²
    val maxCoastingDuration20Max: Float,

    val maxCoastingDuration40Min: Float,      // Threshold: 4.0 m/s²
    val maxCoastingDuration40Max: Float,

    // --- Ideal values ---
    val idealAvgSpeed: Float? = null,
    val idealTopSpeed: Float? = null,
    val idealAvgAccel: Float? = null,
    val idealStopDuration: Float? = null,
    val idealDistance: Float? = null,
    val idealSpeedVariance: Float? = null,
    val idealAccelVariance: Float? = null,
    val idealPathComplexity: Float? = null,
    val idealMaxCoastingDuration05: Float? = null,
    val idealMaxCoastingDuration10: Float? = null,
    val idealMaxCoastingDuration20: Float? = null,
    val idealMaxCoastingDuration40: Float? = null,

    // --- Strategy configuration ---
    val rangeWeight: Float? = 0.5f,

    // --- Feature Importance Weights (learned during training) ---
    val avgSpeedWeight: Float = 1.0f,
    val topSpeedWeight: Float = 0.8f,
    val avgAccelWeight: Float = 0.7f,
    val stopDurationWeight: Float = 0.6f,
    val distanceWeight: Float = 0.5f,
    val speedVarianceWeight: Float = 0.8f,
    val accelVarianceWeight: Float = 0.7f,
    val pathComplexityWeight: Float = 0.4f,
    val maxCoastingDuration05Weight: Float = 0.6f,
    val maxCoastingDuration10Weight: Float = 0.6f,
    val maxCoastingDuration20Weight: Float = 0.6f,
    val maxCoastingDuration40Weight: Float = 0.6f,

    // --- Per-Mode Bias ---
    val modeBias: Float = 0.0f,

    // --- Confidence Threshold ---
    val confidenceThreshold: Float = 0.3f,

    // Model weight—higher means more confidence
    val weight: Float = 1f
) { companion object }
