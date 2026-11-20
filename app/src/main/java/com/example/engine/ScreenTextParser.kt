package com.example.engine

import kotlin.math.roundToInt

data class ParsedScreenMetrics(
    val primaryFare: Double?,
    val primaryDistanceKm: Double?,
    val allFares: List<Double>,
    val allDistancesKm: List<Double>
) {
    val ratePerKm: Double?
        get() = if (primaryFare != null && primaryDistanceKm != null && primaryDistanceKm > 0.0) {
            ((primaryFare / primaryDistanceKm) * 100.0).roundToInt() / 100.0
        } else {
            null
        }
}

/**
 * Smart Regex parser for gig/ride-hailing screens (Rapido, Ola, Uber, Porter, Swiggy, Zomato).
 * Extracts currency fares (₹, Rs., INR) and distances (km, kms, m -> km) accurately while
 * avoiding false positives from durations like "5 min" or timestamps.
 */
object ScreenTextParser {

    // Prefix currency: ₹150, ₹ 1,250.50, Rs. 150, Rs 150.50, INR 150.50
    private val currencyPrefixRegex = Regex(
        pattern = """(?i)(?:₹|rs\.?|inr)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )

    // Suffix currency: 150 ₹, 150.50 Rs., 240 INR, 150/-
    private val currencySuffixRegex = Regex(
        pattern = """(?i)\b([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:₹|/-|(?:rs\.?|inr)(?![a-zA-Z]))"""
    )

    // Kilometers: 2.4 km, 3 kms, 3.5km, 4 kilometers
    private val kmRegex = Regex(
        pattern = """(?i)\b([0-9]+(?:\.[0-9]{1,2})?)\s*(?:kms?|kilomet(?:er|re)s?)\b"""
    )

    // Meters: 850 m, 850m, 850 meters, 850 mtrs — explicitly avoids "min", "ms", "mph", "am", "pm"
    private val meterRegex = Regex(
        pattern = """(?i)\b([0-9]{2,5}(?:\.[0-9]{1,2})?)\s*(?:meters?|metres?|mtrs?|m(?![a-zA-Z/]))"""
    )

    fun parse(textChunks: List<String>): ParsedScreenMetrics {
        if (textChunks.isEmpty()) {
            return ParsedScreenMetrics(null, null, emptyList(), emptyList())
        }
        val combined = textChunks.joinToString(separator = " | ")
        return parse(combined)
    }

    fun parse(text: String): ParsedScreenMetrics {
        if (text.isBlank()) {
            return ParsedScreenMetrics(null, null, emptyList(), emptyList())
        }

        val fares = mutableListOf<Double>()

        currencyPrefixRegex.findAll(text).forEach { match ->
            val raw = match.groupValues[1].replace(",", "")
            raw.toDoubleOrNull()?.takeIf { it in 1.0..100000.0 }?.let { fares.add(it) }
        }

        currencySuffixRegex.findAll(text).forEach { match ->
            val raw = match.groupValues[1].replace(",", "")
            raw.toDoubleOrNull()?.takeIf { it in 1.0..100000.0 }?.let { value ->
                if (!fares.contains(value)) {
                    fares.add(value)
                }
            }
        }

        // Preserve occurrence order across km and meter matches by indexing start positions
        val distanceMatches = mutableListOf<Pair<Int, Double>>()

        kmRegex.findAll(text).forEach { match ->
            val value = match.groupValues[1].toDoubleOrNull()
            if (value != null && value in 0.01..500.0) {
                distanceMatches.add(match.range.first to value)
            }
        }

        meterRegex.findAll(text).forEach { match ->
            val meters = match.groupValues[1].toDoubleOrNull()
            if (meters != null && meters in 10.0..50000.0) {
                val km = ((meters / 1000.0) * 1000.0).roundToInt() / 1000.0
                distanceMatches.add(match.range.first to km)
            }
        }

        val orderedDistances = distanceMatches
            .sortedBy { it.first }
            .map { it.second }

        return ParsedScreenMetrics(
            primaryFare = fares.firstOrNull(),
            primaryDistanceKm = orderedDistances.firstOrNull(),
            allFares = fares,
            allDistancesKm = orderedDistances
        )
    }

    fun findMatchingKeyword(candidateText: String, keywords: List<String>): String? {
        if (candidateText.isBlank() || keywords.isEmpty()) return null
        val normalized = candidateText.trim()
        return keywords.firstOrNull { keyword ->
            normalized.equals(keyword, ignoreCase = true) ||
                normalized.contains(keyword, ignoreCase = true)
        }
    }
}
