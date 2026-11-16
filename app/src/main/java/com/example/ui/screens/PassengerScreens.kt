package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.MathraStrings
import com.example.domain.meter.TaxiMeterEngine
import com.example.domain.model.FareSetting
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.SriLankaPlace
import com.example.domain.model.SriLankaPlaces
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.domain.model.VehicleCategory
import com.example.ui.components.DigitalTaxiMeterCard
import com.example.ui.components.GlassCard
import com.example.ui.components.MathraLiveMapView
import com.example.ui.components.MathraLogoEmblem
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepPurpleContainer
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GoldBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun PassengerHomeScreen(
    viewModel: MainViewModel,
    onOpenNotifications: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSupport: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentGps by viewModel.currentGpsFix.collectAsStateWithLifecycle()
    val gpsGranted by viewModel.gpsPermissionGranted.collectAsStateWithLifecycle()
    val pickup by viewModel.pickupPlace.collectAsStateWithLifecycle()
    val dropoff by viewModel.dropoffPlace.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val selectedPayment by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val fareSettings by viewModel.fareSettings.collectAsStateWithLifecycle()
    val estimate by viewModel.currentFareEstimate.collectAsStateWithLifecycle()
    val onlineDrivers by viewModel.onlineDrivers.collectAsStateWithLifecycle()
    val trips by viewModel.passengerTrips.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val unreadCount = notifications.count { !it.isRead }
    val activeTrip = trips.firstOrNull {
        it.status in listOf(
            TripStatus.REQUESTED.code,
            TripStatus.ACCEPTED.code,
            TripStatus.DRIVER_ARRIVED.code,
            TripStatus.STARTED.code
        )
    }
    val completedUnratedTrip = trips.firstOrNull {
        it.status == TripStatus.COMPLETED.code && it.passengerRating == 0L
    }

    var pickingMode by remember { mutableStateOf<String?>(null) } // "pickup" or "dropoff"
    var promoInput by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (activeTrip != null) {
        ActivePassengerTripScreen(
            trip = activeTrip,
            language = language,
            viewModel = viewModel,
            onOpenSupport = onOpenSupport
        )
        return
    }

    if (completedUnratedTrip != null) {
        PaymentAndReceiptScreen(
            trip = completedUnratedTrip,
            language = language,
            viewModel = viewModel
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with Logo, Greeting, Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MathraLogoEmblem(sizeDp = 42)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${MathraStrings.greeting.get(language)} ${profile?.displayName?.substringBefore(" ") ?: ""}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "MATHRA TAXI • SRI LANKA",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricMagenta
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenHistory, modifier = Modifier.testTag("shortcut_history_button")) {
                        Icon(Icons.Filled.History, contentDescription = "Ride History", tint = TextSecondary)
                    }
                    IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("shortcut_notifications_button")) {
                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.triggerSos(null, "Passenger dashboard SOS alert") },
                        modifier = Modifier.testTag("shortcut_sos_button")
                    ) {
                        Icon(Icons.Filled.Sos, contentDescription = "Emergency SOS", tint = CrimsonSos)
                    }
                    IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("shortcut_profile_button")) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = CyberCyan)
                    }
                }
            }
        }

        // Live Google / Telemetry Map
        item {
            MathraLiveMapView(
                currentGps = currentGps,
                pickup = pickup,
                dropoff = dropoff,
                onlineDrivers = onlineDrivers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            )
        }

        // GPS Permission / Current Location Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CyberCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = MathraStrings.pickupPoint.get(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = if (language == AppLanguage.SINHALA) pickup.nameSi else pickup.nameEn,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row {
                        if (!gpsGranted) {
                            TextButton(onClick = onRequestLocationPermission) {
                                Text("Enable GPS", color = CyberCyan)
                            }
                        } else {
                            IconButton(onClick = { viewModel.useCurrentGpsAsPickup() }) {
                                Icon(Icons.Filled.GpsFixed, contentDescription = "Use Current GPS", tint = EmeraldOnline)
                            }
                        }
                        TextButton(
                            onClick = { pickingMode = "pickup" },
                            modifier = Modifier.testTag("change_pickup_button")
                        ) {
                            Text(if (language == AppLanguage.SINHALA) "වෙනස් කරන්න" else "Edit", color = ElectricMagenta)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = GlassBorder)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianVoid.copy(alpha = 0.7f))
                        .clickable { pickingMode = "dropoff" }
                        .padding(14.dp)
                        .testTag("destination_search_bar"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search Destination", tint = ElectricMagenta)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = dropoff?.let {
                            if (language == AppLanguage.SINHALA) it.nameSi else it.nameEn
                        } ?: MathraStrings.whereTo.get(language),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dropoff != null) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Popular Sri Lanka Destinations
        item {
            Text(
                text = MathraStrings.popularDestinations.get(language),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SriLankaPlaces.popular, key = { it.id }) { place ->
                    val isSelected = dropoff?.id == place.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDropoff(place) },
                        label = {
                            Text(
                                text = if (language == AppLanguage.SINHALA) place.nameSi else place.nameEn,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepPurpleContainer,
                            selectedLabelColor = ElectricMagenta,
                            containerColor = ObsidianElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        // 7 Vehicle Categories with Live LKR Fare Estimates
        item {
            Text(
                text = MathraStrings.selectVehicle.get(language),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(VehicleCategory.entries, key = { it.code }) { category ->
                    val tariff = fareSettings.firstOrNull { it.vehicleType == category.code }
                        ?: FareSetting.defaults().first { it.vehicleType == category.code }
                    val isSelected = selectedVehicle == category
                    val sampleFare = if (dropoff != null) {
                        val dKm = TaxiMeterEngine.estimateRoadDistanceKm(
                            pickup.latitude, pickup.longitude, dropoff!!.latitude, dropoff!!.longitude
                        )
                        val dur = TaxiMeterEngine.estimateDurationMinutes(dKm, category.code).toDouble()
                        TaxiMeterEngine.calculateFare(tariff, dKm, dur, 0.0).totalFareLkr
                    } else tariff.minFareLkr

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) DeepPurpleContainer else ObsidianElevated,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) NeonPurple else GlassBorder
                        ),
                        modifier = Modifier
                            .width(142.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("vehicle_card_${category.code}")
                            .clickable { viewModel.selectVehicle(category) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                imageVector = if (category == VehicleCategory.TUK_TUK) Icons.Filled.ElectricRickshaw else Icons.Filled.DirectionsCar,
                                contentDescription = category.titleEn,
                                tint = if (isSelected) CyberCyan else ElectricMagenta,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == AppLanguage.SINHALA) category.titleSi else category.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${category.seats} Seats",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "LKR ${"%.0f".format(sampleFare)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }

        // Transparent Fare Breakdown + Payment + Promo + Book Button
        if (dropoff != null && estimate != null) {
            item {
                val est = estimate!!
                val distKm = TaxiMeterEngine.estimateRoadDistanceKm(
                    pickup.latitude, pickup.longitude, dropoff!!.latitude, dropoff!!.longitude
                )
                val durMin = TaxiMeterEngine.estimateDurationMinutes(distKm, selectedVehicle.code)
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = NeonPurple) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = MathraStrings.fareEstimate.get(language),
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricMagenta
                        )
                        Text(
                            text = "${"%.1f".format(distKm)} km • ~$durMin min",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LKR ${"%.2f".format(est.totalFareLkr)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        color = CyberCyan
                    )
                    Text(
                        text = "Base LKR ${"%.0f".format(est.baseFareLkr)} + Dist LKR ${"%.0f".format(est.distanceChargeLkr)} + Time LKR ${"%.0f".format(est.timeChargeLkr)}" +
                            if (est.discountLkr > 0) " - Promo LKR ${"%.0f".format(est.discountLkr)}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = selectedPayment == method,
                                onClick = { viewModel.selectPaymentMethod(method) },
                                label = {
                                    Text(if (language == AppLanguage.SINHALA) method.labelSi else method.labelEn)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = promoInput,
                            onValueChange = { promoInput = it },
                            label = { Text("Promo Code") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.LocalOffer, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { viewModel.applyPromoCode(promoInput) }) {
                            Text("Apply")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    NeonPrimaryButton(
                        text = MathraStrings.bookRide.get(language),
                        onClick = { showConfirmDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "book_ride_button"
                    )
                }
            }
        }

        // Quick Support Footer
        item {
            OutlinedButton(
                onClick = onOpenSupport,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passenger_support_button")
            ) {
                Icon(Icons.Filled.SupportAgent, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(MathraStrings.support.get(language))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (pickingMode != null) {
        LocationSearchDialog(
            title = if (pickingMode == "pickup") MathraStrings.pickupPoint.get(language) else MathraStrings.whereTo.get(language),
            language = language,
            onSelect = { place ->
                if (pickingMode == "pickup") viewModel.setPickup(place) else viewModel.setDropoff(place)
                pickingMode = null
            },
            onDismiss = { pickingMode = null }
        )
    }

    if (showConfirmDialog && dropoff != null && estimate != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = ObsidianElevated,
            title = { Text(MathraStrings.confirmRide.get(language), color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Vehicle: ${selectedVehicle.titleEn}", color = TextSecondary)
                    Text("From: ${pickup.nameEn}", color = TextSecondary)
                    Text("To: ${dropoff!!.nameEn}", color = TextSecondary)
                    Text(
                        "Estimated Fare: LKR ${"%.2f".format(estimate!!.totalFareLkr)}",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                NeonPrimaryButton(
                    text = MathraStrings.confirmRide.get(language),
                    onClick = {
                        showConfirmDialog = false
                        viewModel.bookRide {}
                    },
                    testTag = "confirm_ride_dialog_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ActivePassengerTripScreen(
    trip: Trip,
    language: AppLanguage,
    viewModel: MainViewModel,
    onOpenSupport: () -> Unit
) {
    val status = TripStatus.fromCode(trip.status)
    val currentGps by viewModel.currentGpsFix.collectAsStateWithLifecycle()
    val pickupPlace = SriLankaPlace("p", trip.pickupAddress, trip.pickupAddress, "", trip.pickupLat, trip.pickupLng)
    val dropoffPlace = SriLankaPlace("d", trip.dropoffAddress, trip.dropoffAddress, "", trip.dropoffLat, trip.dropoffLng)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = if (language == AppLanguage.SINHALA) status.labelSi else status.labelEn,
                    color = if (status == TripStatus.STARTED) EmeraldOnline else CyberCyan
                )
                IconButton(onClick = { viewModel.triggerSos(trip.id, "Active trip SOS triggered by passenger") }) {
                    Icon(Icons.Filled.Sos, contentDescription = "SOS", tint = CrimsonSos)
                }
            }
        }

        item {
            MathraLiveMapView(
                currentGps = currentGps,
                pickup = pickupPlace,
                dropoff = dropoffPlace,
                onlineDrivers = emptyList(),
                assignedDriverLat = trip.driverLat,
                assignedDriverLng = trip.driverLng,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        if (status == TripStatus.STARTED) {
            item {
                DigitalTaxiMeterCard(
                    totalFareLkr = trip.finalFareLkr,
                    distanceKm = trip.meterDistanceKm,
                    durationSec = trip.meterDurationSec,
                    waitingSec = trip.meterWaitingSec,
                    statusLabel = "LIVE METER",
                    baseFareLkr = trip.baseFareLkr,
                    distanceChargeLkr = trip.distanceChargeLkr,
                    timeChargeLkr = trip.timeChargeLkr,
                    waitingChargeLkr = trip.waitingChargeLkr,
                    additionalChargesLkr = trip.additionalChargesLkr,
                    gpsAccuracyM = trip.lastGpsAccuracyM,
                    rejectionWarning = null
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (trip.driverName != null) "Driver: ${trip.driverName}" else "Finding nearest approved driver...",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                if (!trip.vehiclePlate.isNullOrBlank()) {
                    Text(
                        text = "${VehicleCategory.fromCode(trip.vehicleType).titleEn} • Plate: ${trip.vehiclePlate}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricMagenta
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pickup: ${trip.pickupAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Dropoff: ${trip.dropoffAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = "Est. Fare: LKR ${"%.2f".format(trip.estimatedFareLkr)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (status in listOf(TripStatus.REQUESTED, TripStatus.ACCEPTED, TripStatus.DRIVER_ARRIVED)) {
                        OutlinedButton(
                            onClick = { viewModel.cancelTrip(trip.id, "Cancelled by passenger", "PASSENGER") },
                            modifier = Modifier.weight(1f).testTag("cancel_trip_button")
                        ) {
                            Text(MathraStrings.cancelRide.get(language), color = CrimsonSos)
                        }
                    }
                    OutlinedButton(onClick = onOpenSupport, modifier = Modifier.weight(1f)) {
                        Text("Support")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentAndReceiptScreen(
    trip: Trip,
    language: AppLanguage,
    viewModel: MainViewModel
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.fromCode(trip.paymentMethod)) }
    var rating by remember { mutableLongStateOf(5L) }
    var review by remember { mutableStateOf("") }
    var requestRefund by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CyberCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(MathraStrings.digitalReceipt.get(language), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Trip ID: ${trip.id.take(10)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("${trip.pickupAddress} → ${trip.dropoffAddress}", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TOTAL: LKR ${"%.2f".format(if (trip.finalFareLkr > 0) trip.finalFareLkr else trip.estimatedFareLkr)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge,
                    color = CyberCyan
                )
                Text(
                    "Distance: ${"%.2f".format(trip.meterDistanceKm)} km | Duration: ${trip.meterDurationSec / 60}m | Waiting: ${trip.meterWaitingSec / 60}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(MathraStrings.paymentMethod.get(language), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(if (language == AppLanguage.SINHALA) method.labelSi else method.labelEn) }
                        )
                    }
                }
                if (selectedMethod != PaymentMethod.CASH) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Merchant gateway credentials required for live card/QR settlement. Recorded as AWAITING_GATEWAY (never faked).",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBadge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(MathraStrings.rateTrip.get(language), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldBadge)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$rating / 5", style = MaterialTheme.typography.titleMedium, color = GoldBadge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = rating.toFloat(),
                        onValueChange = { rating = it.toLong().coerceIn(1L, 5L) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Write driver review (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = requestRefund,
                        onClick = { requestRefund = !requestRefund },
                        label = { Text("Request Fare Refund Review") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (language == AppLanguage.SINHALA) "තහවුරු කර අවසන් කරන්න" else "Confirm Receipt & Submit Rating",
                    onClick = { viewModel.submitPaymentAndRating(trip, selectedMethod, requestRefund, rating, review) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_payment_rating_button"
                )
            }
        }
    }
}

@Composable
private fun LocationSearchDialog(
    title: String,
    language: AppLanguage,
    onSelect: (SriLankaPlace) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { SriLankaPlaces.search(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianElevated,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Colombo, Kandy, Galle, BIA...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results, key = { it.id }) { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianVoid.copy(alpha = 0.65f))
                                .clickable { onSelect(place) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ElectricMagenta, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.SINHALA) place.nameSi else place.nameEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${place.district} • ${"%.3f".format(place.latitude)}, ${"%.3f".format(place.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = CyberCyan) }
        }
    )
}
