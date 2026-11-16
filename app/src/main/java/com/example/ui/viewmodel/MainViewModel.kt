package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.i18n.AppLanguage
import com.example.data.location.LocationService
import com.example.data.repository.MathraRepository
import com.example.domain.meter.FareBreakdown
import com.example.domain.meter.TaxiMeterEngine
import com.example.domain.model.AppNotification
import com.example.domain.model.Complaint
import com.example.domain.model.DriverDocument
import com.example.domain.model.DriverProfile
import com.example.domain.model.FareSetting
import com.example.domain.model.LocationFix
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.Promotion
import com.example.domain.model.SriLankaPlace
import com.example.domain.model.SriLankaPlaces
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.VehicleCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MathraRepository,
    private val locationService: LocationService,
    val currentUserId: String,
    val userEmail: String?,
    val userDisplayName: String?
) : ViewModel() {

    private val _language = MutableStateFlow(AppLanguage.SINHALA)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _activeRole = MutableStateFlow("passenger")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Real GPS telemetry state
    private val _currentGpsFix = MutableStateFlow<LocationFix?>(null)
    val currentGpsFix: StateFlow<LocationFix?> = _currentGpsFix.asStateFlow()

    private val _gpsPermissionGranted = MutableStateFlow(false)
    val gpsPermissionGranted: StateFlow<Boolean> = _gpsPermissionGranted.asStateFlow()

    private val _meterRejectionNote = MutableStateFlow<String?>(null)
    val meterRejectionNote: StateFlow<String?> = _meterRejectionNote.asStateFlow()

    // Booking flow state
    private val _pickupPlace = MutableStateFlow(SriLankaPlaces.all[1]) // Galle Face Green default until GPS resolves
    val pickupPlace: StateFlow<SriLankaPlace> = _pickupPlace.asStateFlow()

    private val _dropoffPlace = MutableStateFlow<SriLankaPlace?>(null)
    val dropoffPlace: StateFlow<SriLankaPlace?> = _dropoffPlace.asStateFlow()

    private val _selectedVehicle = MutableStateFlow(VehicleCategory.TUK_TUK)
    val selectedVehicle: StateFlow<VehicleCategory> = _selectedVehicle.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

    private val _appliedPromo = MutableStateFlow<Promotion?>(null)
    val appliedPromo: StateFlow<Promotion?> = _appliedPromo.asStateFlow()

    private val _additionalTollLkr = MutableStateFlow(0.0)
    val additionalTollLkr: StateFlow<Double> = _additionalTollLkr.asStateFlow()

    private var gpsJob: Job? = null
    private var lastValidMeterFix: LocationFix? = null

    val isAdminUser: Boolean = userEmail.equals("hirunideshani07@gmail.com", ignoreCase = true)

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e -> handleStreamError("userProfile", e); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val driverProfile: StateFlow<DriverProfile?> = repository.observeDriverProfile(currentUserId)
        .catch { e -> handleStreamError("driverProfile", e); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val driverDocument: StateFlow<DriverDocument?> = repository.observeDriverDocument(currentUserId)
        .catch { e -> handleStreamError("driverDocument", e); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val onlineDrivers: StateFlow<List<DriverProfile>> = repository.observeOnlineApprovedDrivers()
        .catch { e -> handleStreamError("onlineDrivers", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val fareSettings: StateFlow<List<FareSetting>> = repository.observeActiveFareSettings()
        .catch { e -> handleStreamError("fareSettings", e); emit(FareSetting.defaults()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FareSetting.defaults())

    val passengerTrips: StateFlow<List<Trip>> = repository.observePassengerTrips(currentUserId)
        .catch { e -> handleStreamError("passengerTrips", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val requestedTrips: StateFlow<List<Trip>> = repository.observeRequestedTrips()
        .catch { e -> handleStreamError("requestedTrips", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val driverTrips: StateFlow<List<Trip>> = repository.observeDriverTrips(currentUserId)
        .catch { e -> handleStreamError("driverTrips", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { e -> handleStreamError("notifications", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val complaints: StateFlow<List<Complaint>> = repository.observeComplaints(currentUserId)
        .catch { e -> handleStreamError("complaints", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val promotions: StateFlow<List<Promotion>> = repository.observeActivePromotions()
        .catch { e -> handleStreamError("promotions", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentFareEstimate: StateFlow<FareBreakdown?> = combine(
        pickupPlace,
        dropoffPlace,
        selectedVehicle,
        fareSettings,
        appliedPromo
    ) { pickup, dropoff, vehicle, tariffs, promo ->
        if (dropoff == null) return@combine null
        val tariff = tariffs.firstOrNull { it.vehicleType == vehicle.code }
            ?: FareSetting.defaults().first { it.vehicleType == vehicle.code }
        val distKm = TaxiMeterEngine.estimateRoadDistanceKm(
            pickup.latitude, pickup.longitude,
            dropoff.latitude, dropoff.longitude
        )
        val durMin = TaxiMeterEngine.estimateDurationMinutes(distKm, vehicle.code).toDouble()
        val raw = TaxiMeterEngine.calculateFare(tariff, distKm, durMin, 0.0)
        val discount = if (promo != null) {
            minOf(promo.maxDiscountLkr, raw.totalFareLkr * (promo.discountPercent / 100.0))
        } else 0.0
        TaxiMeterEngine.calculateFare(tariff, distKm, durMin, 0.0, 0.0, discount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = userDisplayName ?: "Mathra Rider",
                email = userEmail,
                phone = null,
                preferredLanguage = "si"
            )
        }
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    _language.value = AppLanguage.fromCode(profile.preferredLanguage)
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _gpsPermissionGranted.value = granted
        if (granted) {
            startLiveGpsCollection()
        }
    }

    private fun startLiveGpsCollection() {
        if (gpsJob?.isActive == true) return
        gpsJob = viewModelScope.launch {
            locationService.getLastKnownFix()?.let { fix ->
                _currentGpsFix.value = fix
            }
            locationService.observeLiveGps(2500L)
                .catch { e -> Log.w(TAG, "GPS stream warning: ${e.message}") }
                .collect { fix ->
                    _currentGpsFix.value = fix
                    handleLiveGpsUpdate(fix)
                }
        }
    }

    private suspend fun handleLiveGpsUpdate(fix: LocationFix) {
        val driver = driverProfile.value
        if (driver != null && driver.isOnline && driver.verificationStatus == "APPROVED") {
            repository.updateDriverLocation(fix)
        }
        val activeDriverTrip = driverTrips.value.firstOrNull {
            it.status == TripStatus.ACCEPTED.code ||
                it.status == TripStatus.DRIVER_ARRIVED.code ||
                it.status == TripStatus.STARTED.code
        }
        if (activeDriverTrip != null && activeDriverTrip.status == TripStatus.STARTED.code) {
            val tariff = fareSettings.value.firstOrNull { it.vehicleType == activeDriverTrip.vehicleType }
                ?: FareSetting.defaults().first { it.vehicleType == activeDriverTrip.vehicleType }
            val step = TaxiMeterEngine.evaluateMeterStep(
                fareSetting = tariff,
                previousFix = lastValidMeterFix,
                newFix = fix,
                currentDistanceKm = activeDriverTrip.meterDistanceKm,
                currentDurationSec = activeDriverTrip.meterDurationSec,
                currentWaitingSec = activeDriverTrip.meterWaitingSec,
                additionalChargesLkr = activeDriverTrip.additionalChargesLkr + _additionalTollLkr.value,
                discountLkr = activeDriverTrip.discountLkr
            )
            if (step.accepted && step.breakdown != null) {
                lastValidMeterFix = fix
                _meterRejectionNote.value = null
                repository.pushMeterAndGpsUpdate(
                    trip = activeDriverTrip,
                    fix = fix,
                    distanceKm = activeDriverTrip.meterDistanceKm + step.deltaDistanceKm,
                    durationSec = activeDriverTrip.meterDurationSec + step.deltaDurationSec,
                    waitingSec = activeDriverTrip.meterWaitingSec + step.deltaWaitingSec,
                    breakdown = step.breakdown
                )
            } else if (!step.accepted) {
                _meterRejectionNote.value = step.rejectionReason
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            repository.updateUserProfile(
                displayName = profile.displayName,
                phone = profile.phone,
                emergencyContact = profile.emergencyContact,
                preferredLanguage = lang.code,
                activeMode = if (_activeRole.value == "driver") "driver" else "passenger"
            )
        }
    }

    fun switchRole(role: String) {
        _activeRole.value = role
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun setPickup(place: SriLankaPlace) {
        _pickupPlace.value = place
    }

    fun useCurrentGpsAsPickup() {
        val fix = _currentGpsFix.value
        if (fix == null) {
            _statusMessage.value = if (_language.value == AppLanguage.SINHALA) {
                "GPS ස්ථානය ලැබෙන තෙක් රැඳී සිටින්න."
            } else {
                "Waiting for device GPS lock."
            }
            return
        }
        _pickupPlace.value = SriLankaPlace(
            id = "gps_current",
            nameEn = "Current GPS (${"%.4f".format(fix.latitude)}, ${"%.4f".format(fix.longitude)})",
            nameSi = "වත්මන් GPS ස්ථානය (${"%.4f".format(fix.latitude)}, ${"%.4f".format(fix.longitude)})",
            district = "Sri Lanka",
            latitude = fix.latitude,
            longitude = fix.longitude
        )
    }

    fun setDropoff(place: SriLankaPlace?) {
        _dropoffPlace.value = place
    }

    fun selectVehicle(category: VehicleCategory) {
        _selectedVehicle.value = category
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun applyPromoCode(codeInput: String) {
        val clean = codeInput.trim().uppercase()
        val match = promotions.value.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.isActive }
        if (match != null) {
            _appliedPromo.value = match
            _statusMessage.value = "Promo ${match.code} applied (${match.discountPercent}% off)"
        } else {
            _appliedPromo.value = null
            _statusMessage.value = "Invalid or inactive promo code."
        }
    }

    fun bookRide(onSuccess: () -> Unit) {
        val pickup = _pickupPlace.value
        val dropoff = _dropoffPlace.value ?: run {
            _statusMessage.value = "Please select a destination first."
            return
        }
        val estimate = currentFareEstimate.value ?: return
        val vehicle = _selectedVehicle.value
        val profile = userProfile.value
        viewModelScope.launch {
            _isBusy.value = true
            val distKm = TaxiMeterEngine.estimateRoadDistanceKm(
                pickup.latitude, pickup.longitude, dropoff.latitude, dropoff.longitude
            )
            val durMin = TaxiMeterEngine.estimateDurationMinutes(distKm, vehicle.code)
            val result = repository.createRideRequest(
                passengerName = profile?.displayName ?: userDisplayName ?: "Passenger",
                vehicleType = vehicle.code,
                pickupAddress = if (_language.value == AppLanguage.SINHALA) pickup.nameSi else pickup.nameEn,
                pickupLat = pickup.latitude,
                pickupLng = pickup.longitude,
                dropoffAddress = if (_language.value == AppLanguage.SINHALA) dropoff.nameSi else dropoff.nameEn,
                dropoffLat = dropoff.latitude,
                dropoffLng = dropoff.longitude,
                estimatedDistanceKm = distKm,
                estimatedDurationMin = durMin,
                estimatedFareLkr = estimate.totalFareLkr,
                baseFareLkr = estimate.baseFareLkr,
                paymentMethod = _selectedPaymentMethod.value.code,
                promoCode = _appliedPromo.value?.code,
                discountLkr = estimate.discountLkr
            )
            _isBusy.value = false
            result.onSuccess { tripId ->
                repository.createNotification(
                    targetUserId = currentUserId,
                    titleEn = "Ride Requested",
                    titleSi = "ගමන් ඉල්ලීම යවන ලදී",
                    bodyEn = "Searching approved ${vehicle.titleEn} drivers near you.",
                    bodySi = "ඔබ අසල අනුමත ${vehicle.titleSi} රියදුරන් සොයමින් සිටී.",
                    type = "TRIP",
                    tripId = tripId
                )
                onSuccess()
            }.onFailure { e ->
                _statusMessage.value = e.localizedMessage ?: "Failed to create ride request"
            }
        }
    }

    fun cancelTrip(tripId: String, reason: String, byRole: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.cancelTrip(tripId, reason, byRole)
                .onSuccess { _statusMessage.value = "Trip cancelled." }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Cancel failed" }
            _isBusy.value = false
        }
    }

    fun submitPaymentAndRating(
        trip: Trip,
        method: PaymentMethod,
        requestRefund: Boolean,
        rating: Long,
        review: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val payStatus = when {
                requestRefund -> PaymentStatus.REFUND_REQUESTED
                method == PaymentMethod.CASH -> PaymentStatus.PAID_CASH
                else -> PaymentStatus.AWAITING_GATEWAY
            }
            repository.submitPaymentAndRating(trip.id, method, payStatus, rating, review)
                .onSuccess {
                    _statusMessage.value = if (method == PaymentMethod.CASH || requestRefund) {
                        "Receipt updated & rating saved."
                    } else {
                        "Online gateway awaiting merchant credentials (never faked)."
                    }
                }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Update failed" }
            _isBusy.value = false
        }
    }

    fun saveProfile(displayName: String, phone: String, emergencyContact: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateUserProfile(
                displayName = displayName,
                phone = phone,
                emergencyContact = emergencyContact,
                preferredLanguage = _language.value.code,
                activeMode = if (_activeRole.value == "driver") "driver" else "passenger"
            ).onSuccess {
                _statusMessage.value = "Profile updated."
            }.onFailure { e ->
                _statusMessage.value = e.localizedMessage ?: "Failed to update profile"
            }
            _isBusy.value = false
        }
    }

    fun triggerSos(activeTripId: String?, details: String) {
        viewModelScope.launch {
            val fix = _currentGpsFix.value
            val coords = if (fix != null) "${fix.latitude}, ${fix.longitude}" else "GPS unavailable"
            repository.submitComplaint(
                tripId = activeTripId,
                category = "SAFETY_SOS",
                subject = "EMERGENCY SOS ALERT",
                description = "$details | Location: $coords"
            )
            repository.createNotification(
                targetUserId = currentUserId,
                titleEn = "SOS Alert Logged",
                titleSi = "හදිසි SOS ඇඟවීම වාර්තා විය",
                bodyEn = "Mathra Safety Team notified at $coords. Call 119 / 1990 immediately if in danger.",
                bodySi = "ආරක්ෂක කණ්ඩායමට දැනුම් දෙන ලදී ($coords). හදිසි අවස්ථාවක 119 / 1990 අමතන්න.",
                type = "SOS",
                tripId = activeTripId
            )
            _statusMessage.value = "SOS Alert sent to Mathra Safety Team."
        }
    }

    fun submitSupportComplaint(tripId: String?, category: String, subject: String, description: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitComplaint(tripId, category, subject, description)
                .onSuccess { _statusMessage.value = "Support ticket submitted." }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to submit ticket" }
            _isBusy.value = false
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    // --- Driver Actions ---
    fun registerDriver(
        displayName: String,
        vehicleType: VehicleCategory,
        vehicleModel: String,
        plateNumber: String,
        vehicleColor: String,
        nicNumber: String,
        licenceNumber: String,
        licenceImageUri: String?,
        vehicleDocImageUri: String?,
        insuranceExpiry: String
    ) {
        val fix = _currentGpsFix.value
        viewModelScope.launch {
            _isBusy.value = true
            repository.registerDriverApplication(
                displayName = displayName,
                vehicleType = vehicleType.code,
                vehicleModel = vehicleModel,
                plateNumber = plateNumber,
                vehicleColor = vehicleColor,
                nicNumber = nicNumber,
                licenceNumber = licenceNumber,
                licenceImageUri = licenceImageUri,
                vehicleDocImageUri = vehicleDocImageUri,
                insuranceExpiry = insuranceExpiry,
                lat = fix?.latitude ?: 6.9271,
                lng = fix?.longitude ?: 79.8612
            ).onSuccess {
                _statusMessage.value = "Driver application submitted for Admin verification."
            }.onFailure { e ->
                _statusMessage.value = e.localizedMessage ?: "Driver registration failed"
            }
            _isBusy.value = false
        }
    }

    fun toggleDriverOnline(online: Boolean) {
        val driver = driverProfile.value ?: return
        if (online && driver.verificationStatus != "APPROVED") {
            _statusMessage.value = "Only Admin-approved drivers can go online."
            return
        }
        viewModelScope.launch {
            repository.setDriverOnlineStatus(online, _currentGpsFix.value)
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Status change failed" }
        }
    }

    fun acceptRideRequest(trip: Trip) {
        val driver = driverProfile.value ?: return
        if (driver.verificationStatus != "APPROVED") {
            _statusMessage.value = "Admin approval required before accepting trips."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.acceptTripAtomically(trip.id, driver)
                .onSuccess {
                    lastValidMeterFix = _currentGpsFix.value
                    _additionalTollLkr.value = 0.0
                    repository.createNotification(
                        targetUserId = currentUserId,
                        titleEn = "Trip Accepted",
                        titleSi = "ගමන භාරගන්නා ලදී",
                        bodyEn = "Proceed to pickup: ${trip.pickupAddress}",
                        bodySi = "මගී ස්ථානයට යන්න: ${trip.pickupAddress}",
                        type = "DRIVER",
                        tripId = trip.id
                    )
                }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not accept trip" }
            _isBusy.value = false
        }
    }

    fun markDriverArrived(tripId: String) {
        viewModelScope.launch {
            repository.updateTripStatusByDriver(tripId, TripStatus.DRIVER_ARRIVED)
                .onFailure { e -> _statusMessage.value = e.localizedMessage }
        }
    }

    fun startTripMeter(tripId: String) {
        lastValidMeterFix = _currentGpsFix.value
        viewModelScope.launch {
            repository.updateTripStatusByDriver(tripId, TripStatus.STARTED)
                .onFailure { e -> _statusMessage.value = e.localizedMessage }
        }
    }

    fun addApprovedExtraCharge(amountLkr: Double) {
        _additionalTollLkr.value = (_additionalTollLkr.value + amountLkr).coerceAtLeast(0.0)
    }

    fun completeTripMeter(trip: Trip) {
        viewModelScope.launch {
            _isBusy.value = true
            val payStatus = if (trip.paymentMethod == PaymentMethod.CASH.code) {
                PaymentStatus.PAID_CASH
            } else {
                PaymentStatus.AWAITING_GATEWAY
            }
            repository.updateTripStatusByDriver(trip.id, TripStatus.COMPLETED, payStatus)
                .onSuccess {
                    _additionalTollLkr.value = 0.0
                    lastValidMeterFix = null
                    _statusMessage.value = "Trip completed! Final fare: LKR ${"%.2f".format(trip.finalFareLkr)}"
                }
                .onFailure { e -> _statusMessage.value = e.localizedMessage }
            _isBusy.value = false
        }
    }

    private fun handleStreamError(source: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Stream [$source] warning: ${(firestoreError ?: error).message}")
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
