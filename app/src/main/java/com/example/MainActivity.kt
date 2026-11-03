package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.AppRepository
import com.example.data.LauncherPreferences
import com.example.model.AppInfo
import com.example.model.LauncherSettings
import com.example.ui.screens.AppDrawerSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NativeThemeStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ChromaOSTheme
import kotlinx.coroutines.launch

enum class LauncherScreen {
    HOME,
    DRAWER,
    THEME_STUDIO,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var launcherPrefs: LauncherPreferences
    private lateinit var appRepo: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        launcherPrefs = LauncherPreferences(this)
        appRepo = AppRepository(this)

        setContent {
            var currentScreen by remember { mutableStateOf(LauncherScreen.HOME) }
            var settings by remember { mutableStateOf(launcherPrefs.loadSettings()) }
            var appsList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
            val scope = rememberCoroutineScope()

            fun refreshApps() {
                scope.launch {
                    appsList = appRepo.getInstalledApps(
                        favoritePackageNames = settings.favoritePackageNames,
                        hiddenPackageNames = settings.hiddenPackageNames
                    )
                }
            }

            LaunchedEffect(settings.favoritePackageNames, settings.hiddenPackageNames) {
                refreshApps()
            }

            // Listen for package install/uninstall events dynamically
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        refreshApps()
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addDataScheme("package")
                }
                registerReceiver(receiver, filter)

                onDispose {
                    unregisterReceiver(receiver)
                }
            }

            // Handle hardware back press cleanly
            BackHandler(enabled = currentScreen != LauncherScreen.HOME) {
                currentScreen = LauncherScreen.HOME
            }

            val activePalette = remember(settings) { settings.getResolvedPalette() }

            ChromaOSTheme(
                palette = activePalette,
                fontPackId = settings.fontPackId,
                darkTheme = true
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Always keep Home rendered underneath for seamless backdrop
                        HomeScreen(
                            apps = appsList,
                            settings = settings,
                            palette = activePalette,
                            onAppClick = { app ->
                                appRepo.launchApp(app.packageName)
                            },
                            onAppLongClick = { app ->
                                val isNowFav = !app.isFavorite
                                val newFavs = if (isNowFav) {
                                    settings.favoritePackageNames + app.packageName
                                } else {
                                    settings.favoritePackageNames - app.packageName
                                }
                                val updated = settings.copy(favoritePackageNames = newFavs)
                                settings = updated
                                launcherPrefs.saveSettings(updated)
                                val msg = if (isNowFav) "Added ${app.label} to Favorites" else "Removed ${app.label} from Favorites"
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            },
                            onOpenAppDrawer = {
                                currentScreen = LauncherScreen.DRAWER
                            },
                            onOpenThemeStudio = {
                                currentScreen = LauncherScreen.THEME_STUDIO
                            },
                            onOpenSettings = {
                                currentScreen = LauncherScreen.SETTINGS
                            }
                        )

                        // Top Layer Overlays (Drawer, Theme Studio, Settings)
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                when (targetState) {
                                    LauncherScreen.DRAWER -> {
                                        (slideInVertically { height -> height } + fadeIn())
                                            .togetherWith(slideOutVertically { height -> height } + fadeOut())
                                    }
                                    LauncherScreen.THEME_STUDIO, LauncherScreen.SETTINGS -> {
                                        fadeIn().togetherWith(fadeOut())
                                    }
                                    LauncherScreen.HOME -> {
                                        fadeIn().togetherWith(fadeOut())
                                    }
                                }
                            },
                            label = "LauncherScreensTransition"
                        ) { screen ->
                            when (screen) {
                                LauncherScreen.HOME -> {
                                    // Empty overlay, Home is already underneath
                                }
                                LauncherScreen.DRAWER -> {
                                    AppDrawerSheet(
                                        apps = appsList,
                                        settings = settings,
                                        palette = activePalette,
                                        onAppClick = { app ->
                                            val launched = appRepo.launchApp(app.packageName)
                                            if (launched) {
                                                currentScreen = LauncherScreen.HOME
                                            }
                                        },
                                        onAppLongClick = { app ->
                                            val isNowFav = !app.isFavorite
                                            val newFavs = if (isNowFav) {
                                                settings.favoritePackageNames + app.packageName
                                            } else {
                                                settings.favoritePackageNames - app.packageName
                                            }
                                            val updated = settings.copy(favoritePackageNames = newFavs)
                                            settings = updated
                                            launcherPrefs.saveSettings(updated)
                                            val msg = if (isNowFav) "Added ${app.label} to Favorites" else "Removed ${app.label} from Favorites"
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        onDismiss = {
                                            currentScreen = LauncherScreen.HOME
                                        }
                                    )
                                }
                                LauncherScreen.THEME_STUDIO -> {
                                    NativeThemeStudioScreen(
                                        currentSettings = settings,
                                        onSaveSettings = { newSettings ->
                                            settings = newSettings
                                            launcherPrefs.saveSettings(newSettings)
                                        },
                                        onBack = {
                                            currentScreen = LauncherScreen.HOME
                                        }
                                    )
                                }
                                LauncherScreen.SETTINGS -> {
                                    SettingsScreen(
                                        currentSettings = settings,
                                        palette = activePalette,
                                        onOpenThemeStudio = {
                                            currentScreen = LauncherScreen.THEME_STUDIO
                                        },
                                        onOpenHtmlThemeStudio = {
                                            startActivity(Intent(this@MainActivity, HtmlThemeStudioActivity::class.java))
                                        },
                                        onResetDefaults = {
                                            val defs = launcherPrefs.resetToDefaults()
                                            settings = defs
                                        },
                                        onBack = {
                                            currentScreen = LauncherScreen.HOME
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
