package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.meter.TaxiMeterEngine
import com.example.domain.model.FareSetting
import com.example.domain.model.LocationFix
import com.example.domain.model.PaymentMethod
import com.example.domain.model.VehicleCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_authenticatedPassenger_createsAndObservesProfile() = runBlocking {
        val uid = signInTestUser("passenger1@mathrataxi.lk")
        val repo = MathraRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(
                displayName = "Kasun Jayawardena",
                email = "passenger1@mathrataxi.lk",
                phone = "+94771234567",
                preferredLanguage = "si"
            )
        }
        assertTrue(result.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Kasun Jayawardena", profile?.displayName)
        assertEquals("si", profile?.preferredLanguage)
    }

    @Test
    fun createRideRequest_authenticatedPassenger_persistsAndStreamsTrip() = runBlocking {
        val uid = signInTestUser("passenger2@mathrataxi.lk")
        val repo = MathraRepository(firestore)

        val tripId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRideRequest(
                passengerName = "Amaya Silva",
                vehicleType = VehicleCategory.TUK_TUK.code,
                pickupAddress = "Colombo Fort",
                pickupLat = 6.9337,
                pickupLng = 79.8500,
                dropoffAddress = "Galle Face Green",
                dropoffLat = 6.9271,
                dropoffLng = 79.8442,
                estimatedDistanceKm = 1.4,
                estimatedDurationMin = 5L,
                estimatedFareLkr = 278.0,
                baseFareLkr = 120.0,
                paymentMethod = PaymentMethod.CASH.code,
                promoCode = null,
                discountLkr = 0.0
            ).getOrThrow()
        }
        assertTrue(tripId.isNotEmpty())

        val trips = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePassengerTrips(uid).first { list -> list.any { it.id == tripId } }
        }
        assertTrue(trips.any { it.id == tripId && it.status == "REQUESTED" })
    }

    @Test
    fun observePassengerTrips_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MathraRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePassengerTrips("some_other_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun registerDriver_cannotGoOnlineBeforeAdminApproval() = runBlocking {
        signInTestUser("driver1@mathrataxi.lk")
        val repo = MathraRepository(firestore)

        val regResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerDriverApplication(
                displayName = "Ruwan Kumara",
                vehicleType = VehicleCategory.SEDAN.code,
                vehicleModel = "Toyota Axio",
                plateNumber = "WP CAK-1234",
                vehicleColor = "Obsidian Black",
                nicNumber = "951234567V",
                licenceNumber = "B1234567",
                licenceImageUri = null,
                vehicleDocImageUri = null,
                insuranceExpiry = "2027-12-31",
                lat = 6.9271,
                lng = 79.8612
            )
        }
        assertTrue(regResult.isSuccess)

        val onlineAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.setDriverOnlineStatus(true, null)
        }
        assertTrue(onlineAttempt.isFailure)
        val firestoreEx = onlineAttempt.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun taxiMeterEngine_rejectsImpossibleGpsJump_andComputesValidFare() {
        val tukTariff = FareSetting.defaults().first { it.vehicleType == VehicleCategory.TUK_TUK.code }
        val fix1 = LocationFix(6.9271, 79.8612, 8f, 6f, 90f, 1_000_000L)
        // Impossible 50km jump in 2 seconds
        val jumpFix = LocationFix(7.4000, 79.8612, 8f, 10f, 90f, 1_002_000L)
        val jumpStep = TaxiMeterEngine.evaluateMeterStep(
            fareSetting = tukTariff,
            previousFix = fix1,
            newFix = jumpFix,
            currentDistanceKm = 0.0,
            currentDurationSec = 0L,
            currentWaitingSec = 0L,
            additionalChargesLkr = 0.0,
            discountLkr = 0.0
        )
        assertFalse(jumpStep.accepted)

        // Realistic ~1.1km movement over 120 seconds
        val validFix = LocationFix(6.9371, 79.8612, 7f, 9f, 0f, 1_120_000L)
        val validStep = TaxiMeterEngine.evaluateMeterStep(
            fareSetting = tukTariff,
            previousFix = fix1,
            newFix = validFix,
            currentDistanceKm = 0.0,
            currentDurationSec = 0L,
            currentWaitingSec = 0L,
            additionalChargesLkr = 0.0,
            discountLkr = 0.0
        )
        assertTrue(validStep.accepted)
        assertTrue((validStep.breakdown?.totalFareLkr ?: 0.0) > tukTariff.baseFareLkr)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
