package com.example.engine

import com.example.data.model.JobOutcome
import com.example.data.model.RuleConfig
import com.example.data.model.formatCompactNumber
import java.util.Locale

data class EvaluationResult(
    val shouldClick: Boolean,
    val outcome: JobOutcome,
    val fareAmount: Double?,
    val distanceKm: Double?,
    val ratePerKm: Double?,
    val matchedKeyword: String?,
    val triggeredRules: List<String>,
    val summaryLine: String
)

object RuleEvaluator {

    fun evaluate(
        metrics: ParsedScreenMetrics,
        matchedKeyword: String?,
        config: RuleConfig
    ): EvaluationResult {
        val fare = metrics.primaryFare
        val distance = metrics.primaryDistanceKm
        val ratePerKm = metrics.ratePerKm

        val passedRules = mutableListOf<String>()
        val failedReasons = mutableListOf<String>()
        val missingReasons = mutableListOf<String>()

        if (matchedKeyword == null) {
            return EvaluationResult(
                shouldClick = false,
                outcome = JobOutcome.NOT_DETECTED,
                fareAmount = fare,
                distanceKm = distance,
                ratePerKm = ratePerKm,
                matchedKeyword = null,
                triggeredRules = listOf("No target keyword button found"),
                summaryLine = buildSummary("No Match", fare, distance, "No keyword button")
            )
        } else {
            passedRules.add("Keyword: $matchedKeyword")
        }

        // 1. Minimum Fare check (>= ₹)
        if (config.minFareEnabled) {
            if (fare == null) {
                missingReasons.add("Fare text not detected")
            } else if (fare >= config.minFareAmount) {
                passedRules.add("Fare ≥ ₹${formatCompactNumber(config.minFareAmount)}")
            } else {
                failedReasons.add(
                    "Below min fare (₹${formatCompactNumber(fare)} < ₹${formatCompactNumber(config.minFareAmount)})"
                )
            }
        }

        // 2. Maximum Distance check (<= km)
        if (config.maxDistanceEnabled) {
            if (distance == null) {
                missingReasons.add("Distance text not detected")
            } else if (distance <= config.maxDistanceKm) {
                passedRules.add("Distance ≤ ${formatCompactNumber(config.maxDistanceKm)} km")
            } else {
                failedReasons.add(
                    "Over max distance (${formatDistance(distance)} > ${formatCompactNumber(config.maxDistanceKm)} km)"
                )
            }
        }

        // 3. Minimum Per-KM Rate check (>= ₹/km)
        if (config.minPerKmEnabled) {
            if (fare == null || distance == null || distance <= 0.0 || ratePerKm == null) {
                missingReasons.add("Fare & distance required for ₹/km check")
            } else if (ratePerKm >= config.minPerKmRate) {
                passedRules.add("Rate ≥ ₹${formatCompactNumber(config.minPerKmRate)}/km")
            } else {
                failedReasons.add(
                    "Low ₹/km rate (₹${formatCompactNumber(ratePerKm)} < ₹${formatCompactNumber(config.minPerKmRate)}/km)"
                )
            }
        }

        // Failed explicit numeric rules always skip the offer
        if (failedReasons.isNotEmpty()) {
            val primaryReason = failedReasons.first()
            return EvaluationResult(
                shouldClick = false,
                outcome = JobOutcome.SKIPPED,
                fareAmount = fare,
                distanceKm = distance,
                ratePerKm = ratePerKm,
                matchedKeyword = matchedKeyword,
                triggeredRules = failedReasons + passedRules,
                summaryLine = buildSummary("Skipped", fare, distance, primaryReason)
            )
        }

        // Missing numeric data handling depends on Safe Mode toggle
        if (missingReasons.isNotEmpty()) {
            if (config.skipIfUndetected) {
                val primaryMissing = missingReasons.first()
                return EvaluationResult(
                    shouldClick = false,
                    outcome = JobOutcome.NOT_DETECTED,
                    fareAmount = fare,
                    distanceKm = distance,
                    ratePerKm = ratePerKm,
                    matchedKeyword = matchedKeyword,
                    triggeredRules = missingReasons + listOf("Safe Mode active"),
                    summaryLine = buildSummary("Not Detected", fare, distance, "$primaryMissing (Safe Mode)")
                )
            } else {
                passedRules.add("Keyword-only fallback enabled")
            }
        }

        return EvaluationResult(
            shouldClick = true,
            outcome = JobOutcome.ACCEPTED,
            fareAmount = fare,
            distanceKm = distance,
            ratePerKm = ratePerKm,
            matchedKeyword = matchedKeyword,
            triggeredRules = passedRules,
            summaryLine = buildSummary("Matched", fare, distance, "CLICKED")
        )
    }

    private fun buildSummary(
        prefix: String,
        fare: Double?,
        distance: Double?,
        resultReason: String
    ): String {
        val metricsParts = buildList {
            if (fare != null) add("₹${formatCompactNumber(fare)}")
            if (distance != null) add(formatDistance(distance))
        }
        val metricSegment = if (metricsParts.isEmpty()) "No metrics" else metricsParts.joinToString(" | ")
        return "$prefix $metricSegment → $resultReason"
    }

    fun formatDistance(km: Double): String {
        return if (km < 1.0) {
            String.format(Locale.US, "%.2f km", km)
        } else {
            String.format(Locale.US, "%.1f km", km)
        }
    }
}
