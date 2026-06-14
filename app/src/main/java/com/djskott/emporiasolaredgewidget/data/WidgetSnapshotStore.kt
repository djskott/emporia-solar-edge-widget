package com.djskott.emporiasolaredgewidget.data

import android.content.Context
import com.djskott.emporiasolaredgewidget.model.BalanceColor
import com.djskott.emporiasolaredgewidget.model.WidgetSnapshot

class WidgetSnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WidgetSnapshot {
        val statusName = preferences.getString(KEY_BALANCE_COLOR, BalanceColor.NEUTRAL.name)
            ?: BalanceColor.NEUTRAL.name

        return WidgetSnapshot(
            homeLoadKw = preferences.readNullableDouble(KEY_HOME_LOAD_KW),
            solarProductionKw = preferences.readNullableDouble(KEY_SOLAR_KW),
            netKw = preferences.readNullableDouble(KEY_NET_KW),
            balanceColor = runCatching { BalanceColor.valueOf(statusName) }.getOrDefault(BalanceColor.NEUTRAL),
            statusMessage = preferences.getString(KEY_STATUS_MESSAGE, "Tap to configure the widget.")
                ?: "Tap to configure the widget.",
            lastUpdatedEpochMs = preferences.getLong(KEY_LAST_UPDATED, NO_TIME).takeUnless { it == NO_TIME },
            configReady = preferences.getBoolean(KEY_CONFIG_READY, false),
        )
    }

    fun save(snapshot: WidgetSnapshot) {
        preferences.edit().apply {
            putNullableDouble(KEY_HOME_LOAD_KW, snapshot.homeLoadKw)
            putNullableDouble(KEY_SOLAR_KW, snapshot.solarProductionKw)
            putNullableDouble(KEY_NET_KW, snapshot.netKw)
            putString(KEY_BALANCE_COLOR, snapshot.balanceColor.name)
            putString(KEY_STATUS_MESSAGE, snapshot.statusMessage)
            putLong(KEY_LAST_UPDATED, snapshot.lastUpdatedEpochMs ?: NO_TIME)
            putBoolean(KEY_CONFIG_READY, snapshot.configReady)
        }.apply()
    }

    private fun android.content.SharedPreferences.readNullableDouble(key: String): Double? {
        if (!contains(key)) {
            return null
        }
        return Double.fromBits(getLong(key, 0L))
    }

    private fun android.content.SharedPreferences.Editor.putNullableDouble(key: String, value: Double?) {
        if (value == null) {
            remove(key)
        } else {
            putLong(key, value.toBits())
        }
    }

    companion object {
        private const val PREFS_NAME = "widget_state"
        private const val KEY_HOME_LOAD_KW = "home_load_kw"
        private const val KEY_SOLAR_KW = "solar_kw"
        private const val KEY_NET_KW = "net_kw"
        private const val KEY_BALANCE_COLOR = "balance_color"
        private const val KEY_STATUS_MESSAGE = "status_message"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_CONFIG_READY = "config_ready"
        private const val NO_TIME = -1L
    }
}

