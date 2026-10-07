package co.candyhouse.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import co.candyhouse.app.ui.QrScanner
import co.candyhouse.app.ui.AppMenu
import co.candyhouse.app.ui.AppToolbar
import co.candyhouse.app.util.AppEnvironment
import co.candyhouse.app.util.dp
import co.candyhouse.app.util.openExternalUrl
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
    private val qrScanner by lazy { QrScanner(this, root, scope, ::emit) }
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
            testSheet = InternalTestBottomSheet(this, scope, ble).also { it.show() }
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
        web.webViewClient = object : WebViewClient() {
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
        qrScanner.open()
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
        if (requestCode == 21 && resultCode == RESULT_OK) data?.data?.let { qrScanner.decodeImage(it) }
        if (requestCode == 20) {
            files?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)); files = null
        }
        if (requestCode == 11 && resultCode == RESULT_OK) requestBle()
    }

    override fun onResume() {
        super.onResume(); if (::web.isInitialized) {
            qrScanner.resume()
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
        qrScanner.pause(); if (::ble.isInitialized) {
            registration.stop(); ble.pause(); web.onPause()
        }; super.onPause()
    }

    private fun handleBack() {
        if (qrScanner.isOpen) {
            qrScanner.close(); emit("qrCancelled")
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
        testSheet?.setOnDismissListener(null); testSheet?.dismiss(); qrScanner.close(); registration.close(); ble.peripheralSettings.close(); ble.observers.remove(bleObserver); ble.pause(); bridge.reset(); files?.onReceiveValue(
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
