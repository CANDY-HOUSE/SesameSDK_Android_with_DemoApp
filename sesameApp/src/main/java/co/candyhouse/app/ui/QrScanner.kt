package co.candyhouse.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import co.candyhouse.app.R
import co.candyhouse.app.util.dp
import co.candyhouse.app.util.qrcode.core.QRCodeView
import co.candyhouse.app.util.qrcode.zxing.QRCodeDecoder
import co.candyhouse.app.util.qrcode.zxing.ZXingView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Camera/gallery presentation only; QR business interpretation remains in Biz. */
class QrScanner(
    private val activity: Activity,
    private val root: FrameLayout,
    private val scope: CoroutineScope,
    private val emit: (String, Any) -> Unit
) {
    private var scanner: ZXingView? = null
    private var scannerOverlay: FrameLayout? = null
    val isOpen get() = scanner != null
    fun resume() { scanner?.apply { startCamera(); startSpotAndShowRect() } }
    fun pause() { scanner?.stopCamera() }
    fun open() {
        if (scanner != null) return
        val qrView = activity.layoutInflater.inflate(R.layout.view_qr_scanner, root, false) as ZXingView
        scanner = qrView
        val overlay = FrameLayout(activity).apply { setBackgroundColor(Color.BLACK) }
        scannerOverlay = overlay
        overlay.addView(qrView, FrameLayout.LayoutParams(-1, -1))
        fun control(icon: Int, description: Int, gravityValue: Int, action: () -> Unit): ImageView {
            val button = ImageView(activity).apply {
                setImageResource(icon); scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(activity.dp(22), activity.dp(22), activity.dp(22), activity.dp(22))
                contentDescription = activity.getString(description)
                setOnClickListener { action() }
            }
            overlay.addView(button, FrameLayout.LayoutParams(activity.dp(80), activity.dp(80), gravityValue))
            return button
        }
        control(R.drawable.ic_icons_filled_close_white, R.string.close, Gravity.TOP or Gravity.END) {
            close(); emit("qrCancelled", org.json.JSONObject())
        }
        val gallery = control(R.drawable.ic_icons_filled_album, R.string.gallery, Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
            activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 21)
        }
        qrView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val box = qrView.scanBoxView
            val params = gallery.layoutParams as FrameLayout.LayoutParams
            val textBottom = box.topOffset + box.rectHeight + box.tipTextMargin + (box.tipTextSl?.height ?: box.tipTextSize)
            val top = (textBottom + activity.dp(8)).coerceAtMost((overlay.height - activity.dp(80)).coerceAtLeast(0))
            if (params.topMargin != top) {
                params.topMargin = top
                gallery.layoutParams = params
            }
        }
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        qrView.apply {
            setDelegate(object : QRCodeView.Delegate {
                override fun onScanQRCodeSuccess(result: String) {
                    close(); emit("qr", result)
                }

                override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {}
                override fun onScanQRCodeOpenCameraError() {
                    close(); emit("error", "Unable to open camera")
                }
            })
            startCamera(); startSpotAndShowRect()
        }
    }

    fun close() {
        scanner?.onDestroy(); scanner = null
        scannerOverlay?.let { root.removeView(it) }; scannerOverlay = null
    }

    fun decodeImage(uri: Uri) {
        val currentScanner = scanner ?: return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    activity.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (bounds.outWidth / inSampleSize > 2048 || bounds.outHeight / inSampleSize > 2048) inSampleSize *= 2
                    }
                    val bitmap = activity.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                        ?: error("Invalid image")
                    try {
                        QRCodeDecoder.syncDecodeQRCode(bitmap) ?: error("No QR code found")
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            if (scanner !== currentScanner) return@launch
            result.onSuccess { close(); emit("qr", it) }
                .onFailure { emit("error", activity.getString(R.string.qr_not_found)) }
        }
    }

}
