package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.domain.meter.FareBreakdown
import com.example.domain.model.AppNotification
import com.example.domain.model.AuditLog
import com.example.domain.model.Complaint
import com.example.domain.model.DriverDocument
import com.example.domain.model.DriverProfile
import com.example.domain.model.FareSetting
import com.example.domain.model.GpsBreadcrumb
import com.example.domain.model.LocationFix
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.Promotion
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MathraRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, expectedUid: String? = null): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    // --- User Profile ---
    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, docRef.path, uid)
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?,
        phone: String?,
        preferredLanguage: String = "si"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to displayName.ifBlank { "Mathra User" }.take(80),
                "email" to email,
                "phone" to phone?.ifBlank { null },
                "preferredLanguage" to preferredLanguage,
                "activeMode" to "passenger",
                "isSuspended" to false,
                "ratingAvg" to 5.0,
                "totalTrips" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    suspend fun updateUserProfile(
        displayName: String,
        phone: String?,
        emergencyContact: String?,
        preferredLanguage: String,
        activeMode: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeMode = if (activeMode == "driver") "driver" else "passenger"
        val updates = mutableMapOf<String, Any>(
            "displayName" to displayName.ifBlank { "Mathra User" }.take(80),
            "preferredLanguage" to if (preferredLanguage == "en") "en" else "si",
            "activeMode" to safeMode,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!phone.isNullOrBlank()) updates["phone"] = phone.take(24)
        if (!emergencyContact.isNullOrBlank()) updates["emergencyContact"] = emergencyContact.take(24)
        docRef.update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    // --- Driver Profile & KYC ---
    fun observeDriverProfile(uid: String): Flow<DriverProfile?> = flow {
        val docRef = db.collection("drivers").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, docRef.path, uid)
        )
    }

    fun observeDriverDocument(uid: String): Flow<DriverDocument?> = flow {
        val docRef = db.collection("driver_documents").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(DriverDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, docRef.path, uid)
        )
    }

    fun observeOnlineApprovedDrivers(): Flow<List<DriverProfile>> = flow {
        val query = db.collection("drivers")
            .whereEqualTo("verificationStatus", "APPROVED")
            .whereEqualTo("isOnline", true)
        emitAll(
            query.snapshots()
                .map { it.toObjects(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "drivers")
        )
    }

    suspend fun registerDriverApplication(
        displayName: String,
        vehicleType: String,
        vehicleModel: String,
        plateNumber: String,
        vehicleColor: String,
        nicNumber: String,
        licenceNumber: String,
        licenceImageUri: String?,
        vehicleDocImageUri: String?,
        insuranceExpiry: String?,
        lat: Double,
        lng: Double
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val driverRef = db.collection("drivers").document(uid)
        val docRef = db.collection("driver_documents").document(uid)
        val existingDriver = driverRef.get().await()

        if (!existingDriver.exists()) {
            val driverPayload = mapOf(
                "userId" to uid,
                "displayName" to displayName.take(80),
                "vehicleType" to vehicleType,
                "vehicleModel" to vehicleModel.take(80),
                "plateNumber" to plateNumber.uppercase().take(24),
                "vehicleColor" to vehicleColor.ifBlank { null },
                "verificationStatus" to "PENDING",
                "isOnline" to false,
                "latitude" to lat,
                "longitude" to lng,
                "heading" to 0.0,
                "gpsAccuracyMeters" to 10.0,
                "ratingAvg" to 5.0,
                "completedTrips" to 0L,
                "totalEarningsLkr" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            driverRef.set(driverPayload).await()
        } else {
            val updates = mapOf(
                "displayName" to displayName.take(80),
                "vehicleType" to vehicleType,
                "vehicleModel" to vehicleModel.take(80),
                "plateNumber" to plateNumber.uppercase().take(24),
                "vehicleColor" to vehicleColor.ifBlank { null },
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            driverRef.update(updates).await()
        }

        val existingDoc = docRef.get().await()
        if (!existingDoc.exists()) {
            val kycPayload = mapOf(
                "userId" to uid,
                "nicNumber" to nicNumber.take(24),
                "licenceNumber" to licenceNumber.take(32),
                "licenceImageUri" to licenceImageUri?.take(500),
                "vehicleDocImageUri" to vehicleDocImageUri?.take(500),
                "insuranceExpiry" to insuranceExpiry?.ifBlank { null }?.take(32),
                "verificationStatus" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(kycPayload).await()
        } else {
            val kycUpdate = mapOf(
                "nicNumber" to nicNumber.take(24),
                "licenceNumber" to licenceNumber.take(32),
                "licenceImageUri" to licenceImageUri?.take(500),
                "vehicleDocImageUri" to vehicleDocImageUri?.take(500),
                "insuranceExpiry" to insuranceExpiry?.ifBlank { null }?.take(32),
                "verificationStatus" to "PENDING",
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.update(kycUpdate).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "drivers/${auth.currentUser?.uid}")
    }

    suspend fun setDriverOnlineStatus(online: Boolean, fix: LocationFix?): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updates = mutableMapOf<String, Any>(
            "isOnline" to online,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (fix != null) {
            updates["latitude"] = fix.latitude
            updates["longitude"] = fix.longitude
            updates["heading"] = fix.bearing.toDouble()
            updates["gpsAccuracyMeters"] = fix.accuracyMeters.toDouble()
        }
        db.collection("drivers").document(uid).update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "drivers/${auth.currentUser?.uid}")
    }

    suspend fun updateDriverLocation(fix: LocationFix): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("drivers").document(uid).update(
            mapOf(
                "latitude" to fix.latitude,
                "longitude" to fix.longitude,
                "heading" to fix.bearing.toDouble(),
                "gpsAccuracyMeters" to fix.accuracyMeters.toDouble(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "drivers/${auth.currentUser?.uid}")
    }

    // --- Fare Settings ---
    fun observeActiveFareSettings(): Flow<List<FareSetting>> = flow {
        val query = db.collection("fare_settings").whereEqualTo("isActive", true)
        emitAll(
            query.snapshots()
                .map { snap ->
                    val list = snap.toObjects(FareSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    list.ifEmpty { FareSetting.defaults() }
                }
                .logFirestoreErrors(OperationType.LIST, "fare_settings")
        )
    }

    // --- Trips & Digital Meter ---
    fun observePassengerTrips(uid: String): Flow<List<Trip>> = flow {
        val query = db.collection("trips").whereEqualTo("passengerId", uid)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "trips", uid)
        )
    }

    fun observeRequestedTrips(): Flow<List<Trip>> = flow {
        val query = db.collection("trips").whereEqualTo("status", TripStatus.REQUESTED.code)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "trips")
        )
    }

    fun observeDriverTrips(uid: String): Flow<List<Trip>> = flow {
        val query = db.collection("trips").whereEqualTo("driverId", uid)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "trips", uid)
        )
    }

    fun observeTripBreadcrumbs(tripId: String, isDriver: Boolean, uid: String): Flow<List<GpsBreadcrumb>> = flow {
        val filterField = if (isDriver) "driverId" else "passengerId"
        val query = db.collection("trips").document(tripId)
            .collection("gps_history")
            .whereEqualTo(filterField, uid)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(GpsBreadcrumb::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.recordedAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "trips/$tripId/gps_history", uid)
        )
    }

    suspend fun createRideRequest(
        passengerName: String,
        vehicleType: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoffAddress: String,
        dropoffLat: Double,
        dropoffLng: Double,
        estimatedDistanceKm: Double,
        estimatedDurationMin: Long,
        estimatedFareLkr: Double,
        baseFareLkr: Double,
        paymentMethod: String,
        promoCode: String?,
        discountLkr: Double
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("trips").document()
        val payload = mapOf(
            "passengerId" to uid,
            "passengerName" to passengerName.ifBlank { "Passenger" }.take(80),
            "vehicleType" to vehicleType,
            "pickupAddress" to pickupAddress.take(200),
            "pickupLat" to pickupLat,
            "pickupLng" to pickupLng,
            "dropoffAddress" to dropoffAddress.take(200),
            "dropoffLat" to dropoffLat,
            "dropoffLng" to dropoffLng,
            "estimatedDistanceKm" to estimatedDistanceKm,
            "estimatedDurationMin" to estimatedDurationMin,
            "estimatedFareLkr" to estimatedFareLkr,
            "status" to TripStatus.REQUESTED.code,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to PaymentStatus.PENDING.code,
            "meterDistanceKm" to 0.0,
            "meterDurationSec" to 0L,
            "meterWaitingSec" to 0L,
            "baseFareLkr" to baseFareLkr,
            "distanceChargeLkr" to 0.0,
            "timeChargeLkr" to 0.0,
            "waitingChargeLkr" to 0.0,
            "additionalChargesLkr" to 0.0,
            "finalFareLkr" to baseFareLkr,
            "promoCode" to promoCode?.ifBlank { null },
            "discountLkr" to discountLkr,
            "passengerRating" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "trips")
    }

    /**
     * Atomic Firestore transaction ensuring a ride cannot be assigned to multiple drivers.
     */
    suspend fun acceptTripAtomically(
        tripId: String,
        driver: DriverProfile
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val tripRef = db.collection("trips").document(tripId)
        db.runTransaction { tx ->
            val snap = tx.get(tripRef)
            val currentStatus = snap.getString("status")
            val currentDriver = snap.getString("driverId")
            if (currentStatus != TripStatus.REQUESTED.code || currentDriver != null) {
                throw IllegalStateException("Ride already accepted or no longer available.")
            }
            tx.update(
                tripRef,
                mapOf(
                    "driverId" to uid,
                    "driverName" to driver.displayName.take(80),
                    "vehiclePlate" to driver.plateNumber.take(24),
                    "status" to TripStatus.ACCEPTED.code,
                    "driverLat" to driver.latitude,
                    "driverLng" to driver.longitude,
                    "driverHeading" to driver.heading,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId")
    }

    suspend fun updateTripStatusByDriver(
        tripId: String,
        newStatus: TripStatus,
        paymentStatus: PaymentStatus? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus.code,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (paymentStatus != null) {
            updates["paymentStatus"] = paymentStatus.code
        }
        db.collection("trips").document(tripId).update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId")
    }

    suspend fun pushMeterAndGpsUpdate(
        trip: Trip,
        fix: LocationFix,
        distanceKm: Double,
        durationSec: Long,
        waitingSec: Long,
        breakdown: FareBreakdown
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val tripRef = db.collection("trips").document(trip.id)
        val breadcrumbRef = tripRef.collection("gps_history").document()
        val batch = db.batch()

        batch.update(
            tripRef,
            mapOf(
                "meterDistanceKm" to distanceKm,
                "meterDurationSec" to durationSec,
                "meterWaitingSec" to waitingSec,
                "baseFareLkr" to breakdown.baseFareLkr,
                "distanceChargeLkr" to breakdown.distanceChargeLkr,
                "timeChargeLkr" to breakdown.timeChargeLkr,
                "waitingChargeLkr" to breakdown.waitingChargeLkr,
                "additionalChargesLkr" to breakdown.additionalChargesLkr,
                "finalFareLkr" to breakdown.totalFareLkr,
                "driverLat" to fix.latitude,
                "driverLng" to fix.longitude,
                "driverHeading" to fix.bearing.toDouble(),
                "lastGpsTimestampMs" to fix.timestampMs,
                "lastGpsAccuracyM" to fix.accuracyMeters.toDouble(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.set(
            breadcrumbRef,
            mapOf(
                "tripId" to trip.id,
                "driverId" to uid,
                "passengerId" to trip.passengerId,
                "latitude" to fix.latitude,
                "longitude" to fix.longitude,
                "accuracyMeters" to fix.accuracyMeters.toDouble(),
                "speedMps" to fix.speedMps.toDouble(),
                "cumulativeDistanceKm" to distanceKm,
                "runningFareLkr" to breakdown.totalFareLkr,
                "recordedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "trips/${trip.id}")
    }

    suspend fun cancelTrip(tripId: String, reason: String, cancelledBy: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("trips").document(tripId).update(
            mapOf(
                "status" to TripStatus.CANCELLED.code,
                "cancellationReason" to reason.take(200),
                "cancelledBy" to cancelledBy.take(32),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId")
    }

    suspend fun submitPaymentAndRating(
        tripId: String,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        rating: Long,
        review: String?
    ): Result<Unit> = runCatching {
        requireUserId()
        val updates = mutableMapOf<String, Any>(
            "paymentMethod" to paymentMethod.code,
            "paymentStatus" to paymentStatus.code,
            "passengerRating" to rating.coerceIn(0L, 5L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!review.isNullOrBlank()) {
            updates["passengerReview"] = review.take(400)
        }
        db.collection("trips").document(tripId).update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId")
    }

    // --- Notifications, Complaints, Promotions ---
    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        val query = db.collection("notifications").whereEqualTo("userId", uid)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "notifications", uid)
        )
    }

    suspend fun createNotification(
        targetUserId: String,
        titleEn: String,
        titleSi: String,
        bodyEn: String,
        bodySi: String,
        type: String,
        tripId: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("notifications").document()
        val payload = mapOf(
            "userId" to targetUserId,
            "titleEn" to titleEn.take(100),
            "titleSi" to titleSi.take(140),
            "bodyEn" to bodyEn.take(300),
            "bodySi" to bodySi.take(400),
            "type" to type,
            "tripId" to tripId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        docRef.set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "notifications")
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("notifications").document(notificationId).update("isRead", true).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
    }

    fun observeComplaints(uid: String): Flow<List<Complaint>> = flow {
        val query = db.collection("complaints").whereEqualTo("userId", uid)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "complaints", uid)
        )
    }

    suspend fun submitComplaint(
        tripId: String?,
        category: String,
        subject: String,
        description: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("complaints").document()
        val payload = mapOf(
            "userId" to uid,
            "tripId" to tripId?.ifBlank { null },
            "category" to category,
            "subject" to subject.take(120),
            "description" to description.take(1000),
            "status" to "OPEN",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        docRef.set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "complaints")
    }

    fun observeActivePromotions(): Flow<List<Promotion>> = flow {
        val query = db.collection("promotions").whereEqualTo("isActive", true)
        emitAll(
            query.snapshots()
                .map { it.toObjects(Promotion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "promotions")
        )
    }

    // --- Admin Operations ---
    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        emitAll(
            db.collection("users").snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "users")
        )
    }

    fun observeAllDriversAdmin(): Flow<List<DriverProfile>> = flow {
        emitAll(
            db.collection("drivers").snapshots()
                .map { it.toObjects(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "drivers")
        )
    }

    fun observeAllTripsAdmin(): Flow<List<Trip>> = flow {
        emitAll(
            db.collection("trips").snapshots()
                .map { snap ->
                    snap.toObjects(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "trips")
        )
    }

    fun observeAllComplaintsAdmin(): Flow<List<Complaint>> = flow {
        emitAll(
            db.collection("complaints").snapshots()
                .map { snap ->
                    snap.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "complaints")
        )
    }

    fun observeAuditLogsAdmin(): Flow<List<AuditLog>> = flow {
        val query = db.collection("audit_logs").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
        emitAll(
            query.snapshots()
                .map { it.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "audit_logs")
        )
    }

    suspend fun adminVerifyDriver(
        driverId: String,
        newStatus: String,
        notes: String
    ): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("drivers").document(driverId),
            mapOf(
                "verificationStatus" to newStatus,
                "isOnline" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val docRef = db.collection("driver_documents").document(driverId)
        val docSnap = docRef.get().await()
        if (docSnap.exists()) {
            batch.update(
                docRef,
                mapOf(
                    "verificationStatus" to newStatus,
                    "reviewNotes" to notes.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        val auditRef = db.collection("audit_logs").document()
        batch.set(
            auditRef,
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to auth.currentUser?.email,
                "action" to "DRIVER_VERIFICATION_$newStatus",
                "targetType" to "driver",
                "targetId" to driverId,
                "details" to notes.ifBlank { "Verification updated to $newStatus" }.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "drivers/$driverId")
    }

    suspend fun adminSuspendUser(targetUid: String, suspended: Boolean): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("users").document(targetUid),
            mapOf("isSuspended" to suspended, "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.set(
            db.collection("audit_logs").document(),
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to auth.currentUser?.email,
                "action" to if (suspended) "USER_SUSPENDED" else "USER_RESTORED",
                "targetType" to "user",
                "targetId" to targetUid,
                "details" to "Account suspension set to $suspended",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
    }

    suspend fun adminSaveFareSetting(setting: FareSetting): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val batch = db.batch()
        batch.set(
            db.collection("fare_settings").document(setting.vehicleType),
            mapOf(
                "vehicleType" to setting.vehicleType,
                "displayNameEn" to setting.displayNameEn,
                "displayNameSi" to setting.displayNameSi,
                "baseFareLkr" to setting.baseFareLkr,
                "perKmRateLkr" to setting.perKmRateLkr,
                "perMinRateLkr" to setting.perMinRateLkr,
                "waitingPerMinRateLkr" to setting.waitingPerMinRateLkr,
                "minFareLkr" to setting.minFareLkr,
                "seatCapacity" to setting.seatCapacity,
                "isActive" to setting.isActive,
                "updatedBy" to adminUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(),
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to auth.currentUser?.email,
                "action" to "FARE_UPDATED",
                "targetType" to "fare_settings",
                "targetId" to setting.vehicleType,
                "details" to "Base ${setting.baseFareLkr} LKR, PerKm ${setting.perKmRateLkr} LKR",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "fare_settings/${setting.vehicleType}")
    }

    suspend fun adminRespondComplaint(complaintId: String, status: String, response: String): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("complaints").document(complaintId),
            mapOf(
                "status" to status,
                "adminResponse" to response.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(),
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to auth.currentUser?.email,
                "action" to "COMPLAINT_$status",
                "targetType" to "complaint",
                "targetId" to complaintId,
                "details" to response.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "complaints/$complaintId")
    }

    suspend fun adminCreatePromotion(code: String, titleEn: String, titleSi: String, percent: Long, maxLkr: Double): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val docRef = db.collection("promotions").document(code.uppercase().trim())
        docRef.set(
            mapOf(
                "code" to code.uppercase().trim().take(24),
                "titleEn" to titleEn.take(100),
                "titleSi" to titleSi.take(140),
                "discountPercent" to percent.coerceIn(1L, 90L),
                "maxDiscountLkr" to maxLkr,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        db.collection("audit_logs").document().set(
            mapOf(
                "actorUid" to adminUid,
                "actorEmail" to auth.currentUser?.email,
                "action" to "PROMO_CREATED",
                "targetType" to "promotion",
                "targetId" to docRef.id,
                "details" to "$percent% off up to LKR $maxLkr",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "promotions")
    }
}
