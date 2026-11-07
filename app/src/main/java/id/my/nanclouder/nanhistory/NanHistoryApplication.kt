package id.my.nanclouder.nanhistory

import android.app.Application
import android.content.Context
import id.my.nanclouder.nanhistory.core.common.di.CoreContainer
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.navigation.IntentRegistry
import id.my.nanclouder.nanhistory.core.common.security.EventLocationDigest
import id.my.nanclouder.nanhistory.core.common.security.EventSignatureVerifier
import id.my.nanclouder.nanhistory.core.common.security.EventSignatures
import id.my.nanclouder.nanhistory.di.AppContainer
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.navigation.AppIntentProvider
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.signature.generateSignatureV2
import id.my.nanclouder.nanhistory.utils.signature.validateSignature

class NanHistoryApplication : Application(), CoreContainerProvider {
    lateinit var container: AppContainer
        private set

    override val coreContainer: CoreContainer
        get() = container

    override fun onCreate() {
        super.onCreate()
        IntentRegistry.register(AppIntentProvider())
        EventSignatures.register(object : EventSignatureVerifier {
            override suspend fun generateSignature(context: Context, event: HistoryEvent, apply: Boolean): String =
                event.generateSignature(context, apply)

            override suspend fun validateSignature(context: Context, event: HistoryEvent): Boolean =
                event.validateSignature(context)

            override suspend fun generateSignatureV2Incremental(
                context: Context,
                event: HistoryEvent,
                locationDigest: EventLocationDigest?
            ): String {
                return (event.generateSignatureV2(
                    context = context,
                    locationDigest = locationDigest
                )).joinToString("") { "%02x".format(it) }
            }
        })
        container = AppContainer(this)
        id.my.nanclouder.nanhistory.core.common.transport.TransportTrainers.register(object : id.my.nanclouder.nanhistory.core.common.transport.TransportTrainer {
            override suspend fun train(
                context: Context,
                existing: Map<id.my.nanclouder.nanhistory.domain.logic.transport.TransportMode, id.my.nanclouder.nanhistory.domain.logic.transport.CalibrationModel>
            ): id.my.nanclouder.nanhistory.domain.logic.transport.TrainingResult {
                val res = id.my.nanclouder.nanhistory.utils.transportModel.train(context, existing)
                return id.my.nanclouder.nanhistory.domain.logic.transport.TrainingResult(
                    accuracy = res.accuracy,
                    totalSamples = res.totalSamples,
                    correctSamples = res.correctSamples,
                    models = res.models
                )
            }
        })
    }
}

