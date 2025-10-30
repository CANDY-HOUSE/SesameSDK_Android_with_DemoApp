package co.candyhouse.app.tabs.menu

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgComposeWebviewBinding
import co.candyhouse.app.tabs.friend.ContactsWebViewManager
import co.candyhouse.server.CHLoginAPIManager.getWebUrlByScene
import co.candyhouse.server.CHResultState
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.L
import co.utils.AnalyticsUtil
import co.utils.ContainerPaddingManager
import co.utils.WebViewJSBridge
import co.utils.safeNavigate
import java.io.File

/**
 * WebView通用设置
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun WebView.setupCommonSettings(supportZoom: Boolean = true) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        setSupportZoom(supportZoom)
        builtInZoomControls = supportZoom
        displayZoomControls = false
        cacheMode = WebSettings.LOAD_DEFAULT
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        allowFileAccess = false
        allowContentAccess = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }
    }
    setBackgroundColor(Color.TRANSPARENT)
}

/**
 * 创建Scheme处理器
 */
internal fun createSchemeHandlers(
    onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)?
): MutableMap<String, (WebView?, Uri, Map<String, String>) -> Boolean> {
    return mutableMapOf<String, (WebView?, Uri, Map<String, String>) -> Boolean>().apply {
        this["ssm"] = { _, uri, params ->
            onSchemeIntercept?.invoke(uri, params)
            true
        }
    }
}

/**
 * 通过scene获取WebView URL
 */
@Composable
internal fun rememberWebUrl(
    initialUrl: String,
    scene: String,
    deviceId: String,
    pushToken: String = "",
    onError: (String) -> Unit = {}
): State<String> {
    val webUrl = remember { mutableStateOf(initialUrl) }

    LaunchedEffect(scene) {
        if (scene.isNotEmpty() && initialUrl.isEmpty()) {
            val extInfo = buildMap {
                when {
                    pushToken.isNotEmpty() -> {
                        put("pushToken", pushToken)
                    }

                    deviceId.isNotEmpty() -> {
                        put("deviceUUID", deviceId)
                    }
                }
            }

            getWebUrlByScene(scene, extInfo.takeIf { it.isNotEmpty() }) { result ->
                result.fold(
                    onSuccess = { state ->
                        webUrl.value = ((state as? CHResultState.CHResultStateNetworks)?.data ?: "") as String
                        L.d("SesameComposeWebView", "rememberWebUrl-Web URL: ${webUrl.value}")
                    },
                    onFailure = { t ->
                        val errorMsg = t.message ?: "Load url failed"
                        L.e("SesameComposeWebView", "rememberWebUrl-Error: $errorMsg")
                        onError(errorMsg)
                    }
                )
            }
        }
    }

    return webUrl
}

/**
 * WebView清理逻辑
 */
internal fun cleanupWebView(webView: WebView?) {
    webView?.let { wv ->
        wv.stopLoading()
        wv.clearCache(true)
        wv.clearHistory()
        wv.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        wv.removeAllViews()
        wv.destroy()
    }
}

class SesameComposeWebView : Fragment() {

    private val logTag = "SesameComposeWebView"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            SesameComposeWebViewContent(
                url = arguments?.getString("url") ?: "",
                scene = arguments?.getString("scene") ?: "",
                deviceId = arguments?.getString("deviceId") ?: "",
                where = arguments?.getString("where") ?: "",
                title = arguments?.getString("title") ?: "",
                pushToken = arguments?.getString("pushToken") ?: "",
                onBackClick = {
                    exit()
                },
                onMoreClick = { whereValue ->
                    when (whereValue) {
                        "device_history_old" -> safeNavigate(R.id.action_mainRoomFG_to_SSM2SettingFG)
                        "device_history_new" -> safeNavigate(R.id.action_mainRoomSS5FG_to_SSM5SettingFG)
                    }
                },
                onSchemeIntercept = { uri, params ->
                    when (uri.path) {
                        "/webview/notify" -> {
                            params["notifyName"]?.let { notifyName ->
                                L.e(logTag, "notifyName=$notifyName")
                                when (notifyName) {
                                    "FriendChanged" -> {
                                        ContactsWebViewManager.setPendingRefresh()
                                        findNavController().popBackStack()
                                    }

                                    "RefreshList" -> {
                                        ContactsWebViewManager.setPendingRefresh()
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ContainerPaddingManager.requestClearPadding(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ContainerPaddingManager.releaseClearPadding(this)
    }

    private fun exit() {
        if (!findNavController().popBackStack()) {
            findNavController().navigate(R.id.deviceListPG)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SesameComposeWebViewContent(
    url: String = "",
    scene: String = "",
    deviceId: String = "",
    where: String = "",
    title: String = "",
    pushToken: String = "",
    onBackClick: () -> Unit,
    onMoreClick: (String) -> Unit,
    onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null
) {
    val tag = "SesameComposeWebView"
    val scope = rememberCoroutineScope()

    // 判断是否需要启用JS桥接
    val enableJSBridge = scene == "device-notify"

    var loading by remember { mutableStateOf(scene.isNotEmpty() && url.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val webUrl by rememberWebUrl(url, scene, deviceId, pushToken) { errorMsg ->
        error = errorMsg
        loading = false
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewInitialized by remember { mutableStateOf(false) }

    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val schemeHandlers = remember {
        mutableMapOf<String, (WebView?, Uri, Map<String, String>) -> Boolean>().apply {
            this["ssm"] = { _, uri, params ->
                when (uri.path) {
                    "/webview/open" -> {
                        params["url"]?.let { targetUrl ->
                            params["notifyName"]?.let { notifyName ->
                                L.e(tag, "notifyName=$notifyName")
                                if (notifyName == "FriendChanged") {
                                    L.e(tag, "targetUrl=$targetUrl")
                                    pendingUrl = targetUrl
                                }
                            }
                        }
                    }

                    else -> {
                        onSchemeIntercept?.invoke(uri, params)
                    }
                }
                true
            }
        }
    }

    var jsBridge by remember { mutableStateOf<WebViewJSBridge?>(null) }

    val context = LocalContext.current
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraIntent by remember { mutableStateOf<Intent?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results = when {
            result.resultCode == Activity.RESULT_OK -> {
                if (data?.data == null && cameraPhotoUri != null) {
                    arrayOf(cameraPhotoUri!!)
                } else {
                    data?.data?.let { arrayOf(it) } ?: data?.clipData?.let { clipData ->
                        (0 until clipData.itemCount).map {
                            clipData.getItemAt(it).uri
                        }.toTypedArray()
                    }
                }
            }

            else -> null
        }
        fileChooserCallback?.onReceiveValue(results)
        fileChooserCallback = null
        cameraPhotoUri = null
    }

    fun launchFileChooser(includeCameraIntent: Intent? = null) {
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }

        val chooserIntent = Intent.createChooser(contentSelectionIntent, "QRコード選択").apply {
            includeCameraIntent?.let {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(it))
            }
        }

        try {
            fileChooserLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            L.e(tag, "Cannot open file chooser: ${e.message}")
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
            cameraPhotoUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingCameraIntent != null) {
            launchFileChooser(pendingCameraIntent)
        } else {
            launchFileChooser()
        }
        pendingCameraIntent = null
    }

    LaunchedEffect(Unit) {
        L.d(tag, "where=$where scene=$scene deviceId=$deviceId title=$title enableJSBridge=$enableJSBridge pushToken=$pushToken")
        if (webUrl.isNotEmpty()) L.d(tag, "url=$webUrl")
    }

    DisposableEffect(Unit) {
        onDispose {
            if (enableJSBridge) {
                webViewRef?.removeJavascriptInterface("AndroidHandler")
                jsBridge = null
            }
            cleanupWebView(webViewRef)
            webViewRef = null
        }
    }

    LaunchedEffect(pendingUrl) {
        pendingUrl?.let { newUrl ->
            webViewRef?.loadUrl(newUrl)
            pendingUrl = null
        }
    }

    BackHandler {
        val wv = webViewRef
        if (wv?.canGoBack() == true) wv.goBack() else onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val wv = webViewRef
                        if (wv?.canGoBack() == true) wv.goBack() else onBackClick()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = "Back",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                },
                actions = {
                    if (scene == "history") {
                        IconButton(onClick = { onMoreClick(where) }) {
                            Icon(painter = painterResource(id = R.drawable.ic_icons_filled_more), contentDescription = "More")
                        }
                    } else {
                        Box(modifier = Modifier.width(48.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AndroidViewBinding(FgComposeWebviewBinding::inflate, modifier = Modifier.fillMaxSize()) {
                val wv = composeWebView

                if (webViewRef !== wv) {
                    webViewRef = wv

                    if (!isWebViewInitialized) {
                        wv.setupCommonSettings(supportZoom = true)

                        if (enableJSBridge) {
                            L.d(tag, "Adding JS Bridge for scene='$scene'")
                            jsBridge = WebViewJSBridge(wv, scope)
                            wv.addJavascriptInterface(jsBridge!!, "AndroidHandler")
                        }

                        wv.webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback

                                // 创建拍照Intent和临时文件
                                var takePictureIntent: Intent? = null
                                try {
                                    val photoFile = File.createTempFile(
                                        "JPEG_${System.currentTimeMillis()}_",
                                        ".jpg",
                                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                    )
                                    cameraPhotoUri = FileProvider.getUriForFile(
                                        context,
                                        BuildConfig.APPLICATION_ID,
                                        photoFile
                                    )
                                    takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                        putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                                    }
                                } catch (e: Exception) {
                                    L.e(tag, "Cannot create temp file: ${e.message}")
                                }

                                when {
                                    takePictureIntent == null -> {
                                        launchFileChooser()
                                    }

                                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                            != PackageManager.PERMISSION_GRANTED -> {
                                        pendingCameraIntent = takePictureIntent
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }

                                    else -> {
                                        launchFileChooser(takePictureIntent)
                                    }
                                }

                                return true
                            }
                        }

                        wv.webViewClient = object : WebViewClient() {

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                error = null
                            }

                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                super.onPageCommitVisible(view, url)
                                loading = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                loading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return false
                                val scheme = uri.scheme?.lowercase()

                                scheme?.let {
                                    schemeHandlers[it]?.let { handler ->
                                        val params = uri.queryParameterNames.associateWith { key ->
                                            uri.getQueryParameter(key) ?: ""
                                        }
                                        if (handler(view, uri, params)) {
                                            return true
                                        }
                                    }
                                }

                                if (scheme != "http" && scheme != "https") {
                                    return try {
                                        view?.context?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                return false
                            }

                            @RequiresApi(Build.VERSION_CODES.M)
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResp: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    error = errorResp?.description?.toString() ?: "Load error"
                                    loading = false
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                error = description ?: "Load error"
                                loading = false
                            }

                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                err: SslError?
                            ) {
                                handler?.cancel()
                                error = "SSL error"
                                loading = false
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    error = "HTTP ${errorResponse?.statusCode ?: ""}"
                                    loading = false
                                }
                            }
                        }

                        isWebViewInitialized = true
                    }
                }

                val target = webUrl
                if (target.isNotEmpty() && wv.url != target) {
                    loading = true
                    wv.loadUrl(target)

                    // firebase 数据埋点
                    val screenName = when (where) {
                        CHDeviceManager.NOTIFICATION_FLAG -> "WebViewFG_notification"
                        CHDeviceManager.SHOP_FLAG -> "WebViewFG_shop"
                        else -> null
                    }
                    screenName?.let { AnalyticsUtil.logScreenView(screenName = it) }
                }
            }

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 8.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp
                )
            }

            error?.takeIf { !loading }?.let { errorMsg ->
                when {
                    errorMsg.contains("Please log in", ignoreCase = true) -> {
                        LaunchedEffect(errorMsg) {
                            Toast.makeText(context, "Please log in", Toast.LENGTH_SHORT).show()
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = errorMsg)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                error = null
                                webViewRef?.reload()
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWebViewContent(
    url: String = "",
    scene: String = "",
    deviceId: String = "",
    height: Dp = 80.dp,
    refreshTrigger: Int = 0,
    onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val tag = "EmbeddedWebView"

    var loading by remember { mutableStateOf(scene.isNotEmpty() && url.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val webUrl by rememberWebUrl(url, scene, deviceId) { errorMsg ->
        error = errorMsg
        loading = false
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewInitialized by remember { mutableStateOf(false) }

    val schemeHandlers = remember { createSchemeHandlers(onSchemeIntercept) }

    DisposableEffect(Unit) {
        onDispose {
            cleanupWebView(webViewRef)
            webViewRef = null
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0 && webViewRef != null) {
            webViewRef?.reload()
            L.d(tag, "Reloading webview due to refresh trigger: $refreshTrigger")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        AndroidViewBinding(
            FgComposeWebviewBinding::inflate,
            modifier = Modifier.fillMaxSize()
        ) {
            val wv = composeWebView

            if (webViewRef !== wv) {
                webViewRef = wv

                if (!isWebViewInitialized) {
                    // 使用共享设置，但禁用缩放（局部WebView通常不需要）
                    wv.setupCommonSettings(supportZoom = false)
                    wv.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

                    wv.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            error = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val scheme = uri.scheme?.lowercase()

                            scheme?.let {
                                schemeHandlers[it]?.let { handler ->
                                    val params = uri.queryParameterNames.associateWith { key ->
                                        uri.getQueryParameter(key) ?: ""
                                    }
                                    if (handler(view, uri, params)) {
                                        return true
                                    }
                                }
                            }

                            if (scheme != "http" && scheme != "https") {
                                return try {
                                    view?.context?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            return false
                        }

                        @RequiresApi(Build.VERSION_CODES.M)
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResp: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                error = errorResp?.description?.toString() ?: "Load error"
                                loading = false
                            }
                        }
                    }

                    isWebViewInitialized = true
                }
            }

            val target = webUrl
            if (target.isNotEmpty() && wv.url != target) {
                loading = true
                wv.loadUrl(target)
                L.d(tag, "Loading embedded webview: $target")
            }
        }

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(16.dp),
                strokeWidth = 2.dp
            )
        }

        if (error != null && !loading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = error ?: "Failed to load")
            }
        }
    }
}