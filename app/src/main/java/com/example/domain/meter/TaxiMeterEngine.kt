package com.example.domain.meter

import com.example.domain.model.FareSetting
import com.example.domain.model.LocationFix
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

data class FareBreakdown(
    val baseFareLkr: Double,
    val distanceChargeLkr: Double,
    val timeChargeLkr: Double,
    val waitingChargeLkr: Double,
    val additionalChargesLkr: Double,
    val discountLkr: Double,
    val totalFareLkr: Double
)

data class MeterStepResult(
    val accepted: Boolean,
    val rejectionReason: String? = null,
    val deltaDistanceKm: Double = 0.0,
    val deltaDurationSec: Long = 0L,
    val deltaWaitingSec: Long = 0L,
    val breakdown: FareBreakdown? = null
)

object TaxiMeterEngine {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val ROAD_FACTOR = 1.24 // Realistic Sri Lankan urban road winding multiplier
    private const val MAX_ACCEPTED_ACCURACY_METERS = 65.0f
    private const val MAX_REALISTIC_SPEED_MPS = 45.0 // 162 km/h impossible jump guard
    private const val WAITING_SPEED_THRESHOLD_MPS = 1.2 // Below ~4.3 km/h counts as waiting/traffic

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    fun estimateRoadDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val straight = haversineKm(lat1, lng1, lat2, lng2)
        return roundToTwoDecimals(max(0.5, straight * ROAD_FACTOR))
    }

    fun estimateDurationMinutes(distanceKm: Double, vehicleCode: String): Long {
        val avgSpeedKmh = when (vehicleCode) {
            "TUK_TUK" -> 26.0
            "NANO", "MINI" -> 30.0
            "SEDAN", "PREMIUM" -> 34.0
            else -> 31.0
        }
        return max(3L, ((distanceKm / avgSpeedKmh) * 60.0).toLong())
    }

    fun calculateFare(
        fareSetting: FareSetting,
        distanceKm: Double,
        durationMinutes: Double,
        waitingMinutes: Double,
        additionalChargesLkr: Double = 0.0,
        discountLkr: Double = 0.0
    ): FareBreakdown {
        val base = fareSetting.baseFareLkr
        val distanceCharge = roundToTwoDecimals(max(0.0, distanceKm) * fareSetting.perKmRateLkr)
        val timeCharge = roundToTwoDecimals(max(0.0, durationMinutes) * fareSetting.perMinRateLkr)
        val waitingCharge = roundToTwoDecimals(max(0.0, waitingMinutes) * fareSetting.waitingPerMinRateLkr)
        val extras = roundToTwoDecimals(max(0.0, additionalChargesLkr))
        val subtotal = max(fareSetting.minFareLkr, base + distanceCharge + timeCharge + waitingCharge + extras)
        val discount = roundToTwoDecimals(minOf(max(0.0, discountLkr), subtotal * 0.9))
        val total = roundToTwoDecimals(max(0.0, subtotal - discount))
        return FareBreakdown(
            baseFareLkr = base,
            distanceChargeLkr = distanceCharge,
            timeChargeLkr = timeCharge,
            waitingChargeLkr = waitingCharge,
            additionalChargesLkr = extras,
            discountLkr = discount,
            totalFareLkr = total
        )
    }

    /**
     * Validates an incoming GPS fix against the previous recorded telemetry point,
     * rejecting inaccurate fixes, duplicate/out-of-order timestamps, and impossible GPS jumps.
     */
    fun evaluateMeterStep(
        fareSetting: FareSetting,
        previousFix: LocationFix?,
        newFix: LocationFix,
        currentDistanceKm: Double,
        currentDurationSec: Long,
        currentWaitingSec: Long,
        additionalChargesLkr: Double,
        discountLkr: Double
    ): MeterStepResult {
        if (newFix.accuracyMeters > MAX_ACCEPTED_ACCURACY_METERS) {
            return MeterStepResult(
                accepted = false,
                rejectionReason = "GPS accuracy too low (${newFix.accuracyMeters.toInt()}m)"
            )
        }

        if (previousFix == null) {
            val breakdown = calculateFare(
                fareSetting = fareSetting,
                distanceKm = currentDistanceKm,
                durationMinutes = currentDurationSec / 60.0,
                waitingMinutes = currentWaitingSec / 60.0,
                additionalChargesLkr = additionalChargesLkr,
                discountLkr = discountLkr
            )
            return MeterStepResult(accepted = true, breakdown = breakdown)
        }

        val deltaMs = newFix.timestampMs - previousFix.timestampMs
        if (deltaMs <= 500L) {
            return MeterStepResult(
                accepted = false,
                rejectionReason = "Duplicate or stale GPS timestamp"
            )
        }

        val deltaSec = (deltaMs / 1000L).coerceIn(1L, 300L)
        val stepDistanceKm = haversineKm(
            previousFix.latitude, previousFix.longitude,
            newFix.latitude, newFix.longitude
        )
        val stepDistanceMeters = stepDistanceKm * 1000.0
        val impliedSpeedMps = stepDistanceMeters / deltaSec.toDouble()

        if (impliedSpeedMps > MAX_REALISTIC_SPEED_MPS) {
            return MeterStepResult(
                accepted = false,
                rejectionReason = "Rejected impossible GPS jump (${impliedSpeedMps.toInt()} m/s)"
            )
        }

        // Ignore micro GPS jitter under 4 meters when stationary
        val validDeltaKm = if (stepDistanceMeters < 4.0) 0.0 else stepDistanceKm
        val isWaiting = newFix.speedMps < WAITING_SPEED_THRESHOLD_MPS && validDeltaKm < 0.006
        val deltaWaitSec = if (isWaiting) deltaSec else 0L

        val newDist = roundToThreeDecimals(currentDistanceKm + validDeltaKm)
        val newDur = currentDurationSec + deltaSec
        val newWait = currentWaitingSec + deltaWaitSec

        val breakdown = calculateFare(
            fareSetting = fareSetting,
            distanceKm = newDist,
            durationMinutes = newDur / 60.0,
            waitingMinutes = newWait / 60.0,
            additionalChargesLkr = additionalChargesLkr,
            discountLkr = discountLkr
        )

        return MeterStepResult(
            accepted = true,
            deltaDistanceKm = validDeltaKm,
            deltaDurationSec = deltaSec,
            deltaWaitingSec = deltaWaitSec,
            breakdown = breakdown
        )
    }

    private fun roundToTwoDecimals(v: Double): Double = round(v * 100.0) / 100.0
    private fun roundToThreeDecimals(v: Double): Double = round(v * 1000.0) / 1000.0
}
