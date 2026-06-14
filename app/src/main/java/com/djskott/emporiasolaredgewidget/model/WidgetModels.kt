package com.djskott.emporiasolaredgewidget.model

enum class BalanceColor {
    GREEN,
    YELLOW,
    RED,
    NEUTRAL,
}

data class WidgetConfig(
    val emporiaEmail: String = "",
    val emporiaPassword: String = "",
    val solarEdgeApiKey: String = "",
    val solarEdgeSiteId: String? = null,
    val emporiaDeviceId: String? = null,
    val emporiaCircuitIds: List<String> = emptyList(),
) {
    val hasCredentials: Boolean
        get() = emporiaEmail.isNotBlank() &&
            emporiaPassword.isNotBlank() &&
            solarEdgeApiKey.isNotBlank()
}

data class WidgetSnapshot(
    val homeLoadKw: Double? = null,
    val solarProductionKw: Double? = null,
    val netKw: Double? = null,
    val balanceColor: BalanceColor = BalanceColor.NEUTRAL,
    val statusMessage: String = "Tap to configure the widget.",
    val lastUpdatedEpochMs: Long? = null,
    val configReady: Boolean = false,
) {
    companion object {
        fun notConfigured(message: String = "Tap to configure the widget."): WidgetSnapshot {
            return WidgetSnapshot(
                statusMessage = message,
                configReady = false,
            )
        }
    }
}

data class RefreshResult(
    val resolvedConfig: WidgetConfig,
    val snapshot: WidgetSnapshot,
)

