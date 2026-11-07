package id.my.nanclouder.nanhistory.core.common.transport

import android.content.Context
import id.my.nanclouder.nanhistory.domain.logic.transport.CalibrationModel
import id.my.nanclouder.nanhistory.domain.logic.transport.TransportMode
import id.my.nanclouder.nanhistory.domain.logic.transport.TrainingResult

interface TransportTrainer {
    suspend fun train(
        context: Context,
        existing: Map<TransportMode, CalibrationModel>
    ): TrainingResult
}

object TransportTrainers {
    private var trainer: TransportTrainer? = null

    fun register(trainer: TransportTrainer) {
        this.trainer = trainer
    }

    suspend fun train(
        context: Context,
        existing: Map<TransportMode, CalibrationModel>
    ): TrainingResult = trainer?.train(context, existing) ?: TrainingResult(
        accuracy = 0f,
        totalSamples = 0,
        correctSamples = 0,
        models = existing
    )
}
