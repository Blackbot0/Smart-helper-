package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppPreset
import com.example.data.model.RuleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.ruleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_job_helper_rules"
)

class RuleRepository(private val context: Context) {

    private object Keys {
        val SELECTED_PRESET_ID = stringPreferencesKey("selected_preset_id")
        val TARGET_PACKAGE_NAME = stringPreferencesKey("target_package_name")
        val ACTION_KEYWORDS = stringPreferencesKey("action_keywords")
        val MIN_FARE_ENABLED = booleanPreferencesKey("min_fare_enabled")
        val MIN_FARE_AMOUNT = doublePreferencesKey("min_fare_amount")
        val MAX_DISTANCE_ENABLED = booleanPreferencesKey("max_distance_enabled")
        val MAX_DISTANCE_KM = doublePreferencesKey("max_distance_km")
        val MIN_PER_KM_ENABLED = booleanPreferencesKey("min_per_km_enabled")
        val MIN_PER_KM_RATE = doublePreferencesKey("min_per_km_rate")
        val SKIP_IF_UNDETECTED = booleanPreferencesKey("skip_if_undetected")
        val REACTION_DELAY_MS = longPreferencesKey("reaction_delay_ms")
        val COOLDOWN_MS = longPreferencesKey("cooldown_ms")
        val OVERLAY_AUTO_RESTART = booleanPreferencesKey("overlay_auto_restart")
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
    }

    val ruleConfigFlow: Flow<RuleConfig> = context.ruleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            RuleConfig(
                selectedPresetId = prefs[Keys.SELECTED_PRESET_ID] ?: AppPreset.RAPIDO.id,
                targetPackageName = prefs[Keys.TARGET_PACKAGE_NAME] ?: AppPreset.RAPIDO.packageName,
                actionKeywords = prefs[Keys.ACTION_KEYWORDS] ?: AppPreset.RAPIDO.defaultKeywords,
                minFareEnabled = prefs[Keys.MIN_FARE_ENABLED] ?: true,
                minFareAmount = prefs[Keys.MIN_FARE_AMOUNT] ?: 120.0,
                maxDistanceEnabled = prefs[Keys.MAX_DISTANCE_ENABLED] ?: true,
                maxDistanceKm = prefs[Keys.MAX_DISTANCE_KM] ?: 3.5,
                minPerKmEnabled = prefs[Keys.MIN_PER_KM_ENABLED] ?: false,
                minPerKmRate = prefs[Keys.MIN_PER_KM_RATE] ?: 15.0,
                skipIfUndetected = prefs[Keys.SKIP_IF_UNDETECTED] ?: true,
                reactionDelayMs = prefs[Keys.REACTION_DELAY_MS] ?: 150L,
                cooldownMs = prefs[Keys.COOLDOWN_MS] ?: 1500L,
                overlayAutoRestart = prefs[Keys.OVERLAY_AUTO_RESTART] ?: false,
                darkThemeEnabled = prefs[Keys.DARK_THEME_ENABLED] ?: true
            )
        }

    suspend fun getSnapshot(): RuleConfig = ruleConfigFlow.first()

    suspend fun applyPreset(preset: AppPreset) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.SELECTED_PRESET_ID] = preset.id
            prefs[Keys.TARGET_PACKAGE_NAME] = preset.packageName
            prefs[Keys.ACTION_KEYWORDS] = preset.defaultKeywords
        }
    }

    suspend fun updateTargetPackage(packageName: String) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.TARGET_PACKAGE_NAME] = packageName.trim()
            val matchingPreset = AppPreset.entries.firstOrNull {
                it.packageName.equals(packageName.trim(), ignoreCase = true)
            }
            prefs[Keys.SELECTED_PRESET_ID] = matchingPreset?.id ?: AppPreset.CUSTOM_ALL.id
        }
    }

    suspend fun updateActionKeywords(keywords: String) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.ACTION_KEYWORDS] = keywords
        }
    }

    suspend fun updateMinFareEnabled(enabled: Boolean) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.MIN_FARE_ENABLED] = enabled
        }
    }

    suspend fun updateMinFareAmount(amount: Double) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.MIN_FARE_AMOUNT] = amount.coerceAtLeast(0.0)
        }
    }

    suspend fun updateMaxDistanceEnabled(enabled: Boolean) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.MAX_DISTANCE_ENABLED] = enabled
        }
    }

    suspend fun updateMaxDistanceKm(km: Double) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.MAX_DISTANCE_KM] = km.coerceAtLeast(0.1)
        }
    }

    suspend fun updateMinPerKmEnabled(enabled: Boolean) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.MIN_PER_KM_ENABLED] = enabled
        }
    }

    suspend fun updateMinPerKmRate(rate: Double) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.MIN_PER_KM_RATE] = rate.coerceAtLeast(0.0)
        }
    }

    suspend fun updateSkipIfUndetected(skip: Boolean) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.SKIP_IF_UNDETECTED] = skip
        }
    }

    suspend fun updateReactionDelayMs(delayMs: Long) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.REACTION_DELAY_MS] = delayMs.coerceIn(0L, 1000L)
        }
    }

    suspend fun updateCooldownMs(cooldownMs: Long) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.COOLDOWN_MS] = cooldownMs.coerceIn(500L, 5000L)
        }
    }

    suspend fun updateOverlayAutoRestart(enabled: Boolean) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.OVERLAY_AUTO_RESTART] = enabled
        }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.ruleDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME_ENABLED] = enabled
        }
    }
}
