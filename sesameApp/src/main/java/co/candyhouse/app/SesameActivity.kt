package co.candyhouse.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.webkit.PermissionRequest
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
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService
import co.candyhouse.app.data.KeyHandoff
import co.candyhouse.app.nfc.NfcTags
import co.candyhouse.app.push.PushNotification
import co.candyhouse.app.ui.AppRoute
import co.candyhouse.app.ui.AppToolbar
import co.candyhouse.app.ui.ExternalPage
import co.candyhouse.app.ui.RequestCode
import co.candyhouse.app.ui.StartupCover
import co.candyhouse.app.ui.WebMediaAccess
import co.candyhouse.app.util.AppEnvironment
import co.candyhouse.app.util.dp
import co.candyhouse.app.util.haptic
import co.candyhouse.app.util.openExternalUrl
import co.candyhouse.app.util.openNotificationSettings
import co.candyhouse.app.util.toKeyJson
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHScanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** One browser surface; Biz draws every page, menu, QR scanner and internal test of its own. */
class SesameActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var webChrome: WebChromeClient
    private lateinit var toolbar: AppToolbar
    private lateinit var webContainer: FrameLayout
    private lateinit var pageProgress: ProgressBar
    private lateinit var startupCover: StartupCover
    private var startupDocumentFailed = false
    private val nfc by lazy { NfcTags(this, scope, ble, ::requestBle, ::emit) }
    private var nfcIntent: Intent? = null
    private var webReady = false
    private var pageLoading = false
    private var pageLoadGeneration = 0L
    private var currentRoute = AppRoute.HOME
    private lateinit var ble: BleController
    private val bleObserver: (JSONArray) -> Unit = { if (::bridge.isInitialized) emit("snapshot", it) }
    private lateinit var registration: BleRegistrationController
    private val externalPage by lazy { ExternalPage(this, webContainer, { webChrome }, ::beginPageLoading, ::finishPageLoading, ::returnToApp) }
    private val childWeb get() = externalPage.view
    private val keyHandoff by lazy { KeyHandoff(this) }
    private lateinit var bridge: WebViewBridge
    private val media by lazy { WebMediaAccess(this, { bridge.isTrusted(it) }) { if (::web.isInitialized && !isFinishing && !isDestroyed) web.reload() } }
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
        startupCover = StartupCover(this, root) {
            startupDocumentFailed = false
            bridge.reset()
            webReady = false
            web.stopLoading()
            web.loadUrl(entry)
        }
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
        toolbar = AppToolbar(this, ::handleBack).apply { visibility = View.GONE }
        webContainer = FrameLayout(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(-1, dp(48)))
            addView(webContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        pageProgress = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = ColorStateList.valueOf(0xff28aeb1.toInt())
        }
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
            showRoute = {
                if (it.path != currentRoute.path) {
                    nfc.binding = null
                }
                currentRoute = it
            },
            pushInfo = {
                JSONObject().put("pushToken", push.token()).put("appIdentifyId", push.installationId).put("env", JSONArray(AppEnvironment.collect(this)))
                    .put("language", resources.configuration.locales[0].toLanguageTag()).put("enabled", NotificationManagerCompat.from(this).areNotificationsEnabled())
            },
            notificationSettings = ::openNotificationSettings,
            openWebPage = ::openWebPage,
            openExternal = { openExternalUrl(it.toUri()) },
            requestLocationPermission = {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), RequestCode.AUTO_UNLOCK_LOCATION)
            },
            openAppSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) },
            watchNfc = { nfc.binding = it },
            haptic = { haptic(web, it) },
            uiReady = { if (!startupDocumentFailed) startupCover.ready(web) }
        )
        bridge = WebViewBridge(origin, scope, requests::handle)
        (application as SesameApp).bleBackend.requestCloud = bridge::requestCloud
    }

    private fun observePush() {
        scope.launch { (application as SesameApp).subscriptionManager.changes.collect { emit("push") } }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), RequestCode.NOTIFICATIONS)
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
            // Only our trusted Biz origin loads here (external pages use ExternalPage's own WebView).
            // Face3 live view opened from a doorbell push must start with the user's saved speaker
            // setting; requiring a gesture forced it muted behind "tap to play".
            mediaPlaybackRequiresUserGesture = false
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val uri = request.url
                if (bridge.isTrusted(uri)) return false
                if (uri.scheme in setOf("https", "http", "mailto", "tel")) runCatching { openExternalUrl(uri) }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (view !== web) return
                startupDocumentFailed = false
                startupCover.begin()
                ble.peripheralSettings.close()
                webReady = false
                bridge.reset()
                registration.close(); discovering = false
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                if (view === web && view.url == url && startupCover.active && !startupDocumentFailed && bridge.isTrusted((url ?: "").toUri())) {
                    bridge.connect(view, url)
                }
                if (childWeb != null || !pageLoading || view.url != url) return
                val generation = pageLoadGeneration
                view.postVisualStateCallback(generation, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        if (view === web && requestId == pageLoadGeneration) finishPageLoading()
                    }
                })
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && view === web) {
                    startupDocumentFailed = true
                    startupCover.fail()
                    finishPageLoading()
                }
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && view === web) {
                    startupDocumentFailed = true
                    startupCover.fail()
                    finishPageLoading()
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (view !== web || view.url != url) return
                webReady = true
                if (bridge.isTrusted((url ?: "").toUri())) {
                    bridge.connect(view, url)
                    nfcIntent?.let { nfc.handle(it); nfcIntent = null }
                }
                openPendingNotification()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (view !== web) return true
                startupCover.begin()
                media.cancel()
                bridge.reset()
                returnToApp()
                webContainer.removeView(view); view.destroy()
                createWebView()
                finishPageLoading()
                return true
            }
        }
        webChrome = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) = media.request(request)
            override fun onPermissionRequestCanceled(request: PermissionRequest) = media.cancelled(request)
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams) = media.chooseFile(callback, params)
        }
        web.webChromeClient = webChrome
        webContainer.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.loadUrl(entry)
    }

    private fun emit(type: String, data: Any = JSONObject()) = bridge.emit(type, data)

    private fun navigate(path: String) = emit("navigate", path)

    private var askedBle = false
    private var promptedBle = false
    private fun requestBle() {
        val permissions =
            if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            if (!promptedBle || discovering) {
                askedBle = true; promptedBle = true; requestPermissions(permissions, RequestCode.BLE_PERMISSIONS)
            }
            return
        }
        ble.resume()
        if (discovering) {
            if (CHBleManager.mScanning == CHScanStatus.BleClose) startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RequestCode.BLE_ENABLE)
            else registration.start()
        }
    }

    private fun openWebPage(url: String) {
        require(url.toUri().scheme == "https")
        returnToApp()
        externalPage.open(url)
        web.visibility = View.INVISIBLE
        beginPageLoading()
    }

    private fun returnToApp() {
        if (!externalPage.close()) return
        pageLoadGeneration++
        finishPageLoading()
    }

    private fun beginPageLoading() {
        pageLoadGeneration++
        pageLoading = true
        updateToolbarVisibility()
        (childWeb ?: web).visibility = View.INVISIBLE
        pageProgress.visibility = View.VISIBLE
    }

    private fun finishPageLoading() {
        pageLoading = false
        pageProgress.visibility = View.GONE
        (childWeb ?: web).visibility = View.VISIBLE
        updateToolbarVisibility()
    }

    private fun updateToolbarVisibility() {
        toolbar.visibility = if (childWeb != null) View.VISIBLE else View.GONE
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfc.handle(intent)
        emit("push")
        openPendingNotification()
    }

    private fun openPendingNotification() {
        if (!webReady) return
        PushNotification.consumeRoute(intent)?.let { route ->
            // The same push also carries url for older apps; drop it so no external page opens too.
            PushNotification.consumeUrl(intent)
            returnToApp()
            navigate(route)
            return
        }
        PushNotification.consumeUrl(intent)?.let { runCatching { openWebPage(it) } }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        when (requestCode) {
            RequestCode.WEB_MEDIA -> return media.permissionResult(results)
            RequestCode.AUTO_UNLOCK_LOCATION -> return emit("locationPermission")
            RequestCode.NOTIFICATIONS -> return SesameConnectedDeviceService.sync(this)
            RequestCode.BLE_PERMISSIONS -> if (results.isEmpty()) {
                promptedBle = false; return
            }
        }
        if (results.isEmpty() || results.any { it != PackageManager.PERMISSION_GRANTED }) {
            emit("error", "Permission denied"); return
        }
        if (requestCode == RequestCode.BLE_PERMISSIONS) requestBle()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.FILE_CHOOSER) media.fileResult(resultCode, data)
        if (requestCode == RequestCode.BLE_ENABLE && resultCode == RESULT_OK) requestBle()
    }

    override fun onResume() {
        super.onResume(); if (::web.isInitialized) {
            nfc.enable()
            SesameConnectedDeviceService.sync(this)
            childWeb?.onResume()
            web.onResume(); if (askedBle) requestBle(); emit("resume")
        }
    }

    override fun onPause() {
        if (::web.isInitialized) emit("pause")
        nfc.disable()
        if (::ble.isInitialized) {
            childWeb?.onPause()
            registration.stop(); ble.pause(); web.onPause()
        }; super.onPause()
    }

    private fun handleBack() {
        if (childWeb != null) {
            if (!externalPage.back()) returnToApp()
        } else if (!currentRoute.exits) emit("back")
        else finish()
    }

    override fun onDestroy() {
        if (::startupCover.isInitialized) startupCover.close()
        media.close()
        (application as SesameApp).bleBackend.requestCloud = null
        pageLoadGeneration++
        registration.close(); ble.peripheralSettings.close(); ble.observers.remove(bleObserver); ble.pause(); bridge.reset()
        returnToApp()
        webContainer.removeView(web); web.destroy(); scope.cancel()
        (application as SesameApp).apply {
            activityPresent = false
            if (SesameConnectedDeviceService.live != null) ensureBackgroundWeb() else ble.close()
        }
        super.onDestroy()
    }
}
