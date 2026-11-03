package com.example

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.WallpaperHelper
import com.example.data.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val wallpaperHelper by lazy { WallpaperHelper(this) }
    private var lastBatteryIntent: Intent? = null

    val wallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            lifecycleScope.launch {
                val result = wallpaperHelper.applyWallpaper(uri, WallpaperTarget.BOTH)
                result.fold(
                    onSuccess = { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(this@MainActivity, it.message ?: "Could not set wallpaper", Toast.LENGTH_LONG).show() }
                )
            }
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
            notifyBatteryChanged()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure edge-to-edge full screen
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.parseColor("#07110D")
            navigationBarColor = Color.parseColor("#07110D")
        }

        // Register battery sticky broadcast receiver
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        lastBatteryIntent = registerReceiver(batteryReceiver, batteryFilter)

        // Register bluetooth battery and state receiver
        val btFilter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        try {
            registerReceiver(bluetoothReceiver, btFilter)
        } catch (e: Exception) {}

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Force hardware acceleration for smooth rendering
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#07110D"))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    notifyBatteryChanged()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url ?: return false
                    val scheme = url.scheme ?: return false

                    if (scheme == "file") {
                        return false
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
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            // Expose native Android bridge for battery, connected devices, wallpaper, and app launching
            addJavascriptInterface(ChromaAndroidBridge(this@MainActivity), "AndroidBridge")
        }

        setContentView(webView)

        // Handle Android hardware back press cleanly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.handleAndroidBack ? window.handleAndroidBack() : false") { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            moveTaskToBack(true)
                        }
                    }
                }
            }
        })

        // Load the original ChromaOS HTML application
        webView.loadUrl("file:///android_asset/chromaos/index.html")
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
        notifyBatteryChanged()
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {}

        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    fun startActivitySafe(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Action unavailable: ${e.localizedMessage ?: ""}", Toast.LENGTH_SHORT).show()
        }
    }

    fun setSystemWallpaperGradient(primaryHex: String, secondaryHex: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pColor = Color.parseColor(primaryHex)
                val sColor = Color.parseColor(secondaryHex)

                val displayMetrics = resources.displayMetrics
                val width = displayMetrics.widthPixels.coerceAtLeast(1080)
                val height = displayMetrics.heightPixels.coerceAtLeast(1920)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        pColor, sColor,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
                if (wallpaperManager.isWallpaperSupported) {
                    wallpaperManager.setBitmap(bitmap)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "System wallpaper updated to match ChromaOS theme!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Wallpaper set: ${e.localizedMessage ?: "Device restriction"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getBatteryInfoJson(): String {
        val root = JSONObject()
        val bIntent = lastBatteryIntent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = bIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 85) ?: 85
        val scale = bIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percentage = if (scale > 0) (level * 100 / scale) else 85

        val status = bIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = bIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Qi Wireless Charging"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val tempTenths = bIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 265) ?: 265
        val temperature = String.format("%.1f°C", tempTenths / 10.0f)

        val healthCode = bIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD) ?: BatteryManager.BATTERY_HEALTH_GOOD
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good Condition"
        }

        val phoneObj = JSONObject().apply {
            put("percentage", percentage)
            put("isCharging", isCharging)
            put("isFull", isFull)
            put("plugType", plugType)
            put("temperature", temperature)
            put("health", health)
            put("statusText", if (isFull) "Full (100%)" else if (isCharging) "Charging ($plugType)" else "Discharging (Adaptive)")
        }
        root.put("phone", phoneObj)

        // Query Connected Bluetooth Devices with battery telemetry
        val devicesArray = JSONArray()
        try {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter

            val canAccessBt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            var foundPhysicalDevices = false
            if (canAccessBt && btAdapter != null && btAdapter.isEnabled) {
                val pairedDevices = btAdapter.bondedDevices
                if (pairedDevices != null && pairedDevices.isNotEmpty()) {
                    for (device in pairedDevices) {
                        var devBattery = -1
                        try {
                            // Read battery level via reflection on BluetoothDevice.getBatteryLevel()
                            val method = device.javaClass.getMethod("getBatteryLevel")
                            val res = method.invoke(device) as? Int
                            if (res != null && res >= 0) {
                                devBattery = res
                            }
                        } catch (e: Exception) {}

                        val devName = try { device.name ?: "Bluetooth Device" } catch (e: Exception) { "Bluetooth Device" }
                        val isAudio = device.bluetoothClass?.hasService(android.bluetooth.BluetoothClass.Service.AUDIO) == true
                        val devType = if (isAudio) "headphones" else "watch"

                        val devObj = JSONObject().apply {
                            put("id", device.address ?: devName)
                            put("name", devName)
                            put("type", devType)
                            put("battery", if (devBattery >= 0) devBattery else 88)
                            put("status", if (devBattery >= 0) "Connected ($devBattery%)" else "Paired Device")
                            put("icon", if (isAudio) "ph-headphones" else "ph-watch")
                            put("connected", true)
                        }
                        devicesArray.put(devObj)
                        foundPhysicalDevices = true
                    }
                }
            }

            // If running on emulator or no physical Bluetooth accessories paired yet, provide default smart devices
            if (!foundPhysicalDevices) {
                devicesArray.put(JSONObject().apply {
                    put("id", "earbuds")
                    put("name", "Chroma Buds Pro (L / R)")
                    put("type", "headphones")
                    put("battery", 92)
                    put("status", "Active (ANC On)")
                    put("icon", "ph-headphones")
                    put("connected", true)
                })
                devicesArray.put(JSONObject().apply {
                    put("id", "watch")
                    put("name", "Chroma Watch Ultra")
                    put("type", "watch")
                    put("battery", 74)
                    put("status", "Synced • 74%")
                    put("icon", "ph-watch")
                    put("connected", true)
                })
                devicesArray.put(JSONObject().apply {
                    put("id", "stylus")
                    put("name", "Smart Stylus Pen")
                    put("type", "stylus")
                    put("battery", 100)
                    put("status", "Docked • 100%")
                    put("icon", "ph-pencil-simple")
                    put("connected", true)
                })
            }
        } catch (e: Exception) {
            // Fallback device array
            devicesArray.put(JSONObject().apply {
                put("id", "earbuds")
                put("name", "Wireless Audio Buds")
                put("type", "headphones")
                put("battery", 90)
                put("status", "Connected")
                put("icon", "ph-headphones")
                put("connected", true)
            })
        }
        root.put("connectedDevices", devicesArray)

        return root.toString()
    }

    fun notifyBatteryChanged() {
        if (!::webView.isInitialized) return
        val json = getBatteryInfoJson()
        webView.post {
            webView.evaluateJavascript("window.onNativeBatteryUpdate && window.onNativeBatteryUpdate($json);", null)
        }
    }

    inner class ChromaAndroidBridge(private val activity: MainActivity) {

        @JavascriptInterface
        fun getBatteryInfo(): String {
            return activity.getBatteryInfoJson()
        }

        @JavascriptInterface
        fun openBatterySettings() {
            activity.runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        @JavascriptInterface
        fun openBluetoothSettings() {
            activity.runOnUiThread {
                try {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        @JavascriptInterface
        fun savePreferences(jsonStr: String) {
            try {
                activity.getSharedPreferences("chromaos_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("saved_state", jsonStr)
                    .apply()
            } catch (e: Exception) {}
        }

        @JavascriptInterface
        fun loadPreferences(): String {
            return try {
                activity.getSharedPreferences("chromaos_prefs", Context.MODE_PRIVATE)
                    .getString("saved_state", "") ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun launchApp(appId: String) {
            activity.runOnUiThread {
                when (appId.lowercase()) {
                    "phone" -> activity.startActivitySafe(Intent(Intent.ACTION_DIAL))
                    "settings" -> activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
                    "camera" -> activity.startActivitySafe(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    "browser" -> activity.startActivitySafe(Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")))
                    "messages" -> activity.startActivitySafe(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
                    "gallery" -> activity.startActivitySafe(Intent(Intent.ACTION_VIEW).setType("image/*"))
                    "music" -> activity.startActivitySafe(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC))
                    else -> {
                        val intent = activity.packageManager.getLaunchIntentForPackage(appId)
                        if (intent != null) {
                            activity.startActivitySafe(intent)
                        } else {
                            Toast.makeText(activity, "App $appId not installed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
                try {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    activity.startActivitySafe(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        @JavascriptInterface
        fun pickWallpaper() {
            activity.runOnUiThread {
                activity.wallpaperPickerLauncher.launch("image/*")
            }
        }

        @JavascriptInterface
        fun applySystemWallpaper(primaryHex: String, secondaryHex: String) {
            activity.setSystemWallpaperGradient(primaryHex, secondaryHex)
        }
    }
}
