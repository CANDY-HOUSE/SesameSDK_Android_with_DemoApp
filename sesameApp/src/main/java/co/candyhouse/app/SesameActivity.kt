package co.candyhouse.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import co.candyhouse.app.ble.BleController
import co.candyhouse.app.ble.BleRegistrationController
import co.candyhouse.app.bridge.NativeRequestHandler
import co.candyhouse.app.bridge.WebViewBridge
import co.candyhouse.app.connecteddevice.AutoUnlockMap
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService
import co.candyhouse.app.data.KeyHandoff
import co.candyhouse.app.internal.InternalTestBottomSheet
import co.candyhouse.app.push.PushNotification
import co.candyhouse.app.ui.AppMenu
import co.candyhouse.app.ui.AppToolbar
import co.candyhouse.app.util.AppEnvironment
import co.candyhouse.app.util.BundledWebResources
import co.candyhouse.app.util.WebResourceSettings
import co.candyhouse.app.util.dp
import co.candyhouse.app.util.openExternalUrl
import co.candyhouse.app.util.qrcode.core.QRCodeView
import co.candyhouse.app.util.qrcode.zxing.QRCodeDecoder
import co.candyhouse.app.util.qrcode.zxing.ZXingView
import co.candyhouse.app.util.toKeyJson
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHScanStatus
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/** One browser surface with native menu, QR scanner and internal BLE test controls. */
class SesameActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var toolbar: AppToolbar
    private lateinit var webContainer: FrameLayout
    private lateinit var pageProgress: ProgressBar
    private lateinit var autoUnlockMap: AutoUnlockMap
    private var nfcDevice: String? = null
    private var nfcIntent: Intent? = null
    private var webReady = false
    private var pageLoading = false
    private var pageLoadGeneration = 0L
    private var currentRoute = "/"
    private val tabPaths = setOf("/", "/vision", "/contacts", "/me/homepage")
    private lateinit var ble: BleController
    private val bleObserver: (JSONArray) -> Unit = { if (::bridge.isInitialized) emit("snapshot", it) }
    private lateinit var registration: BleRegistrationController
    private var scanner: ZXingView? = null
    private var scannerOverlay: FrameLayout? = null
    private var testSheet: InternalTestBottomSheet? = null
    private var returnUrl: String? = null
    private val actionMenu by lazy { AppMenu(this, { navigate("/app/register") }, ::scanQr, { navigate("/contact-add") }) }
    private val keyHandoff by lazy { KeyHandoff(this) }
    private lateinit var bridge: WebViewBridge
    private var files: ValueCallback<Array<Uri>>? = null
    private var registeredKey: JSONObject? = null
    private var discovering = false
    private val origin = BuildConfig.WEB_ORIGIN
    private val entry get() = "$origin/?appHome=1&fromType=app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })
        (application as SesameApp).apply { activityPresent = true; stopBackgroundWeb() }
        initializeBle()
        createLayout()
        autoUnlockMap = AutoUnlockMap(this, webContainer, { web }) { emit("devicePreferences", it) }
        connectBridge()
        createWebView()
        observePush()
        nfcIntent = intent.takeIf { it.action?.startsWith("android.nfc.action.") == true }
        // Let WebView start loading before asking Firebase to refresh the installation token.
        web.post { scope.launch { (application as SesameApp).subscriptionManager.refreshToken() } }
    }

    private fun initializeBle() {
        ble = (application as SesameApp).ble
        ble.observers.add(bleObserver)
        registration = BleRegistrationController(scope, { emit("discovery", it) }, { byteArrayOf() }) {
            keyHandoff.add(it.getKey())
            registeredKey = it.getKey().toKeyJson().put("keyLevel", 0).put("deviceName", it.productModel.deviceModelName())
        }
    }

    private fun createLayout() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        toolbar = AppToolbar(this, ::handleBack, ::showMenu) {
            testSheet?.dismiss()
            val wasBundled = WebResourceSettings.isBundled(this)
            testSheet = InternalTestBottomSheet(this, scope, ble).also { sheet ->
                sheet.setOnDismissListener {
                    if (!isFinishing && wasBundled != WebResourceSettings.isBundled(this)) recreate()
                }
                sheet.show()
            }
        }
        webContainer = FrameLayout(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(-1, dp(48)))
            addView(webContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        pageProgress = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(pageProgress, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
    }

    private fun connectBridge() {
        val push = (application as SesameApp).subscriptionManager
        val requests = NativeRequestHandler(
            context = applicationContext,
            ble, registration, keyHandoff,
            language = { resources.configuration.locales[0].toLanguageTag() },
            requestBle = ::requestBle,
            startDiscovery = { discovering = true; requestBle() },
            stopDiscovery = { discovering = false; registration.stop() },
            registeredKey = { registeredKey },
            scanQr = ::scanQr,
            showRoute = {
                if (it != currentRoute) {
                    nfcDevice = null; autoUnlockMap.close()
                }
                currentRoute = it
                toolbar.showPage(root = it in tabPaths, home = it == "/")
                updateToolbarVisibility()
            },
            pushInfo = {
                JSONObject().put("pushToken", push.token()).put("appIdentifyId", push.installationId).put("env", JSONArray(AppEnvironment.collect(this)))
                    .put("language", resources.configuration.locales[0].toLanguageTag()).put("enabled", NotificationManagerCompat.from(this).areNotificationsEnabled())
            },
            notificationSettings = {
                val settings = if (Build.VERSION.SDK_INT >= 26) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
                startActivity(settings)
            },
            openWebPage = ::openWebPage,
            openExternal = { openExternalUrl(it.toUri()) },
            autoUnlockMap = autoUnlockMap::handle,
            watchNfc = { nfcDevice = it },
            haptic = { web.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        )
        bridge = WebViewBridge(origin, scope, requests::handle)
        (application as SesameApp).bleBackend.requestCloud = bridge::requestCloud
    }

    private fun observePush() {
        scope.launch { (application as SesameApp).subscriptionManager.changes.collect { emit("push") } }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 13)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        web = WebView(this)
        web.settings.apply {
            javaScriptEnabled = true
            userAgentString += " SesameAndroid/1"
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = true
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        L.i("WebResources", "mode=${if (WebResourceSettings.isBundled(this)) "APK_BUNDLE" else "ONLINE"}; origin=$origin")
        val bundledResources = if (WebResourceSettings.isBundled(this)) BundledWebResources(assets, origin) else null
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                bundledResources?.intercept(request)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val uri = request.url
                if (bridge.isTrusted(uri)) return false
                if (returnUrl != null && uri.scheme == "https") return false
                if (uri.scheme in setOf("https", "http", "mailto", "tel")) runCatching { openExternalUrl(uri) }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                autoUnlockMap.close()
                ble.peripheralSettings.close()
                if (returnUrl != null || pageLoading) beginPageLoading()
                webReady = false
                bridge.reset()
                registration.close(); discovering = false
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                if (!pageLoading || view.url != url) return
                val generation = pageLoadGeneration
                view.postVisualStateCallback(generation, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        if (view === web && requestId == pageLoadGeneration) finishPageLoading()
                    }
                })
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && view === web) finishPageLoading()
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && view === web) finishPageLoading()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (view !== web || view.url != url) return
                webReady = true
                if (bridge.isTrusted((url ?: "").toUri())) {
                    returnUrl = null
                    bridge.connect(view, url)
                    nfcIntent?.let { handleNfc(it); nfcIntent = null }
                }
                openPendingNotification()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                bridge.reset()
                webContainer.removeView(view); view.destroy()
                returnUrl = null
                toolbar.showPage(root = true, home = false)
                createWebView()
                finishPageLoading()
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                files?.onReceiveValue(null); files = callback
                return runCatching { startActivityForResult(params.createIntent(), 20); true }.getOrElse {
                    files?.onReceiveValue(null); files = null; false
                }
            }
        }
        webContainer.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.loadUrl(entry)
    }

    private fun emit(type: String, data: Any = JSONObject()) = bridge.emit(type, data)

    private fun navigate(path: String) = emit("navigate", path)
    private fun showMenu() = actionMenu.show(toolbar.menu)

    private var askedBle = false
    private fun requestBle() {
        val permissions =
            if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            if (!askedBle || discovering) {
                askedBle = true; requestPermissions(permissions, 10)
            }
            return
        }
        ble.resume()
        if (discovering) {
            if (CHBleManager.mScanning == CHScanStatus.BleClose) startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 11)
            else registration.start()
        }
    }

    private fun scanQr() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 12); return
        }
        if (scanner != null) return
        val qrView = layoutInflater.inflate(R.layout.view_qr_scanner, root, false) as ZXingView
        scanner = qrView
        val overlay = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        scannerOverlay = overlay
        overlay.addView(qrView, FrameLayout.LayoutParams(-1, -1))
        fun control(icon: Int, description: Int, gravityValue: Int, action: () -> Unit): ImageView {
            val button = ImageView(this).apply {
                setImageResource(icon); scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(22), dp(22), dp(22), dp(22))
                contentDescription = getString(description)
                setOnClickListener { action() }
            }
            overlay.addView(button, FrameLayout.LayoutParams(dp(80), dp(80), gravityValue))
            return button
        }
        control(R.drawable.ic_icons_filled_close_white, R.string.close, Gravity.TOP or Gravity.END) {
            closeScanner(); emit("qrCancelled")
        }
        val gallery = control(R.drawable.ic_icons_filled_album, R.string.gallery, Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 21)
        }
        qrView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val box = qrView.scanBoxView
            val params = gallery.layoutParams as FrameLayout.LayoutParams
            val textBottom = box.topOffset + box.rectHeight + box.tipTextMargin + (box.tipTextSl?.height ?: box.tipTextSize)
            val top = (textBottom + dp(8)).coerceAtMost((overlay.height - dp(80)).coerceAtLeast(0))
            if (params.topMargin != top) {
                params.topMargin = top
                gallery.layoutParams = params
            }
        }
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        qrView.apply {
            setDelegate(object : QRCodeView.Delegate {
                override fun onScanQRCodeSuccess(result: String) {
                    closeScanner(); emit("qr", result)
                }

                override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {}
                override fun onScanQRCodeOpenCameraError() {
                    closeScanner(); emit("error", "Unable to open camera")
                }
            })
            startCamera(); startSpotAndShowRect()
        }
    }

    private fun closeScanner() {
        scanner?.onDestroy(); scanner = null
        scannerOverlay?.let { root.removeView(it) }; scannerOverlay = null
    }

    private fun openWebPage(url: String) {
        require(url.toUri().scheme == "https")
        if (returnUrl == null) returnUrl = web.url?.takeIf { bridge.isTrusted(it.toUri()) } ?: entry
        bridge.reset()
        beginPageLoading()
        toolbar.showPage(root = false, home = false)
        web.loadUrl(url)
    }

    private fun returnToApp() {
        val target = returnUrl ?: entry
        returnUrl = null
        beginPageLoading()
        toolbar.showPage(root = currentRoute in tabPaths, home = currentRoute == "/")
        web.loadUrl(target)
    }

    private fun beginPageLoading() {
        pageLoadGeneration++
        pageLoading = true
        toolbar.visibility = View.INVISIBLE
        web.visibility = View.INVISIBLE
        pageProgress.visibility = View.VISIBLE
    }

    private fun finishPageLoading() {
        pageLoading = false
        pageProgress.visibility = View.GONE
        web.visibility = View.VISIBLE
        updateToolbarVisibility()
    }

    private fun updateToolbarVisibility() {
        val h5Header = currentRoute in setOf(
            "/device-setting",
            "/device-setting/rename",
            "/device-setting/angle",
            "/device-setting/auto-unlock",
            "/device-setting/bot-script",
            "/device-setting/credentials"
        ) || currentRoute == "/biz/devices/list-item" || currentRoute == "/device-history" ||
                currentRoute == "/biz/access-control/region" || currentRoute == "/biz/wifi-module" || currentRoute == "/biz/wifi-module/index"
        toolbar.visibility = if (returnUrl == null && h5Header) View.GONE else if (pageLoading) View.INVISIBLE else View.VISIBLE
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfc(intent)
        emit("push")
        openPendingNotification()
    }

    private fun openPendingNotification() {
        if (!webReady) return
        PushNotification.consumeUrl(intent)?.let { runCatching { openWebPage(it) } }
    }

    private fun handleNfc(intent: Intent) {
        if (intent.action !in setOf(
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )
        ) return
        @Suppress("DEPRECATION") val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val tagId = tag.id.joinToString("") { "%02x".format(it.toInt() and 255) }
        val app = application as SesameApp
        nfcDevice?.let { id ->
            app.devicePreferences.setNfc(id, tagId); emit("devicePreferences", app.devicePreferences.snapshot(id))
            if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) scope.launch(Dispatchers.IO) {
                runCatching {
                    val message = NdefMessage(arrayOf(NdefRecord.createTextRecord(null, "candynfc")))
                    val ndef = Ndef.get(tag)
                    if (ndef != null) {
                        try {
                            ndef.connect(); ndef.writeNdefMessage(message)
                        } finally {
                            ndef.close()
                        }
                    } else NdefFormatable.get(tag)?.let { format ->
                        try {
                            format.connect(); format.format(message)
                        } finally {
                            format.close()
                        }
                    }
                }.onFailure { L.e("SesameNfc", "Tag formatting failed", it) }
            }
            return
        }
        scope.launch {
            ble.restoreSaved(); requestBle()
            ble.allDevices().filter { app.devicePreferences.nfc(it.deviceId.toString()).equals(tagId, true) }.forEach { device ->
                launch {
                    runCatching {
                        withTimeout(12000.milliseconds) {
                            while (device.deviceStatus.value != CHDeviceLoginStatus.logined && device.deviceId.toString()
                                    .uppercase() !in app.bleBackend.gatewayDevices
                            ) delay(500.milliseconds)
                            if (device.deviceStatus.value == CHDeviceLoginStatus.logined) ble.handle(
                                "home.command",
                                JSONObject().put("deviceUUID", device.deviceId.toString())
                            ) { _, _ -> }
                            else app.bleBackend.requestCloud?.invoke("widgetCommand", JSONObject().put("operation", "toggle"), device.deviceId.toString())
                        }
                    }.onFailure { L.e("SesameNfc", "Tag operation failed", it) }
                }
            }
        }
    }

    private fun decodeQrImage(uri: Uri) {
        val currentScanner = scanner ?: return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (bounds.outWidth / inSampleSize > 2048 || bounds.outHeight / inSampleSize > 2048) inSampleSize *= 2
                    }
                    val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                        ?: error("Invalid image")
                    try {
                        QRCodeDecoder.syncDecodeQRCode(bitmap) ?: error("No QR code found")
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            if (scanner !== currentScanner) return@launch
            result.onSuccess { closeScanner(); emit("qr", it) }
                .onFailure { emit("error", getString(R.string.qr_not_found)) }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 14) {
            autoUnlockMap.permissionResult(); return
        }
        if (requestCode == 13) {
            SesameConnectedDeviceService.sync(this); return
        }
        if (results.isEmpty() || results.any { it != PackageManager.PERMISSION_GRANTED }) {
            emit("error", "Permission denied"); return
        }
        if (requestCode == 10) requestBle() else if (requestCode == 12) scanQr()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 21 && resultCode == RESULT_OK) data?.data?.let { decodeQrImage(it) }
        if (requestCode == 20) {
            files?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)); files = null
        }
        if (requestCode == 11 && resultCode == RESULT_OK) requestBle()
    }

    override fun onResume() {
        super.onResume(); if (::web.isInitialized) {
            scanner?.apply { startCamera(); startSpotAndShowRect() }
            autoUnlockMap.resume()
            NfcAdapter.getDefaultAdapter(this)?.enableForegroundDispatch(
                this,
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, SesameActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                ), null, null
            )
            SesameConnectedDeviceService.sync(this)
            web.onResume(); if (askedBle) requestBle(); emit("resume")
        }
    }

    override fun onPause() {
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
        autoUnlockMap.pause()
        actionMenu.dismiss()
        testSheet?.dismiss(); testSheet = null
        scanner?.stopCamera(); if (::ble.isInitialized) {
            registration.stop(); ble.pause(); web.onPause()
        }; super.onPause()
    }

    private fun handleBack() {
        if (scanner != null) {
            closeScanner(); emit("qrCancelled")
        } else if (returnUrl != null) {
            val history = web.copyBackForwardList()
            val previous = history.getItemAtIndex(history.currentIndex - 1)?.url
            if (previous != null && !bridge.isTrusted(previous.toUri())) web.goBack() else returnToApp()
        } else if (currentRoute !in tabPaths) emit("back")
        else finish()
    }

    override fun onDestroy() {
        autoUnlockMap.close()
        (application as SesameApp).bleBackend.requestCloud = null
        pageLoadGeneration++
        testSheet?.setOnDismissListener(null); testSheet?.dismiss(); closeScanner(); registration.close(); ble.peripheralSettings.close(); ble.observers.remove(bleObserver); ble.pause(); bridge.reset(); files?.onReceiveValue(
            null
        )
        webContainer.removeView(web); web.destroy(); scope.cancel()
        (application as SesameApp).apply {
            activityPresent = false
            if (SesameConnectedDeviceService.live != null) ensureBackgroundWeb() else ble.close()
        }
        super.onDestroy()
    }
}
