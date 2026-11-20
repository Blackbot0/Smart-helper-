package com.example.data.model

import java.util.UUID

enum class AppPreset(
    val id: String,
    val displayName: String,
    val packageName: String,
    val defaultKeywords: String
) {
    RAPIDO(
        id = "rapido",
        displayName = "Rapido",
        packageName = "com.rapido.rider",
        defaultKeywords = "Accept, Confirm"
    ),
    OLA_DRIVER(
        id = "ola_driver",
        displayName = "Ola Driver",
        packageName = "com.olacabs.oladriver",
        defaultKeywords = "Accept, Start"
    ),
    UBER_DRIVER(
        id = "uber_driver",
        displayName = "Uber Driver",
        packageName = "com.ubercab.driver",
        defaultKeywords = "Accept, Match"
    ),
    CUSTOM_ALL(
        id = "custom_all",
        displayName = "Custom / All",
        packageName = "",
        defaultKeywords = "Accept, Confirm, Pick Up"
    );

    companion object {
        fun fromId(id: String): AppPreset = entries.firstOrNull { it.id == id } ?: CUSTOM_ALL

        fun labelForPackage(packageName: String): String {
            if (packageName.isBlank()) return "All Apps"
            if (packageName == "sandbox.simulation") return "Sandbox"
            return when {
                packageName.contains("rapido", ignoreCase = true) -> "Rapido"
                packageName.contains("ola", ignoreCase = true) -> "Ola Driver"
                packageName.contains("uber", ignoreCase = true) -> "Uber Driver"
                packageName.contains("porter", ignoreCase = true) -> "Porter"
                packageName.contains("swiggy", ignoreCase = true) -> "Swiggy"
                packageName.contains("zomato", ignoreCase = true) -> "Zomato"
                else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }
}

data class RuleConfig(
    val selectedPresetId: String = AppPreset.RAPIDO.id,
    val targetPackageName: String = AppPreset.RAPIDO.packageName,
    val actionKeywords: String = AppPreset.RAPIDO.defaultKeywords,
    val minFareEnabled: Boolean = true,
    val minFareAmount: Double = 120.0,
    val maxDistanceEnabled: Boolean = true,
    val maxDistanceKm: Double = 3.5,
    val minPerKmEnabled: Boolean = false,
    val minPerKmRate: Double = 15.0,
    val skipIfUndetected: Boolean = true,
    val reactionDelayMs: Long = 150L,
    val cooldownMs: Long = 1500L,
    val overlayAutoRestart: Boolean = false,
    val darkThemeEnabled: Boolean = true
) {
    fun parsedKeywords(): List<String> =
        actionKeywords.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun activePillRuleTag(): String = when {
        minFareEnabled -> "≥₹${formatCompactNumber(minFareAmount)}"
        maxDistanceEnabled -> "≤${formatCompactNumber(maxDistanceKm)}km"
        minPerKmEnabled -> "≥₹${formatCompactNumber(minPerKmRate)}/km"
        else -> "KEYWORD"
    }
}

enum class JobOutcome(val label: String) {
    ACCEPTED("Accepted"),
    SKIPPED("Skipped"),
    NOT_DETECTED("Not Detected")
}

enum class HistorySortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_FARE("Highest Fare"),
    SHORTEST_DISTANCE("Shortest Dist"),
    BEST_RATE("Best ₹/km")
}

data class JobHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val sourcePackage: String,
    val sourceLabel: String = AppPreset.labelForPackage(sourcePackage),
    val outcome: JobOutcome,
    val fareAmount: Double?,
    val distanceKm: Double?,
    val ratePerKm: Double?,
    val matchedKeyword: String?,
    val triggeredRules: List<String>,
    val summaryLine: String,
    val clickMethod: String? = null,
    val rawSnippet: String = ""
)

data class PermissionStatus(
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val notificationsGranted: Boolean = true
) {
    val allCoreGranted: Boolean
        get() = accessibilityEnabled && overlayEnabled && batteryOptimizationIgnored

    val grantedCoreCount: Int
        get() = listOf(accessibilityEnabled, overlayEnabled, batteryOptimizationIgnored).count { it }
}

fun formatCompactNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", value)
    }
}
