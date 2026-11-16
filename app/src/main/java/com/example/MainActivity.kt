package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.i18n.AppLanguage
import com.example.data.auth.AuthManager.authStateFlow
import com.example.data.location.LocationService
import com.example.data.repository.MathraRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.AuthAndOnboardingScreen
import com.example.ui.screens.DriverDashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PassengerHomeScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.RideHistoryScreen
import com.example.ui.screens.SupportAndSosScreen
import com.example.ui.theme.MathraTaxiTheme
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MathraTaxiTheme {
                MathraRootApp()
            }
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_SUPPORT = "support"
private const val ROUTE_PROFILE = "profile"

@Composable
fun MathraRootApp() {
    var showSplash by remember { mutableStateOf(true) }
    var preAuthLanguage by remember { mutableStateOf(AppLanguage.SINHALA) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    if (showSplash) {
        AnimatedSplashScreen(onFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        AuthAndOnboardingScreen(
            language = preAuthLanguage,
            onLanguageChange = { preAuthLanguage = it },
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedMathraApp(
                currentUserId = user.uid,
                userEmail = user.email,
                userDisplayName = user.displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedMathraApp(
    currentUserId: String,
    userEmail: String?,
    userDisplayName: String?
) {
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainViewModel(
                    repository = MathraRepository(db),
                    locationService = LocationService(app),
                    currentUserId = currentUserId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        mainViewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse || hasFine) {
            mainViewModel.onLocationPermissionResult(true)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMsg by mainViewModel.statusMessage.collectAsStateWithLifecycle()
    val activeRole by mainViewModel.activeRole.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(statusMsg) {
        val msg = statusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        mainViewModel.clearMessage()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        containerColor = ObsidianVoid,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(navController = navController, startDestination = ROUTE_HOME) {
                composable(ROUTE_HOME) {
                    when (activeRole) {
                        "driver" -> DriverDashboardScreen(
                            viewModel = mainViewModel,
                            onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                            onOpenProfile = { navController.navigate(ROUTE_PROFILE) },
                            onRequestLocationPermission = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                        "admin" -> {
                            if (mainViewModel.isAdminUser) {
                                val adminViewModel: AdminViewModel = viewModel(
                                    key = "admin_$currentUserId",
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val dbId = app.getString(R.string.firestore_database_id)
                                            AdminViewModel(MathraRepository(FirebaseFirestore.getInstance(dbId)))
                                        }
                                    }
                                )
                                val adminToast by adminViewModel.adminToast.collectAsStateWithLifecycle()
                                LaunchedEffect(adminToast) {
                                    adminToast?.let {
                                        snackbarHostState.showSnackbar(it)
                                        adminViewModel.clearToast()
                                    }
                                }
                                AdminDashboardScreen(
                                    mainViewModel = mainViewModel,
                                    adminViewModel = adminViewModel,
                                    onOpenProfile = { navController.navigate(ROUTE_PROFILE) }
                                )
                            } else {
                                PassengerHomeScreen(
                                    viewModel = mainViewModel,
                                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                    onOpenHistory = { navController.navigate(ROUTE_HISTORY) },
                                    onOpenProfile = { navController.navigate(ROUTE_PROFILE) },
                                    onOpenSupport = { navController.navigate(ROUTE_SUPPORT) },
                                    onRequestLocationPermission = {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        else -> PassengerHomeScreen(
                            viewModel = mainViewModel,
                            onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                            onOpenHistory = { navController.navigate(ROUTE_HISTORY) },
                            onOpenProfile = { navController.navigate(ROUTE_PROFILE) },
                            onOpenSupport = { navController.navigate(ROUTE_SUPPORT) },
                            onRequestLocationPermission = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }
                }
                composable(ROUTE_HISTORY) {
                    BackHandler { navController.popBackStack() }
                    RideHistoryScreen(viewModel = mainViewModel, onBack = { navController.popBackStack() })
                }
                composable(ROUTE_NOTIFICATIONS) {
                    BackHandler { navController.popBackStack() }
                    NotificationsScreen(viewModel = mainViewModel, onBack = { navController.popBackStack() })
                }
                composable(ROUTE_SUPPORT) {
                    BackHandler { navController.popBackStack() }
                    SupportAndSosScreen(viewModel = mainViewModel, onBack = { navController.popBackStack() })
                }
                composable(ROUTE_PROFILE) {
                    BackHandler { navController.popBackStack() }
                    ProfileAndSettingsScreen(viewModel = mainViewModel, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
