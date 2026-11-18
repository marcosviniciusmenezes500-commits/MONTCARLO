package com.droidscope.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "droidscope_preferences")

val DEFAULT_DASHBOARD_ORDER = listOf("DEVICE", "BATTERY", "CPU", "MEMORY", "STORAGE", "NETWORK")

data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val samplingIntervalMs: Long = 2000L,
    val tempAlertThresholdC: Double = 42.0,
    val retentionDays: Int = 30,
    val backgroundMonitoringEnabled: Boolean = false,
    val dashboardCardOrder: List<String> = DEFAULT_DASHBOARD_ORDER,
    val hiddenDashboardCards: Set<String> = emptySet()
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SAMPLING_INTERVAL_MS = longPreferencesKey("sampling_interval_ms")
        val TEMP_ALERT_THRESHOLD_C = doublePreferencesKey("temp_alert_threshold_c")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        val DASHBOARD_ORDER = stringPreferencesKey("dashboard_order")
        val DASHBOARD_HIDDEN = stringSetPreferencesKey("dashboard_hidden")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val rawOrder = prefs[Keys.DASHBOARD_ORDER]
        val parsedOrder = rawOrder
            ?.split(",")
            ?.filter { it in DEFAULT_DASHBOARD_ORDER }
            ?.distinct()
            ?.let { existing -> existing + DEFAULT_DASHBOARD_ORDER.filterNot { it in existing } }
            ?: DEFAULT_DASHBOARD_ORDER

        UserPreferences(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            samplingIntervalMs = prefs[Keys.SAMPLING_INTERVAL_MS] ?: 2000L,
            tempAlertThresholdC = prefs[Keys.TEMP_ALERT_THRESHOLD_C] ?: 42.0,
            retentionDays = prefs[Keys.RETENTION_DAYS] ?: 30,
            backgroundMonitoringEnabled = prefs[Keys.BACKGROUND_MONITORING] ?: false,
            dashboardCardOrder = parsedOrder,
            hiddenDashboardCards = prefs[Keys.DASHBOARD_HIDDEN] ?: emptySet()
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setSamplingIntervalMs(intervalMs: Long) {
        context.dataStore.edit { it[Keys.SAMPLING_INTERVAL_MS] = intervalMs }
    }

    suspend fun setTempAlertThresholdC(thresholdC: Double) {
        context.dataStore.edit { it[Keys.TEMP_ALERT_THRESHOLD_C] = thresholdC }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { it[Keys.RETENTION_DAYS] = days }
    }

    suspend fun setBackgroundMonitoring(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_MONITORING] = enabled }
    }

    suspend fun saveDashboardLayout(order: List<String>, hidden: Set<String>) {
        context.dataStore.edit {
            it[Keys.DASHBOARD_ORDER] = order.joinToString(",")
            it[Keys.DASHBOARD_HIDDEN] = hidden
        }
    }

    suspend fun resetDashboardLayout() {
        context.dataStore.edit {
            it[Keys.DASHBOARD_ORDER] = DEFAULT_DASHBOARD_ORDER.joinToString(",")
            it[Keys.DASHBOARD_HIDDEN] = emptySet()
        }
    }
}
