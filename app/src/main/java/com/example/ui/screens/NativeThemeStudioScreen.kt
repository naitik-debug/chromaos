package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChromaPalette
import com.example.model.IconMaskShape
import com.example.model.LauncherSettings
import com.example.model.OsPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeThemeStudioScreen(
    currentSettings: LauncherSettings,
    onSaveSettings: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var activeOsId by remember { mutableStateOf(currentSettings.activeOsId) }
    var activePaletteId by remember { mutableStateOf(currentSettings.activePaletteId) }
    var iconShape by remember { mutableStateOf(currentSettings.iconShape) }
    var iconSizeDp by remember { mutableFloatStateOf(currentSettings.iconSizeDp.toFloat()) }
    var showAppLabels by remember { mutableStateOf(currentSettings.showAppLabels) }
    var showSearchBar by remember { mutableStateOf(currentSettings.showSearchBar) }
    var showSecondaryWidget by remember { mutableStateOf(currentSettings.showSecondaryWidget) }
    var showHeroWidget by remember { mutableStateOf(currentSettings.showHeroWidget) }
    var showDock by remember { mutableStateOf(currentSettings.showDock) }
    var fontPackId by remember { mutableStateOf(currentSettings.fontPackId) }
    var wallpaperMode by remember { mutableStateOf(currentSettings.wallpaperMode) }

    val activePalette = ChromaPalette.getById(activePaletteId)

    fun copyHex(hex: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ChromaOS Hex", hex)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied HEX: $hex", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07110D))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("native_theme_studio_screen")
    ) {
        // Studio Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "ChromaOS Theme Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize Colors, Clocks & Shapes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
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
            actions = {
                Button(
                    onClick = {
                        val newSettings = currentSettings.copy(
                            activeOsId = activeOsId,
                            activePaletteId = activePaletteId,
                            iconShape = iconShape,
                            iconSizeDp = iconSizeDp.toInt(),
                            showAppLabels = showAppLabels,
                            showSearchBar = showSearchBar,
                            showSecondaryWidget = showSecondaryWidget,
                            showHeroWidget = showHeroWidget,
                            showDock = showDock,
                            fontPackId = fontPackId,
                            wallpaperMode = wallpaperMode
                        )
                        onSaveSettings(newSettings)
                        Toast.makeText(context, "Theme Applied Successfully!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activePalette.primaryAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("save_theme_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Apply", fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: 8 OS Presets
            item {
                Text(
                    text = "1. BASE OPERATING SYSTEM PRESETS",
                    style = MaterialTheme.typography.labelMedium,
                    color = activePalette.highlight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(OsPreset.PRESETS) { preset ->
                        val isSelected = preset.id == activeOsId
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable {
                                    activeOsId = preset.id
                                    iconShape = preset.defaultIconShape
                                    activePaletteId = preset.defaultPaletteId
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) activePalette.primaryAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f)
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, activePalette.primaryAccent)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.shortName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = activePalette.highlight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: 10 Pill-Stack Color Palettes
            item {
                Text(
                    text = "2. 10 PILL-STACK COLOR HARMONIES",
                    style = MaterialTheme.typography.labelMedium,
                    color = activePalette.highlight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Click a card to apply, or tap a color pill to copy its HEX",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChromaPalette.ALL_PALETTES.chunked(2).forEach { rowPalettes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPalettes.forEach { pal ->
                                val isSelected = pal.id == activePaletteId
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activePaletteId = pal.id },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.4f)
                                    ),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(2.dp, pal.primaryAccent)
                                    } else {
                                        androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = pal.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = pal.tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = pal.primaryAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Vertical pill stack
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            pal.hexColors.forEachIndexed { idx, hex ->
                                                val pillColor = pal.colors[idx]
                                                val isLight = idx >= 3
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(24.dp)
                                                        .clip(CircleShape)
                                                        .background(pillColor)
                                                        .clickable { copyHex(hex) }
                                                        .padding(horizontal = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = hex,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isLight) Color(0xFF1E293B) else Color.White
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy",
                                                        tint = if (isLight) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(10.dp)
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
            }

            // Section 3: Icon Mask Shape & Scale Slider
            item {
                Text(
                    text = "3. APP ICON GEOMETRY & SCALE",
                    style = MaterialTheme.typography.labelMedium,
                    color = activePalette.highlight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Icon Mask Shape",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconMaskShape.entries.forEach { shape ->
                                val isSelected = iconShape == shape
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) activePalette.primaryAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                                        .border(
                                            1.dp,
                                            if (isSelected) activePalette.primaryAccent else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { iconShape = shape }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shape.toComposeShape())
                                                .background(activePalette.highlight)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = shape.displayName.substringBefore(" "),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scale Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Icon Scale",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "${iconSizeDp.toInt()} dp",
                                style = MaterialTheme.typography.labelMedium,
                                color = activePalette.highlight,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = iconSizeDp,
                            onValueChange = { iconSizeDp = it },
                            valueRange = 44f..64f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = activePalette.highlight,
                                activeTrackColor = activePalette.primaryAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show App Labels",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Switch(
                                checked = showAppLabels,
                                onCheckedChange = { showAppLabels = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = activePalette.highlight,
                                    checkedTrackColor = activePalette.primaryAccent
                                )
                            )
                        }
                    }
                }
            }

            // Section 4: Typography Font Pack
            item {
                Text(
                    text = "4. STYLISH TYPOGRAPHY PAIRINGS",
                    style = MaterialTheme.typography.labelMedium,
                    color = activePalette.highlight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val fontPacks = listOf(
                    Triple("jakarta", "Plus Jakarta", "Modern Clean Flagship"),
                    Triple("syne", "Syne Geometric", "Avant-Garde Display"),
                    Triple("editorial", "Editorial Serif", "Luxury Cormorant Serif"),
                    Triple("space", "Space Grotesk", "Technical Display"),
                    Triple("ndot", "NDot Monospace", "Cyber Matrix Industrial")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fontPacks.forEach { (id, name, desc) ->
                        val isSelected = fontPackId == id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fontPackId = id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) activePalette.primaryAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, activePalette.primaryAccent)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    text = "09:41 Aa",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isSelected) activePalette.highlight else Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Universal "None (Hide)" Component Toggles
            item {
                Text(
                    text = "5. UNIVERSAL 'NONE' VISIBILITY CONTROLS",
                    style = MaterialTheme.typography.labelMedium,
                    color = activePalette.highlight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hide any screen layer you dislike for a zero-clutter look",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hero Clock Widget", color = Color.White)
                            Switch(checked = showHeroWidget, onCheckedChange = { showHeroWidget = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Secondary OS Feature Widget", color = Color.White)
                            Switch(checked = showSecondaryWidget, onCheckedChange = { showSecondaryWidget = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Home Search Bar Pill", color = Color.White)
                            Switch(checked = showSearchBar, onCheckedChange = { showSearchBar = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bottom Favorites Dock", color = Color.White)
                            Switch(checked = showDock, onCheckedChange = { showDock = it })
                        }
                    }
                }
            }

            // Section 6: Wallpaper Gradient Mode
            item {
                Text(
                    text = "6. WALLPAPER ATMOSPHERE STYLE",
                    style = MaterialTheme.typography.labelMedium,
                    color = activePalette.highlight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        "mesh" to "OS Mesh",
                        "botanical" to "Botanical",
                        "oled" to "OLED Black"
                    )
                    modes.forEach { (mode, label) ->
                        val isSelected = wallpaperMode == mode
                        Button(
                            onClick = { wallpaperMode = mode },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) activePalette.primaryAccent else Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            ),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
