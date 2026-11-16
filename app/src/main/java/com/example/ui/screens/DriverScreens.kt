package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.MathraStrings
import com.example.domain.model.DriverProfile
import com.example.domain.model.SriLankaPlace
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.domain.model.VehicleCategory
import com.example.ui.components.DigitalTaxiMeterCard
import com.example.ui.components.GlassCard
import com.example.ui.components.MathraLiveMapView
import com.example.ui.components.MathraLogoEmblem
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWait
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.GoldBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun DriverDashboardScreen(
    viewModel: MainViewModel,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val driverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()
    val driverDoc by viewModel.driverDocument.collectAsStateWithLifecycle()
    val requestedTrips by viewModel.requestedTrips.collectAsStateWithLifecycle()
    val driverTrips by viewModel.driverTrips.collectAsStateWithLifecycle()
    val currentGps by viewModel.currentGpsFix.collectAsStateWithLifecycle()
    val gpsGranted by viewModel.gpsPermissionGranted.collectAsStateWithLifecycle()
    val meterWarning by viewModel.meterRejectionNote.collectAsStateWithLifecycle()
    val extraCharges by viewModel.additionalTollLkr.collectAsStateWithLifecycle()

    var rejectedTripIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTab by remember { mutableStateOf("dispatch") } // dispatch | earnings | kyc

    val activeTrip = driverTrips.firstOrNull {
        it.status in listOf(
            TripStatus.ACCEPTED.code,
            TripStatus.DRIVER_ARRIVED.code,
            TripStatus.STARTED.code
        )
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MathraLogoEmblem(sizeDp = 42)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MATHRA DRIVER",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = driverProfile?.plateNumber ?: "Registration Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricMagenta
                        )
                    }
                }
                Row {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                    }
                    IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("driver_profile_button")) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = CyberCyan)
                    }
                }
            }
        }

        // Driver Tab Selector
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == "dispatch",
                    onClick = { selectedTab = "dispatch" },
                    label = { Text(if (language == AppLanguage.SINHALA) "ගමන් ඇණවුම්" else "Dispatch & Meter") }
                )
                FilterChip(
                    selected = selectedTab == "earnings",
                    onClick = { selectedTab = "earnings" },
                    label = { Text(MathraStrings.earnings.get(language)) }
                )
                FilterChip(
                    selected = selectedTab == "kyc",
                    onClick = { selectedTab = "kyc" },
                    label = { Text("KYC & Vehicle") }
                )
            }
        }

        if (driverProfile == null || driverProfile?.verificationStatus != "APPROVED" || selectedTab == "kyc") {
            item {
                DriverRegistrationAndKycCard(
                    viewModel = viewModel,
                    driverProfile = driverProfile,
                    existingNic = driverDoc?.nicNumber ?: "",
                    existingLicence = driverDoc?.licenceNumber ?: "",
                    reviewNotes = driverDoc?.reviewNotes,
                    language = language
                )
            }
        } else if (selectedTab == "earnings") {
            item {
                DriverEarningsSection(driverTrips = driverTrips, driverProfile = driverProfile!!)
            }
        } else {
            // Online / Offline & GPS Card
            item {
                val drv = driverProfile!!
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = if (drv.isOnline) EmeraldOnline else NeonPurple) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (drv.isOnline) MathraStrings.goOnline.get(language) else MathraStrings.goOffline.get(language),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (drv.isOnline) "Live GPS sharing active • Approved" else "Switch ON to receive Sri Lanka rides",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (drv.isOnline) EmeraldOnline else TextMuted
                            )
                        }
                        Switch(
                            checked = drv.isOnline,
                            onCheckedChange = { checked ->
                                if (checked && !gpsGranted) {
                                    onRequestLocationPermission()
                                }
                                viewModel.toggleDriverOnline(checked)
                            },
                            modifier = Modifier.testTag("driver_online_switch")
                        )
                    }
                    if (!gpsGranted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = onRequestLocationPermission) {
                            Text("Grant Live GPS Permission", color = CyberCyan)
                        }
                    }
                }
            }

            if (activeTrip != null) {
                item {
                    ActiveDriverTripSection(
                        trip = activeTrip,
                        language = language,
                        extraChargesLkr = extraCharges,
                        meterWarning = meterWarning,
                        viewModel = viewModel
                    )
                }
            } else {
                val available = requestedTrips.filter { it.id !in rejectedTripIds }
                item {
                    Text(
                        text = if (language == AppLanguage.SINHALA)
                            "ආසන්න ගමන් ඉල්ලීම් (${available.size})"
                        else
                            "Nearby Ride Requests (${available.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                if (available.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (driverProfile?.isOnline == true)
                                    "Listening for nearby passenger ride requests in real time..."
                                else
                                    "Go Online to start receiving passenger requests.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(available, key = { it.id }) { req ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CyberCyan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(req.passengerName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                StatusPill(req.vehicleType, ElectricMagenta)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Pickup: ${req.pickupAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("Dropoff: ${req.dropoffAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                text = "${"%.1f".format(req.estimatedDistanceKm)} km • Est. LKR ${"%.2f".format(req.estimatedFareLkr)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { rejectedTripIds = rejectedTripIds + req.id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", color = CrimsonSos)
                                }
                                NeonPrimaryButton(
                                    text = "Accept",
                                    onClick = { viewModel.acceptRideRequest(req) },
                                    modifier = Modifier.weight(1f),
                                    accentColor = EmeraldOnline,
                                    testTag = "accept_trip_button_${req.id}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDriverTripSection(
    trip: Trip,
    language: AppLanguage,
    extraChargesLkr: Double,
    meterWarning: String?,
    viewModel: MainViewModel
) {
    val currentGps by viewModel.currentGpsFix.collectAsStateWithLifecycle()
    val status = TripStatus.fromCode(trip.status)
    val pickupPlace = SriLankaPlace("p", trip.pickupAddress, trip.pickupAddress, "", trip.pickupLat, trip.pickupLng)
    val dropoffPlace = SriLankaPlace("d", trip.dropoffAddress, trip.dropoffAddress, "", trip.dropoffLat, trip.dropoffLng)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MathraLiveMapView(
            currentGps = currentGps,
            pickup = pickupPlace,
            dropoff = dropoffPlace,
            onlineDrivers = emptyList(),
            assignedDriverLat = currentGps?.latitude ?: trip.driverLat,
            assignedDriverLng = currentGps?.longitude ?: trip.driverLng,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        )

        DigitalTaxiMeterCard(
            totalFareLkr = trip.finalFareLkr + extraChargesLkr,
            distanceKm = trip.meterDistanceKm,
            durationSec = trip.meterDurationSec,
            waitingSec = trip.meterWaitingSec,
            statusLabel = if (language == AppLanguage.SINHALA) status.labelSi else status.labelEn,
            baseFareLkr = trip.baseFareLkr,
            distanceChargeLkr = trip.distanceChargeLkr,
            timeChargeLkr = trip.timeChargeLkr,
            waitingChargeLkr = trip.waitingChargeLkr,
            additionalChargesLkr = trip.additionalChargesLkr + extraChargesLkr,
            gpsAccuracyM = currentGps?.accuracyMeters?.toDouble(),
            rejectionWarning = meterWarning
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Passenger: ${trip.passengerName}", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text("Pickup: ${trip.pickupAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Dropoff: ${trip.dropoffAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))

            when (status) {
                TripStatus.ACCEPTED -> {
                    NeonPrimaryButton(
                        text = MathraStrings.driverArrived.get(language),
                        onClick = { viewModel.markDriverArrived(trip.id) },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = CyberCyan,
                        testTag = "driver_arrived_button"
                    )
                }
                TripStatus.DRIVER_ARRIVED -> {
                    NeonPrimaryButton(
                        text = MathraStrings.startTrip.get(language),
                        onClick = { viewModel.startTripMeter(trip.id) },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = EmeraldOnline,
                        testTag = "start_trip_meter_button"
                    )
                }
                TripStatus.STARTED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.addApprovedExtraCharge(300.0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AddRoad, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+300 LKR Toll")
                        }
                        NeonPrimaryButton(
                            text = MathraStrings.completeTrip.get(language),
                            onClick = { viewModel.completeTripMeter(trip) },
                            modifier = Modifier.weight(1f),
                            accentColor = NeonPurple,
                            testTag = "complete_trip_button"
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun DriverRegistrationAndKycCard(
    viewModel: MainViewModel,
    driverProfile: DriverProfile?,
    existingNic: String,
    existingLicence: String,
    reviewNotes: String?,
    language: AppLanguage
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var name by remember(driverProfile, userProfile) {
        mutableStateOf(driverProfile?.displayName ?: userProfile?.displayName ?: viewModel.userDisplayName ?: "")
    }
    var vehicleType by remember(driverProfile) {
        mutableStateOf(VehicleCategory.fromCode(driverProfile?.vehicleType))
    }
    var model by remember(driverProfile) { mutableStateOf(driverProfile?.vehicleModel ?: "") }
    var plate by remember(driverProfile) { mutableStateOf(driverProfile?.plateNumber ?: "") }
    var color by remember(driverProfile) { mutableStateOf(driverProfile?.vehicleColor ?: "Black") }
    var nic by remember(existingNic) { mutableStateOf(existingNic) }
    var licence by remember(existingLicence) { mutableStateOf(existingLicence) }
    var insuranceExpiry by remember { mutableStateOf("2027-12-31") }
    var licenceUri by remember { mutableStateOf<Uri?>(null) }
    var vehicleDocUri by remember { mutableStateOf<Uri?>(null) }

    val licencePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> licenceUri = uri }

    val vehicleDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> vehicleDocUri = uri }

    GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = AmberWait) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Badge, contentDescription = null, tint = GoldBadge)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Driver KYC & Vehicle Verification", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            StatusPill(
                label = driverProfile?.verificationStatus ?: "UNREGISTERED",
                color = when (driverProfile?.verificationStatus) {
                    "APPROVED" -> EmeraldOnline
                    "REJECTED" -> CrimsonSos
                    else -> GoldBadge
                }
            )
        }

        if (driverProfile?.verificationStatus == "PENDING") {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = MathraStrings.verificationPending.get(language),
                style = MaterialTheme.typography.bodyMedium,
                color = GoldBadge
            )
        }
        if (!reviewNotes.isNullOrBlank()) {
            Text("Admin Notes: $reviewNotes", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Driver Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(VehicleCategory.entries) { cat ->
                FilterChip(
                    selected = vehicleType == cat,
                    onClick = { vehicleType = cat },
                    label = { Text(cat.titleEn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Vehicle Model (e.g., Bajaj RE / Toyota Prius)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Plate Number (e.g., WP CAB-4521)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Vehicle Color") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = nic, onValueChange = { nic = it }, label = { Text("Sri Lanka NIC Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = licence, onValueChange = { licence = it }, label = { Text("Driving Licence Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = insuranceExpiry, onValueChange = { insuranceExpiry = it }, label = { Text("Insurance Expiry (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { licencePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (licenceUri != null) "Licence Attached" else "Licence Photo")
            }
            OutlinedButton(
                onClick = { vehicleDocPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (vehicleDocUri != null) "Vehicle Doc Attached" else "Vehicle Doc")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        NeonPrimaryButton(
            text = "Submit for Admin Verification",
            onClick = {
                if (name.isNotBlank() && model.isNotBlank() && plate.isNotBlank() && nic.isNotBlank() && licence.isNotBlank()) {
                    viewModel.registerDriver(
                        displayName = name,
                        vehicleType = vehicleType,
                        vehicleModel = model,
                        plateNumber = plate,
                        vehicleColor = color,
                        nicNumber = nic,
                        licenceNumber = licence,
                        licenceImageUri = licenceUri?.toString(),
                        vehicleDocImageUri = vehicleDocUri?.toString(),
                        insuranceExpiry = insuranceExpiry
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "submit_driver_kyc_button"
        )
    }
}

@Composable
private fun DriverEarningsSection(
    driverTrips: List<Trip>,
    driverProfile: DriverProfile
) {
    val completed = driverTrips.filter { it.status == TripStatus.COMPLETED.code }
    val nowSec = System.currentTimeMillis() / 1000L
    val dayAgo = nowSec - TimeUnit.DAYS.toSeconds(1)
    val weekAgo = nowSec - TimeUnit.DAYS.toSeconds(7)
    val monthAgo = nowSec - TimeUnit.DAYS.toSeconds(30)

    val dailyLkr = completed.filter { (it.createdAt?.seconds ?: 0L) >= dayAgo }.sumOf { it.finalFareLkr }
    val weeklyLkr = completed.filter { (it.createdAt?.seconds ?: 0L) >= weekAgo }.sumOf { it.finalFareLkr }
    val monthlyLkr = completed.filter { (it.createdAt?.seconds ?: 0L) >= monthAgo }.sumOf { it.finalFareLkr }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = EmeraldOnline) {
            Text("Driver Earnings Overview (LKR)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EarningsStat("Daily", dailyLkr)
                EarningsStat("Weekly", weeklyLkr)
                EarningsStat("Monthly", monthlyLkr)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GoldBadge)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Completed Trips: ${completed.size} • Rating: ${"%.1f".format(driverProfile.ratingAvg)} ★",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        completed.forEach { trip ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("${trip.pickupAddress} → ${trip.dropoffAddress}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${"%.2f".format(trip.meterDistanceKm)} km", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        "LKR ${"%.2f".format(trip.finalFareLkr)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldOnline
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningsStat(label: String, amountLkr: Double) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = "LKR ${"%.0f".format(amountLkr)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = CyberCyan
        )
    }
}
