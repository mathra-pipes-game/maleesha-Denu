package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.MathraStrings
import com.example.data.auth.AuthManager
import com.example.domain.model.PaymentStatus
import com.example.domain.model.Trip
import com.example.ui.components.GlassCard
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.MathraLogoEmblem
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldOnline
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
fun RideHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val trips by viewModel.passengerTrips.collectAsStateWithLifecycle()
    var selectedReceiptTrip by remember { mutableStateOf<Trip?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        ScreenTopBar(title = MathraStrings.rideHistory.get(language), onBack = onBack)
        if (trips.isEmpty()) {
            EmptyStateBox("No rides yet. Book your first Mathra Taxi ride!")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedReceiptTrip = trip }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(trip.vehicleType, style = MaterialTheme.typography.titleMedium, color = ElectricMagenta)
                            StatusPill(
                                label = trip.status,
                                color = if (trip.status == "COMPLETED") EmeraldOnline else CyberCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${trip.pickupAddress} → ${trip.dropoffAddress}", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "LKR ${"%.2f".format(if (trip.finalFareLkr > 0) trip.finalFareLkr else trip.estimatedFareLkr)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = PaymentStatus.fromCode(trip.paymentStatus).labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    selectedReceiptTrip?.let { trip ->
        AlertDialog(
            onDismissRequest = { selectedReceiptTrip = null },
            containerColor = ObsidianElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(MathraStrings.digitalReceipt.get(language), color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trip: ${trip.id.take(12)}", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    Text("From: ${trip.pickupAddress}", color = TextSecondary)
                    Text("To: ${trip.dropoffAddress}", color = TextSecondary)
                    Text("Driver: ${trip.driverName ?: "N/A"} (${trip.vehiclePlate ?: "--"})", color = TextSecondary)
                    Text("Base: LKR ${"%.2f".format(trip.baseFareLkr)}", color = TextSecondary)
                    Text("Distance (${"%.2f".format(trip.meterDistanceKm)} km): LKR ${"%.2f".format(trip.distanceChargeLkr)}", color = TextSecondary)
                    Text("Time + Waiting: LKR ${"%.2f".format(trip.timeChargeLkr + trip.waitingChargeLkr)}", color = TextSecondary)
                    Text(
                        "Final Total: LKR ${"%.2f".format(if (trip.finalFareLkr > 0) trip.finalFareLkr else trip.estimatedFareLkr)}",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedReceiptTrip = null }) { Text("Done", color = CyberCyan) }
            }
        )
    }
}

@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        ScreenTopBar(title = MathraStrings.notifications.get(language), onBack = onBack)
        if (notifications.isEmpty()) {
            EmptyStateBox("No notifications yet.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowAccent = if (item.type == "SOS") CrimsonSos else NeonPurple,
                        onClick = { if (!item.isRead) viewModel.markNotificationRead(item.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = if (item.type == "SOS") CrimsonSos else CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.SINHALA) item.titleSi else item.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            if (!item.isRead) {
                                StatusPill("NEW", ElectricMagenta)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.SINHALA) item.bodySi else item.bodyEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupportAndSosScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf("FARE_DISPUTE") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        ScreenTopBar(title = MathraStrings.support.get(language), onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CrimsonSos) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sos, contentDescription = null, tint = CrimsonSos)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(MathraStrings.emergencySos.get(language), style = MaterialTheme.typography.titleLarge, color = CrimsonSos)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sri Lanka Emergency Hotlines: 119 (Police) • 1990 (Suwa Seriya Ambulance) • 110 (Fire & Rescue)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NeonPrimaryButton(
                        text = "SEND LIVE GPS SOS ALERT",
                        onClick = { viewModel.triggerSos(null, "User triggered Emergency SOS from Support Screen") },
                        accentColor = CrimsonSos,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sos_trigger_button"
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Submit Support Ticket / Complaint", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("FARE_DISPUTE", "DRIVER_BEHAVIOR", "LOST_ITEM", "OTHER").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.replace("_", " ")) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe your issue") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NeonPrimaryButton(
                        text = "Submit Ticket",
                        onClick = {
                            if (subject.isNotBlank() && description.isNotBlank()) {
                                viewModel.submitSupportComplaint(null, category, subject, description)
                                subject = ""
                                description = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "submit_complaint_button"
                    )
                }
            }

            items(complaints, key = { it.id }) { ticket ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusPill(ticket.status, if (ticket.status == "RESOLVED") EmeraldOnline else GoldBadge)
                    }
                    Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    if (!ticket.adminResponse.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Admin Reply: ${ticket.adminResponse}", style = MaterialTheme.typography.bodyMedium, color = CyberCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAndSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val language by viewModel.language.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()

    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: viewModel.userDisplayName ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "+94") }
    var emergencyContact by remember(profile) { mutableStateOf(profile?.emergencyContact ?: "+94") }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        ScreenTopBar(title = "${MathraStrings.profile.get(language)} & ${MathraStrings.settings.get(language)}", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CyberCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MathraLogoEmblem(sizeDp = 44)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(profile?.displayName ?: "Mathra User", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(viewModel.userEmail ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                        LanguageTogglePill(language = language, onToggle = { viewModel.setLanguage(it) })
                    }
                }
            }

            // Role Switcher across Passenger / Driver / Admin Console
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (language == AppLanguage.SINHALA) "පද්ධති ප්‍රකාරය තෝරන්න" else "Switch Connected System Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricMagenta
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = activeRole == "passenger",
                            onClick = { viewModel.switchRole("passenger"); onBack() },
                            label = { Text(MathraStrings.passengerMode.get(language)) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = activeRole == "driver",
                            onClick = { viewModel.switchRole("driver"); onBack() },
                            label = { Text(MathraStrings.driverMode.get(language)) },
                            leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        if (viewModel.isAdminUser) {
                            FilterChip(
                                selected = activeRole == "admin",
                                onClick = { viewModel.switchRole("admin"); onBack() },
                                label = { Text(MathraStrings.adminMode.get(language)) },
                                leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Account & Sri Lanka Contact", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Sri Lanka Mobile (+94 7X XXX XXXX)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency SOS Contact") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonPrimaryButton(
                        text = "Save Profile",
                        onClick = { viewModel.saveProfile(displayName, phone, emergencyContact) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_profile_button"
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.weight(1f).testTag("about_mathra_button")
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(MathraStrings.about.get(language))
                    }
                    OutlinedButton(
                        onClick = {
                            AuthManager.signOut(credentialManager, scope) {}
                        },
                        modifier = Modifier.weight(1f).testTag("sign_out_button")
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = CrimsonSos)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", color = CrimsonSos)
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = ObsidianElevated,
            title = { Text("MATHRA TAXI • v1.0.0", color = ElectricMagenta) },
            text = {
                Text(
                    "MATHRA TAXI is Sri Lanka's integrated Passenger, Driver, Digital Taxi Meter & Admin Mobility Platform.\n\nDeveloper: MATHRA DEVELOPMENT TEAM\nCurrency: LKR\nLanguages: සිංහල & English\n\nPOWERED BY MATHRA DEVELOPMENT TEAM",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close", color = CyberCyan) }
            }
        )
    }
}

@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("top_bar_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}

@Composable
fun EmptyStateBox(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MathraLogoEmblem(sizeDp = 56)
        Spacer(modifier = Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    }
}
