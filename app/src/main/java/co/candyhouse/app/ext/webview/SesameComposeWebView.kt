package co.candyhouse.app.ext.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import co.candyhouse.app.ext.webview.bridge.JSBridgeFactory
import co.candyhouse.app.ext.webview.bridge.WebViewJSBridge
import co.candyhouse.app.ext.webview.manager.WebViewCore
import co.candyhouse.app.ext.webview.manager.WebViewCore.cleanupWebView
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.ext.webview.manager.WebViewUrlLoader.rememberWebUrl
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.L
import co.utils.AnalyticsUtil
import co.utils.ContainerPaddingManager
import co.utils.SharedPreferencesUtils
import co.utils.getSerializableCompat
import co.utils.safeNavigate
import java.io.File

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
                extInfo = arguments?.getSerializableCompat<HashMap<String, String>>("extInfo") ?: hashMapOf(),
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
                                        WebViewPoolManager.setPendingRefresh("contacts")
                                        findNavController().popBackStack()
                                    }

                                    "RefreshList" -> {
                                        WebViewPoolManager.setPendingRefresh("contacts")
                                    }

                                    "UserProfileChanged" -> {
                                        WebViewPoolManager.setPendingRefresh("me-index")
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
    extInfo: Map<String, String> = hashMapOf(),
    onBackClick: () -> Unit,
    onMoreClick: (String) -> Unit,
    onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null
) {
    val logTag = "SesameComposeWebView"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 设置锁历史记录标题
    val titleNew = if (scene == "history") {
        SharedPreferencesUtils.preferences.getString(deviceId.lowercase(), title)
    } else {
        title
    }

    // 判断是否需要启用JS桥接
    val enableJSBridge = JSBridgeFactory.needsJSBridge(scene)

    var loading by remember { mutableStateOf(scene.isNotEmpty() && url.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val webUrl by rememberWebUrl(url, scene, deviceId, pushToken, paramInfo = extInfo) { errorMsg ->
        error = "URL_LOAD_ERROR:$errorMsg"
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
                                L.e(logTag, "notifyName=$notifyName")
                                if (notifyName == "FriendChanged") {
                                    L.e(logTag, "targetUrl=$targetUrl")
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
            L.e(logTag, "Cannot open file chooser: ${e.message}")
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
        L.d(logTag, "where=$where scene=$scene enableJSBridge=$enableJSBridge deviceId=$deviceId title=$titleNew pushToken=$pushToken")
        if (webUrl.isNotEmpty()) L.d(logTag, "url=$webUrl")
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
                    if (!titleNew.isNullOrEmpty()) {
                        Text(
                            text = titleNew,
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
                        WebViewCore.applyCommonSettings(wv, supportZoom = true)

                        if (enableJSBridge) {
                            jsBridge = JSBridgeFactory.setupJSBridge(
                                webView = wv,
                                scene = scene,
                                scope = scope,
                                onRequestNotificationSettings = {
                                    (context as? Activity)?.apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            startActivity(
                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                                }
                                            )
                                        } else {
                                            startActivity(
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", packageName, null)
                                                }
                                            )
                                        }
                                    }
                                }
                            )
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
                                    L.e(logTag, "Cannot create temp file: ${e.message}")
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

                        wv.webViewClient = WebViewCore.createWebViewClient(
                            onSchemeIntercept = { uri, params ->
                                schemeHandlers["ssm"]?.invoke(wv, uri, params)
                            },
                            onPageStarted = {
                                loading = true
                                error = null
                            },
                            onPageFinished = {
                                loading = false
                            },
                            onError = { errorMsg ->
                                error = errorMsg
                                loading = false
                            }
                        )

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

                    errorMsg.startsWith("URL_LOAD_ERROR:") -> {
                        LaunchedEffect(errorMsg) {
                            val actualMsg = errorMsg.removePrefix("URL_LOAD_ERROR:")
                            Toast.makeText(context, actualMsg, Toast.LENGTH_SHORT).show()
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
    keyLevel: String = "",
    height: Dp = 80.dp,
    refreshTrigger: Int = 0,
    extInfo: Map<String, String> = hashMapOf(),
    onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val logTag = "EmbeddedWebViewContent"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var dynamicHeight by remember { mutableStateOf(height) }

    // 判断是否需要启用JS桥接
    val enableJSBridge = JSBridgeFactory.needsJSBridge(scene)

    var loading by remember { mutableStateOf(scene.isNotEmpty() && url.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val webUrl by rememberWebUrl(url, scene, deviceId, keyLevel = keyLevel, paramInfo = extInfo) { errorMsg ->
        error = "URL_LOAD_ERROR:$errorMsg"
        loading = false
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewInitialized by remember { mutableStateOf(false) }

    val schemeHandlers = remember { createSchemeHandlers(onSchemeIntercept) }

    var jsBridge by remember { mutableStateOf<WebViewJSBridge?>(null) }

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

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0 && webViewRef != null) {
            webViewRef?.reload()
            L.d(logTag, "Reloading webview due to refresh trigger: $refreshTrigger")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dynamicHeight)
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
                    WebViewCore.applyCommonSettings(wv, supportZoom = false)

                    if (enableJSBridge) {
                        jsBridge = JSBridgeFactory.setupJSBridge(
                            webView = wv,
                            scene = scene,
                            scope = scope,
                            onHeightChanged = { newHeight ->
                                dynamicHeight = newHeight.dp
                                L.d(logTag, "WebView height updated to: ${newHeight}dp")
                            }
                        )
                    }

                    wv.webViewClient = WebViewCore.createWebViewClient(
                        onSchemeIntercept = { uri, params ->
                            schemeHandlers["ssm"]?.invoke(wv, uri, params)
                        },
                        onPageStarted = {
                            loading = true
                            error = null
                        },
                        onPageFinished = {
                            loading = false
                        },
                        onError = { errorMsg ->
                            error = errorMsg
                            loading = false
                        }
                    )

                    isWebViewInitialized = true
                }
            }

            val target = webUrl
            if (target.isNotEmpty() && wv.url != target) {
                loading = true
                wv.loadUrl(target)
                L.d(logTag, "Loading embedded webview: $target")
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

        error?.takeIf { !loading }?.let { errorMsg ->
            when {
                errorMsg.contains("Please log in", ignoreCase = true) -> {
                    LaunchedEffect(errorMsg) {
                        Toast.makeText(context, "Please log in", Toast.LENGTH_SHORT).show()
                    }
                }

                errorMsg.startsWith("URL_LOAD_ERROR:") -> {
                    LaunchedEffect(errorMsg) {
                        val actualMsg = errorMsg.removePrefix("URL_LOAD_ERROR:")
                        Toast.makeText(context, actualMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error ?: "Failed to load")
                    }
                }
            }
        }
    }
}

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