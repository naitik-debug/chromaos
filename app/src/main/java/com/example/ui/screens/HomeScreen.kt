package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.ChromaPalette
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconItem
import com.example.ui.components.FavoritesDock
import com.example.ui.components.HeroClockWidget
import com.example.ui.components.SearchBarPill
import com.example.ui.components.SecondaryWidgetCard

@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    palette: ChromaPalette,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenThemeStudio: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Dynamic wallpaper background brush
    val backgroundBrush = remember(settings.wallpaperMode, palette) {
        when (settings.wallpaperMode) {
            "oled" -> Brush.verticalGradient(listOf(Color(0xFF040608), Color(0xFF07090C)))
            "botanical" -> Brush.radialGradient(
                colors = listOf(
                    palette.primaryAccent.copy(alpha = 0.35f),
                    palette.secondaryBg.copy(alpha = 0.65f),
                    palette.deepBase
                )
            )
            else -> Brush.linearGradient(
                colors = listOf(
                    palette.deepBase,
                    palette.secondaryBg.copy(alpha = 0.85f),
                    palette.tertiaryDepth.copy(alpha = 0.4f)
                )
            )
        }
    }

    // Favorite apps for home grid and dock
    val favoriteApps by remember(apps, settings.favoritePackageNames) {
        derivedStateOf {
            val userFavorites = apps.filter { it.isFavorite }
            if (userFavorites.isNotEmpty()) {
                userFavorites
            } else {
                // Default favorites from installed apps (first 8 apps)
                apps.take(8)
            }
        }
    }

    val dockApps by remember(apps, favoriteApps) {
        derivedStateOf {
            // Prefer common utility apps if available or first 4 from favorites
            val phoneApp = apps.find { it.packageName.contains("dialer") || it.packageName.contains("phone") }
            val messageApp = apps.find { it.packageName.contains("messaging") || it.packageName.contains("mms") }
            val browserApp = apps.find { it.packageName.contains("chrome") || it.packageName.contains("browser") }
            val cameraApp = apps.find { it.packageName.contains("camera") }

            val defaults = listOfNotNull(phoneApp, messageApp, browserApp, cameraApp)
            if (defaults.size >= 4) {
                defaults.take(4)
            } else {
                favoriteApps.take(4).ifEmpty { apps.take(4) }
            }
        }
    }

    val homeGridApps by remember(favoriteApps, dockApps) {
        derivedStateOf {
            val dockPkgNames = dockApps.map { it.packageName }.toSet()
            val remaining = favoriteApps.filter { !dockPkgNames.contains(it.packageName) }
            if (remaining.isNotEmpty()) remaining else apps.filter { !dockPkgNames.contains(it.packageName) }.take(8)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) {
                        onOpenAppDrawer()
                    }
                }
            }
            .testTag("home_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Quick Header (Theme Studio & Settings Quick Access)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onOpenThemeStudio() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.primaryAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenThemeStudio,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .testTag("home_theme_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme Studio",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Upper Widgets Area
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (settings.showHeroWidget) {
                    HeroClockWidget(
                        presetId = settings.activeOsId,
                        palette = palette,
                        onClockClick = onOpenThemeStudio
                    )
                }

                if (settings.showSecondaryWidget) {
                    SecondaryWidgetCard(
                        palette = palette,
                        onMusicClick = onOpenThemeStudio,
                        onBatteryClick = onOpenSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Favorites App Grid (Scroll-free, fits home neatly)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (homeGridApps.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth(),
                        userScrollEnabled = false,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        items(homeGridApps) { app ->
                            AppIconItem(
                                app = app,
                                iconShape = settings.iconShape,
                                iconSizeDp = settings.iconSizeDp.dp,
                                showLabel = settings.showAppLabels,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }
                    }
                }
            }

            // Lower Section: Search Bar + Swipe Drawer Indicator + Bottom Favorites Dock
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (settings.showSearchBar) {
                    SearchBarPill(
                        palette = palette,
                        onSearchClick = onOpenAppDrawer
                    )
                }

                // Swipe up pill indicator
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onOpenAppDrawer() }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("open_app_drawer_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Swipe up",
                        tint = palette.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Swipe up for apps",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (settings.showDock && dockApps.isNotEmpty()) {
                    FavoritesDock(
                        dockApps = dockApps,
                        iconShape = settings.iconShape,
                        iconSizeDp = settings.iconSizeDp.dp,
                        palette = palette,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick
                    )
                }
            }
        }
    }
}
