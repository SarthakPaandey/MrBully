package com.brutal.accountability.ui.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brutal.accountability.data.LocalRepository
import com.brutal.accountability.ui.screens.DashboardScreen
import com.brutal.accountability.ui.screens.OnboardingScreen
import com.brutal.accountability.ui.screens.RestrictedAppsScreen
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.DeepBlack
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val APPS = "apps"
    const val DASHBOARD = "dashboard"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(repository: LocalRepository) {
    val context = LocalContext.current
    val profile by repository.profileFlow.collectAsStateWithLifecycle(initialValue = null)
    val restrictedApps by repository.restrictedAppsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val strictMode by repository.strictModeFlow.collectAsStateWithLifecycle(initialValue = true)
    val apiKey by repository.apiKeyFlow.collectAsStateWithLifecycle(initialValue = "")
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val startDestination = if (profile == null) Routes.ONBOARDING else Routes.DASHBOARD

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BRUTAL",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = BrutalRed
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlack,
                    titleContentColor = BrutalRed
                ),
                actions = {
                    if (profile != null) {
                        IconButton(onClick = {
                            if (currentRoute != Routes.DASHBOARD) {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.DASHBOARD) { inclusive = true }
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = "Dashboard",
                                tint = if (currentRoute == Routes.DASHBOARD) BrutalRed else TextSecondary
                            )
                        }
                        IconButton(onClick = {
                            if (currentRoute != Routes.APPS) {
                                navController.navigate(Routes.APPS) {
                                    popUpTo(Routes.DASHBOARD)
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = "Apps",
                                tint = if (currentRoute == Routes.APPS) BrutalRed else TextSecondary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            enterTransition = {
                fadeIn(tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300)
                )
            },
            exitTransition = {
                fadeOut(tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300)
                )
            },
            popExitTransition = {
                fadeOut(tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300)
                )
            }
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onSave = { payload ->
                        scope.launch(Dispatchers.IO) {
                            val apiKey = payload["API_KEY"] ?: ""
                            if (apiKey.isNotBlank()) repository.setApiKey(apiKey)
                            
                            repository.upsertProfile(
                                nickname = payload["Nickname"] ?: "",
                                profession = payload["Profession"] ?: "",
                                goal = payload["Goal"] ?: "",
                                insecurity = payload["Insecurity"] ?: "",
                                fear = payload["Fear"] ?: "",
                                leverage = payload.filterKeys { k -> 
                                    k !in listOf("Nickname", "Profession", "Goal", "Insecurity", "Fear", "API_KEY") 
                                }
                            )
                        }
                        navController.navigate(Routes.APPS) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                    onFetchQuestion = { contextMap, tempApiKey ->
                        repository.generateDynamicQuestion(contextMap, tempApiKey)
                    }
                )
            }

            composable(Routes.APPS) {
                RestrictedAppsScreen(
                    initiallySelected = restrictedApps,
                    onSave = { apps ->
                        scope.launch(Dispatchers.IO) { repository.setRestrictedApps(apps) }
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.APPS) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.DASHBOARD) {
                val lifecycleOwner = LocalLifecycleOwner.current
                var isAccessibilityEnabled by remember { mutableStateOf(false) }

                fun refreshAccessibilityStatus() {
                    val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                    isAccessibilityEnabled = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC).any {
                        it.resolveInfo.serviceInfo.packageName == context.packageName
                    }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    refreshAccessibilityStatus()
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            refreshAccessibilityStatus()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                DashboardScreen(
                    strictMode = strictMode,
                    savedApiKey = apiKey,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onToggleStrictMode = { enabled ->
                        scope.launch(Dispatchers.IO) { repository.setStrictMode(enabled) }
                    },
                    onOpenAccessibilitySettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    onSavePhrase = { phrase ->
                        scope.launch(Dispatchers.IO) { repository.setPhrase(phrase) }
                    },
                    onSaveDaily = { morning, night ->
                        scope.launch(Dispatchers.IO) { repository.saveDailyCheckIn(morning, night) }
                    },
                    onRememberNote = { n ->
                        scope.launch(Dispatchers.IO) { repository.addSemanticNote(n) }
                    },
                    onSaveApiKey = { key ->
                        scope.launch(Dispatchers.IO) { repository.setApiKey(key) }
                    }
                )
            }
        }
    }
}
