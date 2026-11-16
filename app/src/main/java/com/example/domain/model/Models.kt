package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class VehicleCategory(
    val code: String,
    val titleEn: String,
    val titleSi: String,
    val seats: Int,
    val defaultBaseLkr: Double,
    val defaultPerKmLkr: Double,
    val defaultPerMinLkr: Double,
    val defaultWaitingPerMinLkr: Double,
    val defaultMinFareLkr: Double
) {
    TUK_TUK("TUK_TUK", "Tuk Tuk", "ත්‍රිරෝද රථය", 3, 120.0, 95.0, 5.0, 8.0, 150.0),
    NANO("NANO", "Nano", "නැනෝ කුඩා රථය", 3, 160.0, 115.0, 6.0, 10.0, 220.0),
    MINI("MINI", "Mini", "මිනි හැච්බැක්", 4, 200.0, 130.0, 7.0, 12.0, 280.0),
    SEDAN("SEDAN", "Sedan", "සෙඩාන් රථය", 4, 260.0, 155.0, 9.0, 15.0, 350.0),
    SUV("SUV", "SUV", "SUV රථය", 6, 380.0, 210.0, 12.0, 18.0, 550.0),
    VAN("VAN", "Van", "වෑන් රථය", 9, 450.0, 235.0, 14.0, 20.0, 650.0),
    PREMIUM("PREMIUM", "Premium", "ප්‍රිමියම් විධායක", 4, 550.0, 290.0, 18.0, 25.0, 850.0);

    companion object {
        fun fromCode(code: String?): VehicleCategory =
            entries.firstOrNull { it.code == code } ?: TUK_TUK
    }
}

enum class TripStatus(val code: String, val labelEn: String, val labelSi: String) {
    REQUESTED("REQUESTED", "Searching Driver", "රියදුරෙකු සොයමින්"),
    ACCEPTED("ACCEPTED", "Driver Assigned", "රියදුරු පැමිණෙමින්"),
    DRIVER_ARRIVED("DRIVER_ARRIVED", "Driver Arrived", "රියදුරු පැමිණ ඇත"),
    STARTED("STARTED", "On Trip (Meter Active)", "ගමන ක්‍රියාත්මකයි"),
    COMPLETED("COMPLETED", "Trip Completed", "ගමන අවසන්"),
    CANCELLED("CANCELLED", "Cancelled", "අවලංගු කරන ලදී");

    companion object {
        fun fromCode(code: String?): TripStatus =
            entries.firstOrNull { it.code == code } ?: REQUESTED
    }
}

enum class PaymentMethod(val code: String, val labelEn: String, val labelSi: String) {
    CASH("CASH", "Cash (LKR)", "මුදල් (LKR)"),
    ONLINE_CARD("ONLINE_CARD", "Card Gateway", "කාඩ්පත් ගෙවීම"),
    LANKA_QR("LANKA_QR", "LankaQR", "LankaQR ගෙවීම");

    companion object {
        fun fromCode(code: String?): PaymentMethod =
            entries.firstOrNull { it.code == code } ?: CASH
    }
}

enum class PaymentStatus(val code: String, val labelEn: String, val labelSi: String) {
    PENDING("PENDING", "Pending Payment", "ගෙවීම බලාපොරොත්තුවෙන්"),
    PAID_CASH("PAID_CASH", "Paid in Cash", "මුදලින් ගෙවා ඇත"),
    AWAITING_GATEWAY("AWAITING_GATEWAY", "Awaiting Gateway Credentials", "ගෙවීම් ද්වාරය වින්‍යාස කර නැත"),
    REFUND_REQUESTED("REFUND_REQUESTED", "Refund Requested", "මුදල් ආපසු ඉල්ලීම");

    companion object {
        fun fromCode(code: String?): PaymentStatus =
            entries.firstOrNull { it.code == code } ?: PENDING
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val preferredLanguage: String = "si",
    val activeMode: String = "passenger",
    val emergencyContact: String? = null,
    val isSuspended: Boolean = false,
    val ratingAvg: Double = 5.0,
    val totalTrips: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DriverProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val vehicleType: String = VehicleCategory.TUK_TUK.code,
    val vehicleModel: String = "",
    val plateNumber: String = "",
    val vehicleColor: String? = null,
    val verificationStatus: String = "PENDING",
    val isOnline: Boolean = false,
    val latitude: Double = 6.9271,
    val longitude: Double = 79.8612,
    val heading: Double = 0.0,
    val gpsAccuracyMeters: Double = 10.0,
    val ratingAvg: Double = 5.0,
    val completedTrips: Long = 0L,
    val totalEarningsLkr: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DriverDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nicNumber: String = "",
    val licenceNumber: String = "",
    val licenceImageUri: String? = null,
    val vehicleDocImageUri: String? = null,
    val insuranceExpiry: String? = null,
    val verificationStatus: String = "PENDING",
    val reviewNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FareSetting(
    @DocumentId val id: String = "",
    val vehicleType: String = VehicleCategory.TUK_TUK.code,
    val displayNameEn: String = "Tuk Tuk",
    val displayNameSi: String = "ත්‍රිරෝද රථය",
    val baseFareLkr: Double = 120.0,
    val perKmRateLkr: Double = 95.0,
    val perMinRateLkr: Double = 5.0,
    val waitingPerMinRateLkr: Double = 8.0,
    val minFareLkr: Double = 150.0,
    val seatCapacity: Long = 3L,
    val isActive: Boolean = true,
    val updatedBy: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        fun defaults(): List<FareSetting> = VehicleCategory.entries.map { cat ->
            FareSetting(
                id = cat.code,
                vehicleType = cat.code,
                displayNameEn = cat.titleEn,
                displayNameSi = cat.titleSi,
                baseFareLkr = cat.defaultBaseLkr,
                perKmRateLkr = cat.defaultPerKmLkr,
                perMinRateLkr = cat.defaultPerMinLkr,
                waitingPerMinRateLkr = cat.defaultWaitingPerMinLkr,
                minFareLkr = cat.defaultMinFareLkr,
                seatCapacity = cat.seats.toLong(),
                isActive = true
            )
        }
    }
}

@IgnoreExtraProperties
data class Trip(
    @DocumentId val id: String = "",
    val passengerId: String = "",
    val passengerName: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val vehicleType: String = VehicleCategory.TUK_TUK.code,
    val vehiclePlate: String? = null,
    val pickupAddress: String = "",
    val pickupLat: Double = 6.9271,
    val pickupLng: Double = 79.8612,
    val dropoffAddress: String = "",
    val dropoffLat: Double = 6.9271,
    val dropoffLng: Double = 79.8612,
    val estimatedDistanceKm: Double = 0.0,
    val estimatedDurationMin: Long = 0L,
    val estimatedFareLkr: Double = 0.0,
    val status: String = TripStatus.REQUESTED.code,
    val paymentMethod: String = PaymentMethod.CASH.code,
    val paymentStatus: String = PaymentStatus.PENDING.code,
    val meterDistanceKm: Double = 0.0,
    val meterDurationSec: Long = 0L,
    val meterWaitingSec: Long = 0L,
    val baseFareLkr: Double = 0.0,
    val distanceChargeLkr: Double = 0.0,
    val timeChargeLkr: Double = 0.0,
    val waitingChargeLkr: Double = 0.0,
    val additionalChargesLkr: Double = 0.0,
    val finalFareLkr: Double = 0.0,
    val driverLat: Double = 0.0,
    val driverLng: Double = 0.0,
    val driverHeading: Double = 0.0,
    val lastGpsTimestampMs: Long = 0L,
    val lastGpsAccuracyM: Double = 0.0,
    val promoCode: String? = null,
    val discountLkr: Double = 0.0,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val passengerRating: Long = 0L,
    val passengerReview: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GpsBreadcrumb(
    @DocumentId val id: String = "",
    val tripId: String = "",
    val driverId: String = "",
    val passengerId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    val speedMps: Double = 0.0,
    val cumulativeDistanceKm: Double = 0.0,
    val runningFareLkr: Double = 0.0,
    @ServerTimestamp val recordedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleEn: String = "",
    val titleSi: String = "",
    val bodyEn: String = "",
    val bodySi: String = "",
    val type: String = "SYSTEM",
    val tripId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Complaint(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tripId: String? = null,
    val category: String = "OTHER",
    val subject: String = "",
    val description: String = "",
    val status: String = "OPEN",
    val adminResponse: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Promotion(
    @DocumentId val id: String = "",
    val code: String = "",
    val titleEn: String = "",
    val titleSi: String = "",
    val discountPercent: Long = 10L,
    val maxDiscountLkr: Double = 500.0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String? = null,
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMps: Float,
    val bearing: Float,
    val timestampMs: Long
)
