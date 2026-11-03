package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WallpaperHelper
import com.example.data.WallpaperTarget
import com.example.model.ChromaPalette
import com.example.model.LauncherSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: LauncherSettings,
    palette: ChromaPalette,
    onOpenThemeStudio: () -> Unit,
    onOpenHtmlThemeStudio: () -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpaperHelper = remember { WallpaperHelper(context) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var selectedWallpaperUri by remember { mutableStateOf<Uri?>(null) }
    var isApplyingWallpaper by remember { mutableStateOf(false) }

    // System Image Document Picker (Zero broad storage permission required!)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedWallpaperUri = uri
            showWallpaperDialog = true
        }
    }

    // Role Manager for Home App selection on Android 10+
    val roleManagerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Returned from system default app settings
    }

    fun openDefaultHomeSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    roleManagerLauncher.launch(intent)
                    return
                }
            }
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Open Android Settings > Apps > Default apps > Home app to choose ChromaOS", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07110D))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("launcher_settings_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Launcher Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Default Home App Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDefaultHomeSettings() }
                        .testTag("set_default_home_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.secondaryBg.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.primaryAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Home App",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to set ChromaOS as your default launcher in Android Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Theme Studio Settings Item
            item {
                SettingsActionItem(
                    title = "ChromaOS Theme Studio (Native)",
                    subtitle = "OS presets, 10 pill-stack palettes, shapes & fonts",
                    icon = Icons.Default.Palette,
                    accentColor = palette.primaryAccent,
                    testTag = "open_native_studio_item",
                    onClick = onOpenThemeStudio
                )
            }

            // Wallpaper Picker Item
            item {
                SettingsActionItem(
                    title = "Set System Wallpaper",
                    subtitle = "Pick image using Android document picker (zero storage permission)",
                    icon = Icons.Default.Image,
                    accentColor = palette.highlight,
                    testTag = "pick_wallpaper_item",
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            // Embedded Original HTML Studio Item
            item {
                SettingsActionItem(
                    title = "Original HTML Theme Studio",
                    subtitle = "Preview the original interactive web lab bundled offline",
                    icon = Icons.Default.Web,
                    accentColor = palette.tertiaryDepth,
                    testTag = "open_html_studio_item",
                    onClick = onOpenHtmlThemeStudio
                )
            }

            // Reset Settings Item
            item {
                SettingsActionItem(
                    title = "Reset to Defaults",
                    subtitle = "Restore default Pixel 10 layout and Slate Ochre palette",
                    icon = Icons.Default.RestartAlt,
                    accentColor = Color(0xFFF43F5E),
                    testTag = "reset_defaults_item",
                    onClick = { showResetDialog = true }
                )
            }

            // Device & Safety Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "About ChromaOS Launcher",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Built natively using Kotlin and Jetpack Compose. Requests zero invasive permissions (no Accessibility, no notification listeners, no overlay). Wallpaper operations use Android WallpaperManager and system document picker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Reset Settings?") },
            text = { Text(text = "This will restore the launcher to its default Pixel 10 Clean preset, Slate Ochre colors, and standard icon shapes.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetDialog = false
                        Toast.makeText(context, "Settings restored to defaults", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    // Wallpaper Screen Target Selection Dialog
    if (showWallpaperDialog && selectedWallpaperUri != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isApplyingWallpaper) showWallpaperDialog = false
            },
            title = { Text(text = "Apply Wallpaper") },
            text = {
                Column {
                    Text(text = "Where would you like to set this wallpaper?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: Certain device brands may require manual home screen wallpaper settings in phone settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    if (isApplyingWallpaper) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = palette.primaryAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Applying wallpaper...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                if (!isApplyingWallpaper) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isApplyingWallpaper = true
                                scope.launch {
                                    val result = wallpaperHelper.applyWallpaper(selectedWallpaperUri!!, WallpaperTarget.HOME)
                                    isApplyingWallpaper = false
                                    showWallpaperDialog = false
                                    result.fold(
                                        onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                        onFailure = { Toast.makeText(context, it.message ?: "Failed", Toast.LENGTH_LONG).show() }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent)
                        ) {
                            Text("Home Screen")
                        }

                        Button(
                            onClick = {
                                isApplyingWallpaper = true
                                scope.launch {
                                    val result = wallpaperHelper.applyWallpaper(selectedWallpaperUri!!, WallpaperTarget.BOTH)
                                    isApplyingWallpaper = false
                                    showWallpaperDialog = false
                                    result.fold(
                                        onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                        onFailure = { Toast.makeText(context, it.message ?: "Failed", Toast.LENGTH_LONG).show() }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.highlight)
                        ) {
                            Text("Both Screens", color = Color(0xFF1E293B))
                        }
                    }
                }
            },
            dismissButton = {
                if (!isApplyingWallpaper) {
                    TextButton(onClick = { showWallpaperDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
