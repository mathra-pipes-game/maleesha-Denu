package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepPurpleContainer
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowAccent: Color = NeonPurple,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    ObsidianElevated.copy(alpha = 0.85f),
                    ObsidianCard.copy(alpha = 0.94f)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    glowAccent.copy(alpha = 0.55f),
                    GlassHighlight,
                    glowAccent.copy(alpha = 0.18f)
                )
            ),
            shape = shape
        )
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
        .padding(16.dp)

    Column(modifier = baseModifier, content = content)
}

@Composable
fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = NeonPurple,
    testTag: String = "neon_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = Color.White,
            disabledContainerColor = ObsidianElevated,
            disabledContentColor = TextMuted
        ),
        modifier = modifier
            .height(54.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MathraLogoEmblem(
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    val pulse by rememberInfiniteTransition(label = "logoPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DeepPurpleContainer, ObsidianVoid)
                )
            )
            .border(1.5.dp, NeonPurple.copy(alpha = pulse), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp * 0.65f).dp)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.12f, h * 0.82f)
                lineTo(w * 0.12f, h * 0.25f)
                lineTo(w * 0.50f, h * 0.58f)
                lineTo(w * 0.88f, h * 0.25f)
                lineTo(w * 0.88f, h * 0.82f)
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(ElectricMagenta, CyberCyan)),
                style = Stroke(width = w * 0.13f)
            )
            drawCircle(
                color = CyberCyan,
                radius = w * 0.08f,
                center = Offset(w * 0.50f, h * 0.16f)
            )
        }
    }
}

@Composable
fun LanguageTogglePill(
    language: AppLanguage,
    onToggle: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = ObsidianElevated,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("language_toggle_pill")
            .clickable {
                val next = if (language == AppLanguage.SINHALA) AppLanguage.ENGLISH else AppLanguage.SINHALA
                onToggle(next)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == AppLanguage.SINHALA) "සිං | EN" else "EN | සිං",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricMagenta
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun DigitalTaxiMeterCard(
    totalFareLkr: Double,
    distanceKm: Double,
    durationSec: Long,
    waitingSec: Long,
    statusLabel: String,
    baseFareLkr: Double,
    distanceChargeLkr: Double,
    timeChargeLkr: Double,
    waitingChargeLkr: Double,
    additionalChargesLkr: Double,
    gpsAccuracyM: Double?,
    rejectionWarning: String?,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "meterPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meterBorder"
    )
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_taxi_meter_card"),
        glowAccent = NeonPurple.copy(alpha = pulse)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalTaxi,
                    contentDescription = "Digital Meter",
                    tint = ElectricMagenta,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MATHRA DIGITAL METER",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricMagenta
                )
            }
            StatusPill(
                label = statusLabel,
                color = if (statusLabel.contains("ACTIVE", true) || statusLabel.contains("ක්‍රියා")) EmeraldOnline else CyberCyan
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianVoid)
                .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TOTAL FARE (LKR)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "LKR ${"%.2f".format(totalFareLkr)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MeterMetricCell("DISTANCE", "${"%.2f".format(distanceKm)} KM")
                    MeterMetricCell("DURATION", "${durationSec / 60}m ${durationSec % 60}s")
                    MeterMetricCell("WAITING", "${waitingSec / 60}m ${waitingSec % 60}s")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Base: LKR ${"%.0f".format(baseFareLkr)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Dist: LKR ${"%.0f".format(distanceChargeLkr)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Time: LKR ${"%.0f".format(timeChargeLkr)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Wait: LKR ${"%.0f".format(waitingChargeLkr)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Extras: LKR ${"%.0f".format(additionalChargesLkr)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            if (gpsAccuracyM != null) {
                Text("GPS ±${gpsAccuracyM.toInt()}m", style = MaterialTheme.typography.labelSmall, color = EmeraldOnline)
            }
        }
        if (!rejectionWarning.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Telemetry Guard: $rejectionWarning",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricMagenta
            )
        }
    }
}

@Composable
private fun MeterMetricCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}
