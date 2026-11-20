package com.example

import com.example.data.model.JobOutcome
import com.example.data.model.RuleConfig
import com.example.engine.RuleEvaluator
import com.example.engine.ScreenTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test
    fun parsesAllCurrencyFormatsCorrectly() {
        assertEquals(150.0, ScreenTextParser.parse("New Ride ₹150").primaryFare!!, 0.01)
        assertEquals(150.0, ScreenTextParser.parse("Estimated Rs. 150").primaryFare!!, 0.01)
        assertEquals(150.5, ScreenTextParser.parse("Payout INR 150.50").primaryFare!!, 0.01)
        assertEquals(150.0, ScreenTextParser.parse("Earn 150 ₹ now").primaryFare!!, 0.01)
        assertEquals(1250.75, ScreenTextParser.parse("Airport Trip ₹1,250.75").primaryFare!!, 0.01)
    }

    @Test
    fun parsesKmAndMetersWithoutConfusingMinutes() {
        val kmMetrics = ScreenTextParser.parse("Pickup 2.4 km • 8 min away")
        assertEquals(2.4, kmMetrics.primaryDistanceKm!!, 0.001)

        val kmsMetrics = ScreenTextParser.parse("Total trip 3 kms • 12 mins")
        assertEquals(3.0, kmsMetrics.primaryDistanceKm!!, 0.001)

        val meterMetrics = ScreenTextParser.parse("Pickup 850 m • 5 min • ₹165")
        assertEquals(0.85, meterMetrics.primaryDistanceKm!!, 0.001)
        assertEquals(165.0, meterMetrics.primaryFare!!, 0.01)

        val minutesOnly = ScreenTextParser.parse("Pickup in 5 min • 12 mins drop • ₹140")
        assertNull(minutesOnly.primaryDistanceKm)
        assertEquals(140.0, minutesOnly.primaryFare!!, 0.01)
    }

    @Test
    fun evaluatesProfitableOfferAsAccepted() {
        val config = RuleConfig(
            minFareEnabled = true,
            minFareAmount = 120.0,
            maxDistanceEnabled = true,
            maxDistanceKm = 3.5,
            minPerKmEnabled = true,
            minPerKmRate = 15.0,
            skipIfUndetected = true
        )
        val metrics = ScreenTextParser.parse("₹185 • 2.1 km • Accept")
        val result = RuleEvaluator.evaluate(metrics, matchedKeyword = "Accept", config = config)

        assertTrue(result.shouldClick)
        assertEquals(JobOutcome.ACCEPTED, result.outcome)
        assertTrue(result.summaryLine.contains("CLICKED"))
    }

    @Test
    fun skipsLowFareAndHighDistanceOffers() {
        val config = RuleConfig(
            minFareEnabled = true,
            minFareAmount = 120.0,
            maxDistanceEnabled = true,
            maxDistanceKm = 3.5,
            skipIfUndetected = true
        )
        val metrics = ScreenTextParser.parse("₹65 • 4.2 km • Accept")
        val result = RuleEvaluator.evaluate(metrics, matchedKeyword = "Accept", config = config)

        assertFalse(result.shouldClick)
        assertEquals(JobOutcome.SKIPPED, result.outcome)
        assertTrue(result.summaryLine.contains("Below min fare"))
    }

    @Test
    fun safeModeVsKeywordFallbackWhenMetricsMissing() {
        val safeConfig = RuleConfig(
            minFareEnabled = true,
            minFareAmount = 120.0,
            maxDistanceEnabled = true,
            maxDistanceKm = 3.5,
            skipIfUndetected = true
        )
        val missingMetrics = ScreenTextParser.parse("Surge Bonus Offer • Nearby • Accept")
        val safeResult = RuleEvaluator.evaluate(missingMetrics, matchedKeyword = "Accept", config = safeConfig)
        assertFalse(safeResult.shouldClick)
        assertEquals(JobOutcome.NOT_DETECTED, safeResult.outcome)

        val fallbackConfig = safeConfig.copy(skipIfUndetected = false)
        val fallbackResult = RuleEvaluator.evaluate(missingMetrics, matchedKeyword = "Accept", config = fallbackConfig)
        assertTrue(fallbackResult.shouldClick)
        assertEquals(JobOutcome.ACCEPTED, fallbackResult.outcome)
    }
}
