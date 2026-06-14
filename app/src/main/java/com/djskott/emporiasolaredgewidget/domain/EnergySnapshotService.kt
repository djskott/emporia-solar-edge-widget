package com.djskott.emporiasolaredgewidget.domain

import android.content.Context
import com.djskott.emporiasolaredgewidget.model.BalanceColor
import com.djskott.emporiasolaredgewidget.model.RefreshResult
import com.djskott.emporiasolaredgewidget.model.WidgetConfig
import com.djskott.emporiasolaredgewidget.model.WidgetSnapshot
import com.djskott.emporiasolaredgewidget.network.EmporiaClient
import com.djskott.emporiasolaredgewidget.network.EmporiaSelection
import com.djskott.emporiasolaredgewidget.network.HttpJsonClient
import com.djskott.emporiasolaredgewidget.network.SolarEdgeClient

class EnergySnapshotService(
    context: Context,
    httpJsonClient: HttpJsonClient = HttpJsonClient(),
) {
    private val appContext = context.applicationContext
    private val emporiaClient = EmporiaClient(httpJsonClient)
    private val solarEdgeClient = SolarEdgeClient(httpJsonClient)

    fun refresh(config: WidgetConfig): RefreshResult {
        if (!config.hasCredentials) {
            throw IllegalStateException("Emporia email, Emporia password, and SolarEdge API key are required.")
        }

        val emporiaSession = emporiaClient.authenticate(config.emporiaEmail, config.emporiaPassword)
        val resolvedSelection = if (!config.emporiaDeviceId.isNullOrBlank() && config.emporiaCircuitIds.isNotEmpty()) {
            EmporiaSelection(
                deviceId = config.emporiaDeviceId,
                circuitIds = config.emporiaCircuitIds,
            )
        } else {
            emporiaClient.discoverMainSelection(emporiaSession)
        }

        val siteId = config.solarEdgeSiteId ?: solarEdgeClient.discoverSiteId(config.solarEdgeApiKey)
        val homeLoadKw = emporiaClient.getCurrentHomeLoadKw(emporiaSession, resolvedSelection)
        val solarKw = solarEdgeClient.getCurrentProductionKw(config.solarEdgeApiKey, siteId)
        val netKw = solarKw - homeLoadKw

        return RefreshResult(
            resolvedConfig = config.copy(
                solarEdgeSiteId = siteId,
                emporiaDeviceId = resolvedSelection.deviceId,
                emporiaCircuitIds = resolvedSelection.circuitIds,
            ),
            snapshot = WidgetSnapshot(
                homeLoadKw = homeLoadKw,
                solarProductionKw = solarKw,
                netKw = netKw,
                balanceColor = balanceFor(netKw),
                statusMessage = "Live data from Emporia and SolarEdge.",
                lastUpdatedEpochMs = System.currentTimeMillis(),
                configReady = true,
            ),
        )
    }

    private fun balanceFor(netKw: Double): BalanceColor {
        return when {
            netKw > YELLOW_BAND_KW -> BalanceColor.GREEN
            netKw < -YELLOW_BAND_KW -> BalanceColor.RED
            else -> BalanceColor.YELLOW
        }
    }

    companion object {
        private const val YELLOW_BAND_KW = 1.0
    }
}

