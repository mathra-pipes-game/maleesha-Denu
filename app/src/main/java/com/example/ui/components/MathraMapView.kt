package com.example.ui.components

import android.os.Bundle
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.BuildConfig
import com.example.domain.model.DriverProfile
import com.example.domain.model.LocationFix
import com.example.domain.model.SriLankaPlace
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextSecondary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.max

@Composable
fun MathraLiveMapView(
    currentGps: LocationFix?,
    pickup: SriLankaPlace?,
    dropoff: SriLankaPlace?,
    onlineDrivers: List<DriverProfile>,
    assignedDriverLat: Double? = null,
    assignedDriverLng: Double? = null,
    modifier: Modifier = Modifier
) {
    val mapsKey = BuildConfig.MAPS_API_KEY
    val hasValidMapsKey = mapsKey.isNotBlank() && mapsKey != "MY_GOOGLE_MAPS_API_KEY"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        if (hasValidMapsKey) {
            RealGoogleMapCanvas(
                currentGps = currentGps,
                pickup = pickup,
                dropoff = dropoff,
                onlineDrivers = onlineDrivers,
                assignedDriverLat = assignedDriverLat,
                assignedDriverLng = assignedDriverLng,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            TelemetryVectorMapCanvas(
                currentGps = currentGps,
                pickup = pickup,
                dropoff = dropoff,
                onlineDrivers = onlineDrivers,
                assignedDriverLat = assignedDriverLat,
                assignedDriverLng = assignedDriverLng,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating Telemetry Overlay Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = ObsidianVoid.copy(alpha = 0.82f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription = "GPS Signal",
                        tint = if (currentGps != null) EmeraldOnline else ElectricMagenta,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentGps != null) {
                            "GPS ${"%.4f".format(currentGps.latitude)}, ${"%.4f".format(currentGps.longitude)} (±${currentGps.accuracyMeters.toInt()}m)"
                        } else {
                            "Colombo Grid • GPS Standby"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = ObsidianVoid.copy(alpha = 0.82f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (hasValidMapsKey) "Google Maps Live" else "${onlineDrivers.size} Drivers Online",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RealGoogleMapCanvas(
    currentGps: LocationFix?,
    pickup: SriLankaPlace?,
    dropoff: SriLankaPlace?,
    onlineDrivers: List<DriverProfile>,
    assignedDriverLat: Double?,
    assignedDriverLng: Double?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            mv.getMapAsync { gMap ->
                gMap.setMapStyle(MapStyleOptions(DARK_OBSIDIAN_MAP_STYLE))
                gMap.clear()

                val points = mutableListOf<LatLng>()
                pickup?.let {
                    val p = LatLng(it.latitude, it.longitude)
                    points.add(p)
                    gMap.addMarker(
                        MarkerOptions()
                            .position(p)
                            .title("Pickup: ${it.nameEn}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    )
                }
                dropoff?.let {
                    val d = LatLng(it.latitude, it.longitude)
                    points.add(d)
                    gMap.addMarker(
                        MarkerOptions()
                            .position(d)
                            .title("Dropoff: ${it.nameEn}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    )
                }
                if (pickup != null && dropoff != null) {
                    gMap.addPolyline(
                        PolylineOptions()
                            .add(LatLng(pickup.latitude, pickup.longitude), LatLng(dropoff.latitude, dropoff.longitude))
                            .color(0xFFA855F7.toInt())
                            .width(10f)
                    )
                }
                onlineDrivers.forEach { drv ->
                    gMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(drv.latitude, drv.longitude))
                            .title("${drv.displayName} (${drv.plateNumber})")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                }
                if (assignedDriverLat != null && assignedDriverLng != null && assignedDriverLat != 0.0) {
                    val drvPos = LatLng(assignedDriverLat, assignedDriverLng)
                    points.add(drvPos)
                    gMap.addMarker(
                        MarkerOptions()
                            .position(drvPos)
                            .title("Assigned Driver")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                }

                when {
                    points.size >= 2 -> {
                        val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
                        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 90))
                    }
                    points.size == 1 -> {
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 13.5f))
                    }
                    currentGps != null -> {
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentGps.latitude, currentGps.longitude), 14f))
                    }
                    else -> {
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(6.9271, 79.8612), 12.5f))
                    }
                }
            }
        }
    )
}

@Composable
private fun TelemetryVectorMapCanvas(
    currentGps: LocationFix?,
    pickup: SriLankaPlace?,
    dropoff: SriLankaPlace?,
    onlineDrivers: List<DriverProfile>,
    assignedDriverLat: Double?,
    assignedDriverLng: Double?,
    modifier: Modifier = Modifier
) {
    val radarPhase by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarWave"
    )

    Canvas(modifier = modifier.background(Brush.radialGradient(listOf(ObsidianElevated, ObsidianVoid)))) {
        val w = size.width
        val h = size.height

        // Subtle dark cyber street grid
        val stepX = w / 8f
        val stepY = h / 6f
        for (i in 1..7) {
            drawLine(
                color = NeonPurple.copy(alpha = 0.09f),
                start = Offset(i * stepX, 0f),
                end = Offset(i * stepX, h),
                strokeWidth = 1f
            )
        }
        for (j in 1..5) {
            drawLine(
                color = NeonPurple.copy(alpha = 0.09f),
                start = Offset(0f, j * stepY),
                end = Offset(w, j * stepY),
                strokeWidth = 1f
            )
        }

        // Compute dynamic bounding projection around active points
        val allLats = mutableListOf<Double>()
        val allLngs = mutableListOf<Double>()
        currentGps?.let { allLats.add(it.latitude); allLngs.add(it.longitude) }
        pickup?.let { allLats.add(it.latitude); allLngs.add(it.longitude) }
        dropoff?.let { allLats.add(it.latitude); allLngs.add(it.longitude) }
        if (assignedDriverLat != null && assignedDriverLat != 0.0 && assignedDriverLng != null) {
            allLats.add(assignedDriverLat); allLngs.add(assignedDriverLng)
        }
        onlineDrivers.forEach { allLats.add(it.latitude); allLngs.add(it.longitude) }

        if (allLats.isEmpty()) {
            allLats.add(6.9271); allLngs.add(79.8612)
        }

        val minLat = allLats.min() - 0.015
        val maxLat = allLats.max() + 0.015
        val minLng = allLngs.min() - 0.015
        val maxLng = allLngs.max() + 0.015
        val latSpan = max(0.02, maxLat - minLat)
        val lngSpan = max(0.02, maxLng - minLng)

        fun toOffset(lat: Double, lng: Double): Offset {
            val nx = ((lng - minLng) / lngSpan).toFloat().coerceIn(0.08f, 0.92f)
            val ny = (1f - ((lat - minLat) / latSpan).toFloat()).coerceIn(0.18f, 0.88f)
            return Offset(nx * w, ny * h)
        }

        // Draw curved neon route between pickup & dropoff
        if (pickup != null && dropoff != null) {
            val p1 = toOffset(pickup.latitude, pickup.longitude)
            val p2 = toOffset(dropoff.latitude, dropoff.longitude)
            val mid = Offset((p1.x + p2.x) / 2f, ((p1.y + p2.y) / 2f) - h * 0.12f)
            val routePath = Path().apply {
                moveTo(p1.x, p1.y)
                quadraticTo(mid.x, mid.y, p2.x, p2.y)
            }
            drawPath(
                path = routePath,
                color = NeonPurple.copy(alpha = 0.25f),
                style = Stroke(width = 14f)
            )
            drawPath(
                path = routePath,
                brush = Brush.linearGradient(listOf(CyberCyan, ElectricMagenta)),
                style = Stroke(
                    width = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                )
            )
        }

        // Draw online drivers
        onlineDrivers.forEach { drv ->
            val pos = toOffset(drv.latitude, drv.longitude)
            drawCircle(EmeraldOnline.copy(alpha = 0.25f), radius = 18f, center = pos)
            drawCircle(EmeraldOnline, radius = 7f, center = pos)
        }

        // Draw current GPS pulse
        currentGps?.let { fix ->
            val pos = toOffset(fix.latitude, fix.longitude)
            drawCircle(
                color = CyberCyan.copy(alpha = 1f - radarPhase),
                radius = 12f + (36f * radarPhase),
                center = pos,
                style = Stroke(width = 2.5f)
            )
            drawCircle(CyberCyan, radius = 9f, center = pos)
        }

        // Draw pickup marker
        pickup?.let {
            val pos = toOffset(it.latitude, it.longitude)
            drawCircle(CyberCyan.copy(alpha = 0.22f), radius = 22f, center = pos)
            drawCircle(CyberCyan, radius = 10f, center = pos)
            drawCircle(ObsidianVoid, radius = 4f, center = pos)
        }

        // Draw dropoff marker
        dropoff?.let {
            val pos = toOffset(it.latitude, it.longitude)
            drawCircle(ElectricMagenta.copy(alpha = 0.24f), radius = 24f, center = pos)
            drawCircle(ElectricMagenta, radius = 11f, center = pos)
            drawCircle(ObsidianVoid, radius = 4f, center = pos)
        }

        // Draw assigned live driver marker
        if (assignedDriverLat != null && assignedDriverLat != 0.0 && assignedDriverLng != null) {
            val pos = toOffset(assignedDriverLat, assignedDriverLng)
            drawCircle(EmeraldOnline.copy(alpha = 0.30f), radius = 26f, center = pos)
            drawCircle(EmeraldOnline, radius = 11f, center = pos)
        }
    }
}

private const val DARK_OBSIDIAN_MAP_STYLE = """
[
  {"elementType":"geometry","stylers":[{"color":"#0d0b16"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#b5b0c9"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#06050b"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1e1933"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3b1d6a"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#050814"}]}
]
"""
