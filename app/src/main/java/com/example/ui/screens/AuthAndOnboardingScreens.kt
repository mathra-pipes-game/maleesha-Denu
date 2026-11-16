package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.MathraStrings
import com.example.data.auth.AuthManager
import com.example.ui.components.GlassCard
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.MathraLogoEmblem
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1200L)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF210E3D), ObsidianVoid)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MathraLogoEmblem(sizeDp = 96)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "MATHRA TAXI",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "මත්‍රා ටැක්සි • SRI LANKA",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricMagenta
                )
                Spacer(modifier = Modifier.height(28.dp))
                CircularProgressIndicator(
                    color = CyberCyan,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "POWERED BY MATHRA DEVELOPMENT TEAM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun AuthAndOnboardingScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showOnboarding by remember { mutableStateOf(true) }
    var onboardingPage by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MathraLogoEmblem(sizeDp = 40)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = MathraStrings.appName.get(language),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sri Lanka • LKR",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricMagenta
                        )
                    }
                }
                LanguageTogglePill(language = language, onToggle = onLanguageChange)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (showOnboarding) {
                val titles = listOf(
                    if (language == AppLanguage.SINHALA) "ශ්‍රී ලංකාවේ නවීනතම ටැක්සි අත්දැකීම" else "Luxury Sri Lanka Mobility",
                    if (language == AppLanguage.SINHALA) "සජීවී GPS සහ ඩිජිටල් මීටරය" else "Real GPS & Transparent Digital Meter",
                    if (language == AppLanguage.SINHALA) "මගී, රියදුරු සහ පරිපාලක පද්ධති" else "Passenger, Driver & Admin Ecosystem"
                )
                val descriptions = listOf(
                    if (language == AppLanguage.SINHALA)
                        "ත්‍රිරෝද රථයේ සිට ප්‍රිමියම් රථ දක්වා කොළඹ, මහනුවර, ගාල්ල ඇතුළු දිවයින පුරා ආරක්ෂිත ගමන්."
                    else
                        "Book Tuk Tuks, Nanos, Minis, Sedans, SUVs, Vans, or Premium cabs across Colombo, Kandy, Galle & beyond.",
                    if (language == AppLanguage.SINHALA)
                        "LKR ගාස්තු පදනම, දුර, කාලය සහ පොරොත්තු කාලය සජීවීව පෙන්වන වංචා-විරෝධී GPS ටැක්සි මීටරය."
                    else
                        "Live GPS route tracking, jump-validated distance metering, and crystal-clear LKR digital receipts.",
                    if (language == AppLanguage.SINHALA)
                        "අනුමත රියදුරන්, 119/1990 හදිසි SOS ආරක්ෂාව සහ Google ගිණුම් ආරක්ෂාව."
                    else
                        "Verified drivers, 119/1990 SOS protection, and enterprise cloud sync."
                )

                Image(
                    painter = painterResource(
                        id = if (onboardingPage == 1) R.drawable.img_onboarding_gps else R.drawable.img_hero_colombo
                    ),
                    contentDescription = "Mathra Taxi Sri Lanka Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = titles[onboardingPage],
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = descriptions[onboardingPage],
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showOnboarding = false }) {
                            Text(
                                text = if (language == AppLanguage.SINHALA) "මඟහරින්න" else "Skip",
                                color = TextMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { idx ->
                                Box(
                                    modifier = Modifier
                                        .width(if (idx == onboardingPage) 22.dp else 8.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (idx == onboardingPage) NeonPurple else TextMuted.copy(alpha = 0.4f))
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (onboardingPage < 2) onboardingPage++ else showOnboarding = false
                            },
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = if (onboardingPage < 2) {
                                    if (language == AppLanguage.SINHALA) "ඊළඟ" else "Next"
                                } else {
                                    if (language == AppLanguage.SINHALA) "ආරම්භ කරන්න" else "Start"
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_colombo),
                    contentDescription = "Colombo Night Skyline Mathra Taxi",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = CyberCyan) {
                Text(
                    text = MathraStrings.greeting.get(language),
                    style = MaterialTheme.typography.headlineLarge,
                    color = ElectricMagenta
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MathraStrings.subtitle.get(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureBadge(
                        icon = Icons.Filled.Speed,
                        label = "LKR Meter",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureBadge(
                        icon = Icons.Filled.Security,
                        label = "Verified Auth",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                } else {
                    NeonPrimaryButton(
                        text = MathraStrings.signInGoogle.get(language),
                        onClick = {
                            isLoading = true
                            authError = null
                            AuthManager.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    authError = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "google_sign_in_button"
                    )
                }

                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = MathraStrings.poweredBy.get(language),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianVoid.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
