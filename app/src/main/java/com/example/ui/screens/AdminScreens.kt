package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.domain.model.FareSetting
import com.example.domain.model.TripStatus
import com.example.ui.components.GlassCard
import com.example.ui.components.MathraLiveMapView
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
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AdminDashboardScreen(
    mainViewModel: MainViewModel,
    adminViewModel: AdminViewModel,
    onOpenProfile: () -> Unit
) {
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val drivers by adminViewModel.allDrivers.collectAsStateWithLifecycle()
    val trips by adminViewModel.allTrips.collectAsStateWithLifecycle()
    val complaints by adminViewModel.allComplaints.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val fareSettings by mainViewModel.fareSettings.collectAsStateWithLifecycle()

    var section by remember { mutableStateOf("overview") }
    val sections = listOf(
        "overview" to "Overview",
        "drivers" to "Drivers (${drivers.count { it.verificationStatus == "PENDING" }})",
        "fares" to "Fares",
        "trips" to "Trips",
        "complaints" to "Tickets (${complaints.count { it.status == "OPEN" }})",
        "promos" to "Promos",
        "audit" to "Audit & Reports"
    )

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
                    MathraLogoEmblem(sizeDp = 40)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MATHRA ADMIN CONSOLE", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        }
                        Text("Role-Based Governance • LKR Analytics", style = MaterialTheme.typography.labelSmall, color = ElectricMagenta)
                    }
                }
                IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("admin_profile_button")) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = CyberCyan)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sections) { (key, label) ->
                    FilterChip(
                        selected = section == key,
                        onClick = { section = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        when (section) {
            "overview" -> {
                val onlineDrivers = drivers.filter { it.isOnline && it.verificationStatus == "APPROVED" }
                val activeTrips = trips.count { it.status in listOf("REQUESTED", "ACCEPTED", "DRIVER_ARRIVED", "STARTED") }
                val completedTrips = trips.filter { it.status == TripStatus.COMPLETED.code }
                val revenueLkr = completedTrips.sumOf { it.finalFareLkr }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CyberCyan) {
                        Text("Platform Telemetry Overview", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KpiCell("Users", "${users.size}")
                            KpiCell("Drivers", "${drivers.size}")
                            KpiCell("Online", "${onlineDrivers.size}")
                            KpiCell("Pending KYC", "${drivers.count { it.verificationStatus == "PENDING" }}")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KpiCell("Active Trips", "$activeTrips")
                            KpiCell("Completed", "${completedTrips.size}")
                            KpiCell("Cancelled", "${trips.count { it.status == "CANCELLED" }}")
                            KpiCell("Revenue", "LKR ${"%.0f".format(revenueLkr)}")
                        }
                    }
                }

                item {
                    MathraLiveMapView(
                        currentGps = null,
                        pickup = null,
                        dropoff = null,
                        onlineDrivers = onlineDrivers,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )
                }

                item {
                    NeonPrimaryButton(
                        text = "Initialize / Sync Default LKR Tariffs to Firestore",
                        onClick = { adminViewModel.seedAllDefaultTariffs() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "seed_tariffs_button"
                    )
                }
            }

            "drivers" -> {
                items(drivers, key = { it.id }) { drv ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(drv.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("${drv.vehicleType} • ${drv.vehicleModel} (${drv.plateNumber})", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            StatusPill(
                                label = drv.verificationStatus,
                                color = when (drv.verificationStatus) {
                                    "APPROVED" -> EmeraldOnline
                                    "REJECTED" -> CrimsonSos
                                    else -> GoldBadge
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NeonPrimaryButton(
                                text = "Approve",
                                onClick = { adminViewModel.approveDriver(drv.userId) },
                                accentColor = EmeraldOnline,
                                modifier = Modifier.weight(1f),
                                testTag = "approve_driver_${drv.userId}"
                            )
                            OutlinedButton(
                                onClick = { adminViewModel.rejectDriver(drv.userId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject", color = CrimsonSos)
                            }
                        }
                    }
                }
                items(users, key = { "u_${it.id}" }) { usr ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(usr.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(usr.email ?: usr.userId, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            OutlinedButton(onClick = { adminViewModel.toggleUserSuspension(usr.userId, !usr.isSuspended) }) {
                                Text(if (usr.isSuspended) "Unsuspend" else "Suspend", color = if (usr.isSuspended) EmeraldOnline else CrimsonSos)
                            }
                        }
                    }
                }
            }

            "fares" -> {
                items(fareSettings, key = { it.vehicleType }) { fare ->
                    FareEditorCard(fare = fare, onSave = { adminViewModel.saveFareSetting(it) })
                }
            }

            "trips" -> {
                items(trips, key = { it.id }) { trip ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${trip.vehicleType} • ${trip.passengerName}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            StatusPill(trip.status, CyberCyan)
                        }
                        Text("${trip.pickupAddress} → ${trip.dropoffAddress}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            "Fare: LKR ${"%.2f".format(if (trip.finalFareLkr > 0) trip.finalFareLkr else trip.estimatedFareLkr)} • Driver: ${trip.driverName ?: "Unassigned"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricMagenta
                        )
                    }
                }
            }

            "complaints" -> {
                items(complaints, key = { it.id }) { ticket ->
                    ComplaintAdminCard(ticket = ticket, onResolve = { resp ->
                        adminViewModel.resolveComplaint(ticket.id, "RESOLVED", resp)
                    })
                }
            }

            "promos" -> {
                item {
                    PromoCreatorCard(onCreate = { code, titleEn, titleSi, pct, maxLkr ->
                        adminViewModel.createPromotion(code, titleEn, titleSi, pct, maxLkr)
                    })
                }
            }

            "audit" -> {
                item {
                    val completedCount = trips.count { it.status == "COMPLETED" }
                    val totalLkr = trips.filter { it.status == "COMPLETED" }.sumOf { it.finalFareLkr }
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = NeonPurple) {
                        Text("CSV / Management Summary Report", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "users=${users.size},drivers=${drivers.size},completed_trips=$completedCount,revenue_lkr=${"%.2f".format(totalLkr)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }
                }
                items(auditLogs, key = { it.id }) { log ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(log.action, style = MaterialTheme.typography.titleMedium, color = ElectricMagenta)
                        Text("${log.targetType}:${log.targetId} — ${log.details}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Actor: ${log.actorEmail ?: log.actorUid}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = CyberCyan)
    }
}

@Composable
private fun FareEditorCard(
    fare: FareSetting,
    onSave: (FareSetting) -> Unit
) {
    var base by remember(fare) { mutableStateOf(fare.baseFareLkr.toString()) }
    var perKm by remember(fare) { mutableStateOf(fare.perKmRateLkr.toString()) }
    var perMin by remember(fare) { mutableStateOf(fare.perMinRateLkr.toString()) }
    var waitMin by remember(fare) { mutableStateOf(fare.waitingPerMinRateLkr.toString()) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("${fare.displayNameEn} (${fare.displayNameSi})", style = MaterialTheme.typography.titleMedium, color = ElectricMagenta)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = base, onValueChange = { base = it }, label = { Text("Base LKR") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = perKm, onValueChange = { perKm = it }, label = { Text("Per Km") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = perMin, onValueChange = { perMin = it }, label = { Text("Per Min") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = waitMin, onValueChange = { waitMin = it }, label = { Text("Wait/Min") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                onSave(
                    fare.copy(
                        baseFareLkr = base.toDoubleOrNull() ?: fare.baseFareLkr,
                        perKmRateLkr = perKm.toDoubleOrNull() ?: fare.perKmRateLkr,
                        perMinRateLkr = perMin.toDoubleOrNull() ?: fare.perMinRateLkr,
                        waitingPerMinRateLkr = waitMin.toDoubleOrNull() ?: fare.waitingPerMinRateLkr
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save ${fare.displayNameEn} Rates", color = CyberCyan)
        }
    }
}

@Composable
private fun ComplaintAdminCard(
    ticket: com.example.domain.model.Complaint,
    onResolve: (String) -> Unit
) {
    var reply by remember { mutableStateOf(ticket.adminResponse ?: "") }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ticket.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            StatusPill(ticket.status, if (ticket.status == "RESOLVED") EmeraldOnline else CrimsonSos)
        }
        Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Admin Resolution Note") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { if (reply.isNotBlank()) onResolve(reply) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mark Resolved", color = EmeraldOnline)
        }
    }
}

@Composable
private fun PromoCreatorCard(
    onCreate: (String, String, String, Long, Double) -> Unit
) {
    var code by remember { mutableStateOf("MATHRA20") }
    var titleEn by remember { mutableStateOf("20% Colombo Night Ride") }
    var titleSi by remember { mutableStateOf("20% රාත්‍රී ගමන් වට්ටම") }
    var pct by remember { mutableStateOf("20") }
    var maxLkr by remember { mutableStateOf("400") }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Create Promotion Code", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title (English)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = titleSi, onValueChange = { titleSi = it }, label = { Text("Title (සිංහල)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = pct, onValueChange = { pct = it }, label = { Text("Discount %") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = maxLkr, onValueChange = { maxLkr = it }, label = { Text("Max LKR") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        NeonPrimaryButton(
            text = "Publish Promo",
            onClick = { onCreate(code, titleEn, titleSi, pct.toLongOrNull() ?: 10L, maxLkr.toDoubleOrNull() ?: 300.0) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "publish_promo_button"
        )
    }
}
