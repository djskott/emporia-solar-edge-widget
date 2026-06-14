package com.djskott.emporiasolaredgewidget.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.djskott.emporiasolaredgewidget.data.SecureConfigRepository
import com.djskott.emporiasolaredgewidget.data.WidgetSnapshotStore
import com.djskott.emporiasolaredgewidget.domain.EnergySnapshotService
import com.djskott.emporiasolaredgewidget.model.BalanceColor
import com.djskott.emporiasolaredgewidget.model.WidgetSnapshot

class WidgetRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val configRepository = SecureConfigRepository(applicationContext)
        val snapshotStore = WidgetSnapshotStore(applicationContext)
        val config = configRepository.load()

        if (config == null || !config.hasCredentials) {
            snapshotStore.save(WidgetSnapshot.notConfigured())
            EmporiaSolarEdgeWidget().updateAll(applicationContext)
            return Result.success()
        }

        return try {
            val refreshResult = EnergySnapshotService(applicationContext).refresh(config)
            configRepository.save(refreshResult.resolvedConfig)
            snapshotStore.save(refreshResult.snapshot)
            EmporiaSolarEdgeWidget().updateAll(applicationContext)
            Result.success()
        } catch (error: Exception) {
            snapshotStore.save(
                WidgetSnapshot(
                    balanceColor = BalanceColor.NEUTRAL,
                    statusMessage = error.message ?: "Refresh failed.",
                    lastUpdatedEpochMs = System.currentTimeMillis(),
                    configReady = true,
                ),
            )
            EmporiaSolarEdgeWidget().updateAll(applicationContext)
            Result.success()
        }
    }
}

