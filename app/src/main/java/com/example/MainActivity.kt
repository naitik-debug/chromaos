package com.example

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.role.RoleManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.WallpaperHelper
import com.example.data.WallpaperTarget
import com.example.theme.ChromaThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val wallpaperHelper by lazy { WallpaperHelper(this) }
    val themeManager by lazy { ChromaThemeManager(applicationContext) }
    private var lastBatteryIntent: Intent? = null
    private val connectedAclDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val bluetoothBatteryLevels = ConcurrentHashMap<String, Int>()
    private var a2dpProfile: BluetoothProfile? = null
    private var headsetProfile: BluetoothProfile? = null

    @Volatile
    private var cachedInstalledAppsJson: String = "[]"
    @Volatile
    private var launchedFromHomeIntent: Boolean = false
    private var pendingWallpaperTarget: WallpaperTarget = WallpaperTarget.BOTH
    private var isTorchEnabled: Boolean = false
    private var torchCallbackRegistered: Boolean = false

    private val roleHomeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        notifyHardwareChanged()
    }

    private val wallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val target = pendingWallpaperTarget
            lifecycleScope.launch {
                val result = wallpaperHelper.applyWallpaperWithPreview(uri, target)
                result.fold(
                    onSuccess = { picked ->
                        val updated = themeManager.getCurrentTheme().copy(
                            wallpaperMode = "custom",
                            wallpaperTarget = target.name.lowercase(),
                            customWallpaperDataUrl = picked.previewDataUri
                        )
                        themeManager.saveThemeDraft(updated.toJson().toString())
                        Toast.makeText(this@MainActivity, picked.message, Toast.LENGTH_SHORT).show()
                        if (::webView.isInitialized) {
                            val escapedUri = JSONObject.quote(picked.previewDataUri)
                            val escapedTarget = JSONObject.quote(target.name.lowercase())
                            webView.post {
                                webView.evaluateJavascript(
                                    "window.onCustomWallpaperSelected && window.onCustomWallpaperSelected($escapedUri, $escapedTarget);",
                                    null
                                )
                            }
                        }
                    },
                    onFailure = {
                        Toast.makeText(
                            this@MainActivity,
                            it.message ?: "Could not set wallpaper",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initBluetoothProfileProxies()
        }
        notifyBatteryChanged()
        notifyHardwareChanged()
    }

    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        notifyHardwareChanged()
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            isTorchEnabled = enabled
            notifyHardwareChanged()
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProfile = proxy
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProfile = proxy
            }
            notifyBatteryChanged()
            notifyHardwareChanged()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProfile = null
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProfile = null
            }
            notifyBatteryChanged()
            notifyHardwareChanged()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                lastBatteryIntent = intent
                notifyBatteryChanged()
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val action = intent.action
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            if (device != null) {
                val addr = device.address
                if (!addr.isNullOrEmpty()) {
                    if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                        connectedAclDevices[addr] = device
                    } else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                        connectedAclDevices.remove(addr)
                        bluetoothBatteryLevels.remove(addr)
                    } else if (action == "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED") {
                        val lvl = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                        if (lvl in 0..100) {
                            bluetoothBatteryLevels[addr] = lvl
                        }
                    }
                }
            }
            notifyBatteryChanged()
            notifyHardwareChanged()
        }
    }

    private val hardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            notifyHardwareChanged()
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshInstalledAppsCacheAsync(notifyWeb = true)
        }
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission()) {
            bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun hasWriteSettingsPermission(): Boolean {
        return Settings.System.canWrite(this)
    }

    fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            writeSettingsLauncher.launch(intent)
        } catch (e: Exception) {
            startActivitySafe(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
    }

    fun isDefaultHomeLauncher(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == packageName
    }

    private fun initBluetoothProfileProxies() {
        try {
            if (!hasBluetoothPermission()) return
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter
            if (btAdapter != null && btAdapter.isEnabled) {
                btAdapter.getProfileProxy(this, profileListener, BluetoothProfile.A2DP)
                btAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        launchedFromHomeIntent = intent?.hasCategory(Intent.CATEGORY_HOME) == true
        registerReceivers()
        initBluetoothProfileProxies()
        registerTorchCallback()
        refreshInstalledAppsCacheAsync(notifyWeb = false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                themeManager.themeFlow.collect { theme ->
                    notifyThemeChanged(theme.toJson().toString())
                }
            }
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#0D0F12"))

            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d(
                            "ChromaOS_JS",
                            "${consoleMessage?.messageLevel()}: ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})"
                        )
                    }
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    notifyThemeChanged(themeManager.getCurrentTheme().toJson().toString())
                    notifyBatteryChanged()
                    notifyHardwareChanged()
                    if (cachedInstalledAppsJson != "[]") {
                        notifyInstalledAppsChanged()
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url ?: return false
                    val scheme = url.scheme ?: return false

                    if (scheme == "file") {
                        val path = url.path ?: ""
                        return !path.startsWith("/android_asset/chromaos/")
                    }

                    if (scheme == "tel") {
                        startActivitySafe(Intent(Intent.ACTION_DIAL, url))
                        return true
                    }
                    if (scheme == "mailto") {
                        startActivitySafe(Intent(Intent.ACTION_SENDTO, url))
                        return true
                    }

                    return try {
                        startActivitySafe(Intent(Intent.ACTION_VIEW, url))
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
            }

            addJavascriptInterface(ChromaAndroidBridge(this@MainActivity), "AndroidBridge")
        }

        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.handleAndroidBack ? window.handleAndroidBack() : false") { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else if (!isDefaultHomeLauncher()) {
                            moveTaskToBack(true)
                        }
                    }
                }
            }
        })

        webView.loadUrl("file:///android_asset/chromaos/index.html")
    }

    private fun registerReceivers() {
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val btFilter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        val hwFilter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lastBatteryIntent = registerReceiver(batteryReceiver, batteryFilter, RECEIVER_EXPORTED)
            runCatching { registerReceiver(bluetoothReceiver, btFilter, RECEIVER_EXPORTED) }
            runCatching { registerReceiver(hardwareReceiver, hwFilter, RECEIVER_EXPORTED) }
            runCatching { registerReceiver(packageReceiver, pkgFilter, RECEIVER_EXPORTED) }
        } else {
            lastBatteryIntent = registerReceiver(batteryReceiver, batteryFilter)
            runCatching { registerReceiver(bluetoothReceiver, btFilter) }
            runCatching { registerReceiver(hardwareReceiver, hwFilter) }
            runCatching { registerReceiver(packageReceiver, pkgFilter) }
        }
    }

    private fun registerTorchCallback() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            torchCallbackRegistered = true
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
        initBluetoothProfileProxies()
        notifyBatteryChanged()
        notifyHardwareChanged()
        refreshInstalledAppsCacheAsync(notifyWeb = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isHomeCategory = intent.hasCategory(Intent.CATEGORY_HOME)
        if (isHomeCategory) {
            launchedFromHomeIntent = true
        }
        if (isHomeCategory || intent.action == Intent.ACTION_MAIN) {
            notifyHardwareChanged()
            if (::webView.isInitialized) {
                webView.post {
                    webView.evaluateJavascript(
                        "window.onHomeIntent ? window.onHomeIntent($isHomeCategory) : (window.switchMobileNav && window.switchMobileNav('phone'))",
                        null
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(batteryReceiver) }
        runCatching { unregisterReceiver(bluetoothReceiver) }
        runCatching { unregisterReceiver(hardwareReceiver) }
        runCatching { unregisterReceiver(packageReceiver) }

        if (torchCallbackRegistered) {
            runCatching {
                val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cm?.unregisterTorchCallback(torchCallback)
            }
            torchCallbackRegistered = false
        }

        runCatching {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter
            a2dpProfile?.let { btAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
            headsetProfile?.let { btAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
        }

        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    fun startActivitySafe(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Action unavailable: ${e.localizedMessage ?: "No handler"}",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun refreshInstalledAppsCacheAsync(notifyWeb: Boolean) {
        lifecycleScope.launch(Dispatchers.Default) {
            val json = buildInstalledAppsJsonSync()
            cachedInstalledAppsJson = json
            if (notifyWeb) {
                withContext(Dispatchers.Main) {
                    notifyInstalledAppsChanged()
                }
            }
        }
    }

    private fun notifyInstalledAppsChanged() {
        if (!::webView.isInitialized) return
        val json = cachedInstalledAppsJson
        webView.post {
            webView.evaluateJavascript(
                "window.onInstalledAppsUpdated && window.onInstalledAppsUpdated($json);",
                null
            )
        }
    }

    private fun resolveAppCategory(appInfo: android.content.pm.ApplicationInfo?, isSystemApp: Boolean): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            return when (appInfo.category) {
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Media"
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
                android.content.pm.ApplicationInfo.CATEGORY_MAPS,
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "Web & Info"
                else -> if (isSystemApp) "System" else "Utilities"
            }
        }
        return if (isSystemApp) "System" else "Utilities"
    }

    fun buildInstalledAppsJsonSync(): String {
        return try {
            val pm = packageManager
            val seenComponents = mutableSetOf<String>()
            val items = mutableListOf<JSONObject>()

            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val launcherActivities = runCatching {
                launcherApps?.getActivityList(null, android.os.Process.myUserHandle())
            }.getOrNull().orEmpty()

            if (launcherActivities.isNotEmpty()) {
                for (info in launcherActivities) {
                    val component = info.componentName
                    val pkg = component.packageName
                    val actName = component.className
                    if (pkg == packageName) continue
                    val dedupKey = "$pkg/$actName"
                    if (!seenComponents.add(dedupKey)) continue

                    val appInfo = info.applicationInfo
                    if (!appInfo.enabled) continue

                    val label = info.label?.toString()?.ifBlank { pkg } ?: pkg
                    val drawable = runCatching { info.getIcon(0) }.getOrNull()
                        ?: runCatching { pm.getActivityIcon(component) }.getOrNull()
                    val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    val iconUri = if (drawable != null) encodeDrawableToDataUri(drawable) else ""

                    items.add(
                        JSONObject().apply {
                            put("packageName", pkg)
                            put("activityName", actName)
                            put("label", label)
                            put("iconDataUri", iconUri)
                            put("isSystemApp", isSystemApp)
                            put("isEnabled", appInfo.enabled)
                            put("category", resolveAppCategory(appInfo, isSystemApp))
                        }
                    )
                }
            }

            if (items.isEmpty()) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, 0)
                }

                for (info in resolveInfos) {
                    val actInfo = info.activityInfo ?: continue
                    val pkg = actInfo.packageName ?: continue
                    val actName = actInfo.name ?: continue
                    if (pkg == packageName) continue
                    val dedupKey = "$pkg/$actName"
                    if (!seenComponents.add(dedupKey)) continue

                    val appInfo = actInfo.applicationInfo
                    if (appInfo != null && !appInfo.enabled) continue

                    val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                    val drawable = runCatching { info.loadIcon(pm) }.getOrNull()
                    val isSystemApp = appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    val iconUri = if (drawable != null) encodeDrawableToDataUri(drawable) else ""

                    items.add(
                        JSONObject().apply {
                            put("packageName", pkg)
                            put("activityName", actName)
                            put("label", label)
                            put("iconDataUri", iconUri)
                            put("isSystemApp", isSystemApp)
                            put("isEnabled", appInfo?.enabled != false)
                            put("category", resolveAppCategory(appInfo, isSystemApp))
                        }
                    )
                }
            }

            items.sortBy { it.optString("label").lowercase() }

            val arr = JSONArray()
            items.forEach { arr.put(it) }
            arr.toString()
        } catch (_: Exception) {
            "[]"
        }
    }

    fun launchAndroidAppInternal(appIdOrPkg: String, explicitActivityName: String? = null): JSONObject {
        val raw = appIdOrPkg.trim()
        if (raw.isBlank()) {
            return JSONObject().apply {
                put("success", false)
                put("packageName", "")
                put("label", "")
                put("error", "Empty app identifier")
            }
        }

        val pm = packageManager
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val userHandle = android.os.Process.myUserHandle()

        fun buildResult(success: Boolean, pkg: String, label: String, error: String): JSONObject {
            return JSONObject().apply {
                put("success", success)
                put("packageName", pkg)
                put("label", label)
                put("error", error)
            }
        }

        fun runLaunchIntent(intent: Intent, pkgLabel: String, pkgName: String): JSONObject {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val resolved = intent.resolveActivity(pm)
            if (resolved == null) {
                val err = "No handler installed for $pkgLabel"
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                return buildResult(false, pkgName, pkgLabel, err)
            }
            return try {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    startActivity(intent)
                } else {
                    Handler(Looper.getMainLooper()).post {
                        runCatching { startActivity(intent) }.onFailure { ex ->
                            Toast.makeText(this, "Could not open $pkgLabel: ${ex.localizedMessage ?: "Launch error"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                buildResult(true, pkgName.ifBlank { resolved.packageName ?: raw }, pkgLabel, "")
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Unable to launch $pkgLabel"
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                buildResult(false, pkgName, pkgLabel, err)
            }
        }

        fun launchViaComponent(pkg: String, activityName: String, label: String): JSONObject {
            val component = ComponentName(pkg, activityName)
            val activityInfo = runCatching { pm.getActivityInfo(component, 0) }.getOrNull()
            val isLauncherEnabled = runCatching {
                launcherApps?.isActivityEnabled(component, userHandle) == true
            }.getOrDefault(false)

            if (activityInfo == null && !isLauncherEnabled) {
                val err = "Activity \"$pkg/$activityName\" is not installed or unavailable"
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                return buildResult(false, pkg, label, err)
            }
            if (activityInfo != null && (!activityInfo.enabled || activityInfo.applicationInfo?.enabled == false)) {
                val resolvedLabel = runCatching { activityInfo.loadLabel(pm).toString() }.getOrDefault(label)
                val err = "$resolvedLabel is disabled on this device"
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                return buildResult(false, pkg, resolvedLabel, err)
            }
            val resolvedLabel = activityInfo?.let { runCatching { it.loadLabel(pm).toString() }.getOrNull()?.ifBlank { label } } ?: label

            if (launcherApps != null) {
                val startedViaLauncherApps = try {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        launcherApps.startMainActivity(component, userHandle, null, null)
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            runCatching {
                                launcherApps.startMainActivity(component, userHandle, null, null)
                            }.onFailure {
                                startActivitySafe(
                                    Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_LAUNCHER)
                                        .setComponent(component)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                )
                            }
                        }
                    }
                    true
                } catch (_: Exception) {
                    false
                }
                if (startedViaLauncherApps) {
                    return buildResult(true, pkg, resolvedLabel, "")
                }
            }
            val componentIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                this.component = component
            }
            return runLaunchIntent(componentIntent, resolvedLabel, pkg)
        }

        // 1. Direct component launch if both packageName & activityName provided
        if (!explicitActivityName.isNullOrBlank() && raw.contains('.')) {
            val appInfo = runCatching { pm.getApplicationInfo(raw, 0) }.getOrNull()
            if (appInfo == null) {
                val err = "App \"$raw\" is not installed on this device"
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                return buildResult(false, raw, raw, err)
            }
            if (!appInfo.enabled) {
                val err = "$raw is disabled on this device"
                runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                return buildResult(false, raw, raw, err)
            }
            return launchViaComponent(raw, explicitActivityName, raw)
        }

        // 2. Logical app alias resolution (with real Realme/ColorOS + Google/AOSP package & category fallbacks)
        val firstInstalledPackage = { candidates: List<String> ->
            candidates.firstNotNullOfOrNull { pkg ->
                pm.getLaunchIntentForPackage(pkg)?.let { pkg to it }
            }
        }

        when (raw.lowercase()) {
            "phone", "dialer" -> return runLaunchIntent(Intent(Intent.ACTION_DIAL), "Phone", "")
            "settings" -> return runLaunchIntent(Intent(Settings.ACTION_SETTINGS), "Settings", "com.android.settings")
            "camera" -> {
                val camPkg = firstInstalledPackage(listOf("com.oplus.camera", "com.oppo.camera", "com.android.camera2", "com.google.android.GoogleCamera"))
                return if (camPkg != null) runLaunchIntent(camPkg.second, "Camera", camPkg.first)
                else runLaunchIntent(Intent(MediaStore.ACTION_IMAGE_CAPTURE), "Camera", "")
            }
            "browser", "chrome" -> {
                val browserPkg = firstInstalledPackage(listOf("com.android.chrome", "com.heytap.browser", "com.coloros.browser", "org.mozilla.firefox"))
                return if (browserPkg != null) runLaunchIntent(browserPkg.second, "Browser", browserPkg.first)
                else runLaunchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")), "Browser", "")
            }
            "messages", "sms" -> {
                val smsPkg = firstInstalledPackage(listOf("com.google.android.apps.messaging", "com.android.mms"))
                return if (smsPkg != null) runLaunchIntent(smsPkg.second, "Messages", smsPkg.first)
                else runLaunchIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING), "Messages", "")
            }
            "gallery", "photos" -> {
                val galleryPkg = firstInstalledPackage(listOf("com.google.android.apps.photos", "com.coloros.gallery3d", "com.oplus.gallery"))
                return if (galleryPkg != null) runLaunchIntent(galleryPkg.second, "Photos", galleryPkg.first)
                else runLaunchIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY), "Photos", "")
            }
            "music" -> return runLaunchIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC), "Music", "")
            "clock", "alarm" -> {
                val clockPkg = firstInstalledPackage(listOf("com.google.android.deskclock", "com.coloros.alarmclock", "com.android.deskclock"))
                return if (clockPkg != null) runLaunchIntent(clockPkg.second, "Clock", clockPkg.first)
                else runLaunchIntent(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS), "Clock", "")
            }
            "calculator" -> {
                val calcPkg = firstInstalledPackage(listOf("com.coloros.calculator", "com.google.android.calculator", "com.android.calculator2"))
                return if (calcPkg != null) runLaunchIntent(calcPkg.second, "Calculator", calcPkg.first)
                else runLaunchIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR), "Calculator", "")
            }
            "notes" -> {
                val notesPkg = firstInstalledPackage(listOf("com.coloros.note", "com.google.android.keep", "com.oplus.note"))
                return if (notesPkg != null) runLaunchIntent(notesPkg.second, "Notes", notesPkg.first)
                else runLaunchIntent(Intent(Intent.ACTION_CREATE_NOTE), "Notes", "")
            }
            "weather" -> {
                val weatherPkg = firstInstalledPackage(listOf("com.coloros.weather2", "com.coloros.weather", "com.oplus.weather2"))
                return if (weatherPkg != null) runLaunchIntent(weatherPkg.second, "Weather", weatherPkg.first)
                else runLaunchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.google.com")), "Weather", "")
            }
            "files" -> {
                val filesPkg = firstInstalledPackage(listOf("com.google.android.apps.nbu.files", "com.coloros.filemanager", "com.android.documentsui"))
                return if (filesPkg != null) runLaunchIntent(filesPkg.second, "Files", filesPkg.first)
                else runLaunchIntent(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), "Files", "")
            }
        }

        // 3. Direct Android package resolution (via LauncherApps then PackageManager)
        val installedAppInfo = runCatching { pm.getApplicationInfo(raw, 0) }.getOrNull()
        if (installedAppInfo != null && !installedAppInfo.enabled) {
            val label = runCatching { pm.getApplicationLabel(installedAppInfo).toString() }.getOrDefault(raw)
            val err = "$label is disabled"
            runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
            return buildResult(false, raw, label, err)
        }

        val matchedActivity = runCatching {
            launcherApps?.getActivityList(raw, userHandle)?.firstOrNull()
        }.getOrNull()
        if (matchedActivity != null) {
            val label = matchedActivity.label?.toString()?.ifBlank { raw } ?: raw
            return launchViaComponent(raw, matchedActivity.componentName.className, label)
        }

        val launchIntent = pm.getLaunchIntentForPackage(raw)
        if (launchIntent != null) {
            val label = installedAppInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: raw
            return runLaunchIntent(launchIntent, label, raw)
        }

        val err = "App \"$raw\" is not installed or has no launcher activity"
        runOnUiThread { Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
        return buildResult(false, raw, raw, err)
    }

    private fun encodeDrawableToDataUri(drawable: Drawable): String {
        return try {
            val targetSize = 72
            val bmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(drawable.bitmap, targetSize, targetSize, true)
                canvas.drawBitmap(scaled, 0f, 0f, null)
                if (scaled !== drawable.bitmap) {
                    scaled.recycle()
                }
            } else {
                drawable.setBounds(0, 0, targetSize, targetSize)
                drawable.draw(canvas)
            }

            val baos = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bmp.compress(format, 80, baos)
            bmp.recycle()
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/webp;base64,$b64"
        } catch (_: Exception) {
            ""
        }
    }

    fun applyThemeWallpaper(colorsJson: String, wallpaperMode: String, target: String) {
        lifecycleScope.launch {
            val parsedColors = try {
                val arr = JSONArray(colorsJson)
                (0 until arr.length()).map { idx -> Color.parseColor(arr.getString(idx)) }
            } catch (_: Exception) {
                listOf(Color.parseColor("#291C0E"), Color.parseColor("#6E473B"))
            }

            val dm = resources.displayMetrics
            val res = wallpaperHelper.applyThemeWallpaperBitmap(
                colors = parsedColors,
                wallpaperMode = wallpaperMode,
                whichScreen = WallpaperTarget.fromString(target),
                width = dm.widthPixels.coerceAtLeast(1080),
                height = dm.heightPixels.coerceAtLeast(1920)
            )
            res.fold(
                onSuccess = { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    Toast.makeText(this@MainActivity, err.message ?: "Wallpaper update restricted", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun createDeviceJsonObject(device: BluetoothDevice, defaultType: String): JSONObject {
        val addr = device.address ?: ""
        val devBattery = bluetoothBatteryLevels[addr] ?: -1

        val devName = try {
            if (hasBluetoothPermission()) device.name ?: "Bluetooth Device" else "Bluetooth Device"
        } catch (_: Exception) {
            "Bluetooth Device"
        }

        val isAudio = try {
            if (hasBluetoothPermission()) {
                device.bluetoothClass?.hasService(android.bluetooth.BluetoothClass.Service.AUDIO) == true
            } else false
        } catch (_: Exception) {
            false
        }

        val icon = when {
            isAudio || defaultType == "headphones" -> "ph-headphones"
            defaultType == "watch" -> "ph-watch"
            else -> "ph-bluetooth"
        }

        return JSONObject().apply {
            put("id", addr.ifEmpty { devName })
            put("name", devName)
            put("type", defaultType)
            put("battery", devBattery)
            put("hasBattery", devBattery >= 0)
            put("status", if (devBattery >= 0) "Connected ($devBattery%)" else "Connected")
            put("icon", icon)
            put("connected", true)
        }
    }

    fun getBatteryInfoJson(): String {
        val root = JSONObject()
        val bIntent = lastBatteryIntent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = bIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = bIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = bIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val tempTenths = bIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperature = if (tempTenths > 0) String.format("%.1f°C", tempTenths / 10.0f) else ""

        val healthCode = bIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val phoneObj = JSONObject().apply {
            put("percentage", if (percentage >= 0) percentage else -1)
            put("isAvailable", percentage >= 0)
            put("isCharging", isCharging)
            put("isFull", isFull)
            put("plugType", plugType)
            put("temperature", temperature)
            put("health", health)
            put(
                "statusText",
                when {
                    percentage < 0 -> "Reading Battery..."
                    isFull -> "Full (100%)"
                    isCharging -> "Charging ($plugType)"
                    else -> "Discharging"
                }
            )
        }
        root.put("phone", phoneObj)

        val devicesArray = JSONArray()
        try {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter
            val canAccessBt = hasBluetoothPermission()

            if (canAccessBt && btAdapter != null && btAdapter.isEnabled) {
                val seenAddresses = mutableSetOf<String>()

                runCatching {
                    a2dpProfile?.connectedDevices?.forEach { dev ->
                        if (dev != null && !dev.address.isNullOrEmpty() && seenAddresses.add(dev.address)) {
                            devicesArray.put(createDeviceJsonObject(dev, "headphones"))
                        }
                    }
                }
                runCatching {
                    headsetProfile?.connectedDevices?.forEach { dev ->
                        if (dev != null && !dev.address.isNullOrEmpty() && seenAddresses.add(dev.address)) {
                            devicesArray.put(createDeviceJsonObject(dev, "headset"))
                        }
                    }
                }
                for ((addr, dev) in connectedAclDevices) {
                    if (seenAddresses.add(addr)) {
                        val isAudio = runCatching {
                            dev.bluetoothClass?.hasService(android.bluetooth.BluetoothClass.Service.AUDIO) == true
                        }.getOrDefault(false)
                        devicesArray.put(createDeviceJsonObject(dev, if (isAudio) "headphones" else "bluetooth"))
                    }
                }
            }
        } catch (_: Exception) {}

        root.put("connectedDevices", devicesArray)
        root.put("hasBtPermission", hasBluetoothPermission())
        root.put("canRequestBtPermission", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission())
        return root.toString()
    }

    fun getHardwareStateJson(): String {
        val obj = JSONObject()
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
            val volumePct = if (maxVol > 0) ((curVol.toFloat() / maxVol) * 100).toInt() else 50

            val canWrite = hasWriteSettingsPermission()
            val sysBrightnessPct = try {
                val sysVal = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 180)
                ((sysVal.coerceIn(10, 255) / 255f) * 100).toInt()
            } catch (_: Exception) {
                70
            }
            val winBrightness = window.attributes.screenBrightness
            val effectiveBrightness = if (canWrite || winBrightness < 0f) {
                sysBrightnessPct
            } else {
                (winBrightness * 100).toInt().coerceIn(5, 100)
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiEnabled = runCatching { wifiManager?.isWifiEnabled ?: false }.getOrDefault(false)

            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter
            val btEnabled = runCatching { btAdapter?.isEnabled ?: false }.getOrDefault(false)

            obj.put("volume", volumePct)
            obj.put("brightness", effectiveBrightness)
            obj.put("isSystemBrightness", canWrite)
            obj.put("flashlightOn", isTorchEnabled)
            obj.put("wifiEnabled", wifiEnabled)
            obj.put("bluetoothEnabled", btEnabled)
            obj.put("hasBtPermission", hasBluetoothPermission())
            obj.put("canRequestBtPermission", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission())
            obj.put("isDefaultLauncher", isDefaultHomeLauncher())
            obj.put("sdkInt", Build.VERSION.SDK_INT)
            obj.put("versionName", BuildConfig.VERSION_NAME)
            obj.put("versionCode", BuildConfig.VERSION_CODE)
        } catch (_: Exception) {}
        return obj.toString()
    }

    fun notifyThemeChanged(themeJson: String) {
        if (!::webView.isInitialized) return
        val quoted = JSONObject.quote(themeJson)
        webView.post {
            webView.evaluateJavascript("window.onNativeThemeUpdated && window.onNativeThemeUpdated($quoted);", null)
        }
    }

    fun notifyBatteryChanged() {
        if (!::webView.isInitialized) return
        val json = getBatteryInfoJson()
        webView.post {
            webView.evaluateJavascript("window.onNativeBatteryUpdate && window.onNativeBatteryUpdate($json);", null)
        }
    }

    fun notifyHardwareChanged() {
        if (!::webView.isInitialized) return
        val json = getHardwareStateJson()
        webView.post {
            webView.evaluateJavascript("window.onNativeHardwareUpdate && window.onNativeHardwareUpdate($json);", null)
        }
    }

    inner class ChromaAndroidBridge(private val activity: MainActivity) {

        @JavascriptInterface
        fun getBatteryInfo(): String = activity.getBatteryInfoJson()

        @JavascriptInterface
        fun getHardwareState(): String = activity.getHardwareStateJson()

        @JavascriptInterface
        fun isDefaultHomeLauncher(): Boolean = activity.isDefaultHomeLauncher()

        @JavascriptInterface
        fun hasWriteSettingsPermission(): Boolean = activity.hasWriteSettingsPermission()

        @JavascriptInterface
        fun requestWriteSettingsPermission() {
            activity.runOnUiThread { activity.requestWriteSettingsPermission() }
        }

        @JavascriptInterface
        fun openBatterySettings() {
            activity.runOnUiThread {
                try {
                    activity.startActivitySafe(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                } catch (_: Exception) {
                    activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        @JavascriptInterface
        fun openBluetoothSettings() {
            activity.runOnUiThread {
                activity.startActivitySafe(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }

        @JavascriptInterface
        fun openSystemPanel(panelType: String) {
            activity.runOnUiThread {
                val intent = when (panelType.lowercase()) {
                    "internet", "wifi" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                    } else {
                        Intent(Settings.ACTION_WIFI_SETTINGS)
                    }
                    "volume", "sound" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_VOLUME)
                    } else {
                        Intent(Settings.ACTION_SOUND_SETTINGS)
                    }
                    "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    "battery" -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                    "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    "dnd" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                    else -> Intent(Settings.ACTION_SETTINGS)
                }
                activity.startActivitySafe(intent)
            }
        }

        @JavascriptInterface
        fun savePreferences(jsonStr: String) {
            try {
                activity.themeManager.saveThemeDraft(jsonStr)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun loadPreferences(): String {
            return try {
                activity.themeManager.getRawThemeJson()
            } catch (_: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun getCurrentTheme(): String {
            return try {
                activity.themeManager.getCurrentTheme().toJson().toString()
            } catch (_: Exception) {
                "{}"
            }
        }

        @JavascriptInterface
        fun saveThemeDraft(themeJson: String): String {
            return try {
                activity.themeManager.saveThemeDraft(themeJson).toJson().toString()
            } catch (_: Exception) {
                "{}"
            }
        }

        @JavascriptInterface
        fun resetThemeToDefault(): String {
            return try {
                activity.themeManager.resetToDefault().toJson().toString()
            } catch (_: Exception) {
                "{}"
            }
        }

        @JavascriptInterface
        fun applyTheme(themeJson: String): String {
            val savedTheme = activity.themeManager.saveThemeDraft(themeJson)
            val dm = activity.resources.displayMetrics
            activity.lifecycleScope.launch {
                val outcome = activity.themeManager.applyTheme(
                    rawJson = themeJson,
                    applyWallpaper = true,
                    widthPx = dm.widthPixels.coerceAtLeast(1080),
                    heightPx = dm.heightPixels.coerceAtLeast(1920)
                )
                val quotedOutcome = JSONObject.quote(outcome.toJsonString())
                Toast.makeText(activity, outcome.theme.themeName + " theme applied", Toast.LENGTH_SHORT).show()
                if (activity::webView.isInitialized) {
                    activity.webView.post {
                        activity.webView.evaluateJavascript(
                            "window.onNativeThemeApplied && window.onNativeThemeApplied($quotedOutcome);",
                            null
                        )
                    }
                }
            }
            return savedTheme.toJson().toString()
        }

        @JavascriptInterface
        fun isLaunchedAsHome(): Boolean = activity.isDefaultHomeLauncher() || activity.launchedFromHomeIntent

        @JavascriptInterface
        fun launchApp(appId: String): String {
            return activity.launchAndroidAppInternal(appId, null).toString()
        }

        @JavascriptInterface
        fun launchComponent(packageName: String, activityName: String): String {
            return activity.launchAndroidAppInternal(packageName, activityName).toString()
        }

        @JavascriptInterface
        fun refreshInstalledApps(): String {
            val fresh = activity.buildInstalledAppsJsonSync()
            activity.cachedInstalledAppsJson = fresh
            activity.notifyInstalledAppsChanged()
            return fresh
        }

        @JavascriptInterface
        fun getSystemInfo(): String {
            return try {
                JSONObject().apply {
                    put("manufacturer", Build.MANUFACTURER)
                    put("model", Build.MODEL)
                    put("device", Build.DEVICE)
                    put("androidVersion", Build.VERSION.RELEASE)
                    put("sdkInt", Build.VERSION.SDK_INT)
                    put("appName", "ChromaOS")
                    put("versionName", BuildConfig.VERSION_NAME)
                    put("versionCode", BuildConfig.VERSION_CODE)
                    put("isDefaultLauncher", activity.isDefaultHomeLauncher())
                }.toString()
            } catch (_: Exception) {
                "{}"
            }
        }

        @JavascriptInterface
        fun getInstalledApps(): String {
            if (activity.cachedInstalledAppsJson == "[]") {
                activity.cachedInstalledAppsJson = activity.buildInstalledAppsJsonSync()
            }
            return activity.cachedInstalledAppsJson
        }

        @JavascriptInterface
        fun vibrate(durationMs: Long) {
            try {
                val dur = durationMs.coerceIn(10, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vm?.defaultVibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(dur, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val v = activity.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v?.vibrate(
                            android.os.VibrationEffect.createOneShot(dur, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        v?.vibrate(dur)
                    }
                }
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun clearAppCache() {
            activity.runOnUiThread {
                try {
                    activity.webView.clearCache(true)
                    Toast.makeText(activity, "ChromaOS Cache Cleared", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        }

        @JavascriptInterface
        fun openAndroidSettings() {
            activity.runOnUiThread {
                activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
            }
        }

        @JavascriptInterface
        fun openHomeChooser() {
            activity.runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = activity.getSystemService(RoleManager::class.java)
                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        try {
                            activity.roleHomeLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                            return@runOnUiThread
                        } catch (_: Exception) {}
                    }
                }
                try {
                    activity.startActivitySafe(Intent(Settings.ACTION_HOME_SETTINGS))
                } catch (_: Exception) {
                    activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        @JavascriptInterface
        fun pickWallpaper() {
            pickWallpaperForTarget("both")
        }

        @JavascriptInterface
        fun pickWallpaperForTarget(target: String) {
            activity.runOnUiThread {
                activity.pendingWallpaperTarget = WallpaperTarget.fromString(target)
                activity.wallpaperPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }

        @JavascriptInterface
        fun applySystemWallpaper(primaryHex: String, secondaryHex: String) {
            val fallbackArr = JSONArray().apply {
                put(primaryHex)
                put(secondaryHex)
                put(primaryHex)
                put(secondaryHex)
                put(secondaryHex)
            }
            activity.applyThemeWallpaper(fallbackArr.toString(), "mesh", "both")
        }

        @JavascriptInterface
        fun applySystemWallpaperTheme(colorsJson: String, wallpaperMode: String, target: String) {
            activity.applyThemeWallpaper(colorsJson, wallpaperMode, target)
        }

        @JavascriptInterface
        fun requestBluetoothPermission() {
            activity.runOnUiThread {
                activity.requestBluetoothPermission()
            }
        }

        @JavascriptInterface
        fun hasBluetoothPermission(): Boolean = activity.hasBluetoothPermission()

        @JavascriptInterface
        fun openWifiSettings() {
            openSystemPanel("internet")
        }

        @JavascriptInterface
        fun setVolume(percent: Int) {
            activity.runOnUiThread {
                try {
                    val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@runOnUiThread
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val targetVol = ((percent.coerceIn(0, 100) / 100f) * maxVol).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                } catch (_: Exception) {}
            }
        }

        @JavascriptInterface
        fun getVolume(): Int {
            return try {
                val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 50
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (maxVol > 0) ((currVol.toFloat() / maxVol) * 100).toInt() else 50
            } catch (_: Exception) {
                50
            }
        }

        @JavascriptInterface
        fun setBrightness(percent: Int) {
            activity.runOnUiThread {
                try {
                    val clampedPct = percent.coerceIn(5, 100)
                    if (activity.hasWriteSettingsPermission()) {
                        Settings.System.putInt(
                            activity.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                        )
                        val sysBrightness = ((clampedPct / 100f) * 255).toInt().coerceIn(10, 255)
                        Settings.System.putInt(
                            activity.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            sysBrightness
                        )
                        val lp = activity.window.attributes
                        lp.screenBrightness = -1f
                        activity.window.attributes = lp
                    } else {
                        val lp = activity.window.attributes
                        lp.screenBrightness = clampedPct / 100f
                        activity.window.attributes = lp
                    }
                } catch (_: Exception) {}
            }
        }

        @JavascriptInterface
        fun getBrightness(): Int {
            if (activity.hasWriteSettingsPermission()) {
                try {
                    val sysVal = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 180)
                    return ((sysVal.coerceIn(10, 255) / 255f) * 100).toInt()
                } catch (_: Exception) {}
            }
            val b = activity.window.attributes.screenBrightness
            return if (b >= 0) (b * 100).toInt() else 70
        }

        @JavascriptInterface
        fun toggleFlashlight(enable: Boolean): String {
            return try {
                val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                if (cameraManager == null) {
                    val err = "Camera service unavailable"
                    return JSONObject().apply {
                        put("success", false)
                        put("enabled", activity.isTorchEnabled)
                        put("error", err)
                    }.toString()
                }
                val torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }.getOrDefault(false)
                } ?: cameraManager.cameraIdList.firstOrNull()

                if (torchCameraId == null) {
                    return JSONObject().apply {
                        put("success", false)
                        put("enabled", false)
                        put("error", "No camera flash hardware found")
                    }.toString()
                }
                cameraManager.setTorchMode(torchCameraId, enable)
                activity.isTorchEnabled = enable
                activity.notifyHardwareChanged()
                JSONObject().apply {
                    put("success", true)
                    put("enabled", enable)
                    put("error", "")
                }.toString()
            } catch (e: Exception) {
                activity.notifyHardwareChanged()
                JSONObject().apply {
                    put("success", false)
                    put("enabled", activity.isTorchEnabled)
                    put("error", e.localizedMessage ?: "Unable to toggle flashlight")
                }.toString()
            }
        }
    }
}
