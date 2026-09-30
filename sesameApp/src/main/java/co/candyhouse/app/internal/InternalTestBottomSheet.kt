package co.candyhouse.app.internal

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import co.candyhouse.app.R
import co.candyhouse.app.SesameApp
import co.candyhouse.app.ble.BleController
import co.candyhouse.app.util.WebResourceSettings
import co.candyhouse.app.util.dp
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHDeviceStatusDelegate
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.os3.bot2.CHSesameBot2
import co.candyhouse.sesame.ble.os3.sesame5.CHSesame5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

/** Native-only internal stress test. Keeps the existing counter and per-lock interval preferences. */
class InternalTestBottomSheet(context: Context, private val scope: CoroutineScope, private val ble: BleController) : Dialog(context) {
    private val prefs = context.getSharedPreferences("lock_toggle_test_prefs", Context.MODE_PRIVATE)
    private val locks = ble.testDevices().filter { testActionFor(it) != null }
    private var selected = locks.find { it.deviceId.toString() == prefs.getString("selected_id", null) } ?: locks.firstOrNull()
    private var count = prefs.getInt("toggle_count", 0)
    private var job: Job? = null
    private var testSessionId = 0
    private var previous: CHDeviceStatusDelegate? = null
    private var testDelegate: CHDeviceStatusDelegate? = null
    private val content = LayoutInflater.from(context).inflate(R.layout.view_internal_test, null)
    private val stats = content.findViewById<TextView>(R.id.tvStats)
    private val subtitle = content.findViewById<TextView>(R.id.tvSubtitle)
    private val options = content.findViewById<LinearLayout>(R.id.lockRows)
    private val start = content.findViewById<Button>(R.id.btnStart)
    private val stop = content.findViewById<Button>(R.id.btnStop)
    private val reset = content.findViewById<Button>(R.id.btnReset)

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        content.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
        content.clipToOutline = true
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(context.dp(16) + safe.left, context.dp(12), context.dp(16) + safe.right, context.dp(24) + safe.bottom)
            insets
        }
        setContentView(content)
        content.findViewById<View>(R.id.btnClose).setOnClickListener { dismiss() }
        start.setOnClickListener { startTest() }
        stop.setOnClickListener { stopTest() }
        reset.setOnClickListener {
            if (job == null) {
                count = 0
                prefs.edit().apply { putInt("toggle_count", 0); remove("selected_id"); locks.forEach { remove("interval_${it.deviceId}") } }.apply()
                selected = locks.firstOrNull(); render()
            }
        }
        val firmware = context.getSharedPreferences("firmware_dir_prefs", Context.MODE_PRIVATE)
        fun showFirmware(prod: Boolean) {
            content.findViewById<TextView>(R.id.tvFirmwareProd).alpha = if (prod) 1f else 0.45f
            content.findViewById<TextView>(R.id.tvFirmwareDev).alpha = if (prod) 0.45f else 1f
        }
        content.findViewById<Switch>(R.id.switchFirmwareDir).apply {
            isChecked = firmware.getString("firmware_dir", "prod") == "prod"
            showFirmware(isChecked)
            thumbTintList = ColorStateList.valueOf(0xff28aeb1.toInt())
            trackTintList = ColorStateList.valueOf(0xffbfe8e9.toInt())
            setOnCheckedChangeListener { _, checked ->
                firmware.edit().putString("firmware_dir", if (checked) "prod" else "dev").apply()
                showFirmware(checked)
            }
        }
        fun showWebResources(bundled: Boolean) {
            content.findViewById<TextView>(R.id.tvWebOffline).alpha = if (bundled) 1f else 0.45f
            content.findViewById<TextView>(R.id.tvWebOnline).alpha = if (bundled) 0.45f else 1f
        }
        content.findViewById<Switch>(R.id.switchWebResources).apply {
            isChecked = WebResourceSettings.isBundled(context)
            showWebResources(isChecked)
            thumbTintList = ColorStateList.valueOf(0xff28aeb1.toInt())
            trackTintList = ColorStateList.valueOf(0xffbfe8e9.toInt())
            setOnCheckedChangeListener { _, checked ->
                WebResourceSettings.setBundled(context, checked)
                showWebResources(checked)
            }
        }
        content.findViewById<TextView>(R.id.appIdentifyId).apply {
            text = (context.applicationContext as SesameApp).subscriptionManager.installationId
            setTextIsSelectable(true)
        }
        render()
    }

    override fun show() {
        super.show()
        ViewCompat.requestApplyInsets(content)
        window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.35f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setGravity(Gravity.BOTTOM)
            setLayout(-1, context.resources.displayMetrics.heightPixels - context.dp(40))
        }
    }

    private fun interval(device: CHDevices) = prefs.getInt("interval_${device.deviceId}", 1).coerceAtLeast(1)

    private fun render() {
        val running = job != null
        subtitle.text = "${selected?.let(ble::deviceName) ?: "未选择"}, ${selected?.let(::interval) ?: 1}s toggle"
        stats.text = "开关锁次数: $count 次"
        options.removeAllViews()
        locks.forEach { device ->
            val row = LayoutInflater.from(context).inflate(R.layout.item_lock_single_select, options, false)
            val chosen = selected === device
            val radio = row.findViewById<RadioButton>(R.id.rbSelected)
            radio.isChecked = chosen; radio.isEnabled = !running
            row.findViewById<TextView>(R.id.tvName).text = ble.deviceName(device)
            row.findViewById<TextView>(R.id.tvSec).apply {
                text = "${interval(device)}s"; visibility = if (chosen) View.VISIBLE else View.INVISIBLE
            }
            fun select() {
                if (!running) {
                    if (selected !== device) {
                        count = 0
                        prefs.edit().putInt("toggle_count", 0).apply()
                    }
                    selected = device; prefs.edit().putString("selected_id", device.deviceId.toString()).apply(); render()
                }
            }
            radio.setOnClickListener { select() }; row.setOnClickListener { select() }
            row.alpha = if (running) 0.6f else 1f
            listOf(R.id.btnMinus to -1, R.id.btnPlus to 1).forEach { (id, delta) ->
                row.findViewById<Button>(id).apply {
                    isEnabled = !running && chosen; alpha = if (isEnabled) 1f else 0.3f
                    setOnClickListener {
                        prefs.edit().putInt("interval_${device.deviceId}", (interval(device) + delta).coerceAtLeast(1)).apply(); render()
                    }
                }
            }
            options.addView(row)
        }
        start.isEnabled = !running; stop.isEnabled = running; reset.isEnabled = !running
        start.alpha = if (running) 0.5f else 1f
        stop.alpha = if (running) 1f else 0.5f
        reset.alpha = if (running) 0.5f else 1f
    }

    private class TestAction(
        val countOnStatusChange: Boolean,
        val run: (ByteArray?, () -> Unit) -> Unit
    )

    private fun testActionFor(device: CHDevices): TestAction? = when (device) {
        is CHSesame5 -> TestAction(countOnStatusChange = true) { historyTag, _ ->
            device.toggle(historytag = historyTag) {}
        }

        is CHSesameBot2 -> TestAction(countOnStatusChange = false) { historyTag, onSuccess ->
            device.click(historytag = historyTag) { result -> result.onSuccess { onSuccess() } }
        }

        else -> null
    }

    private fun recordToggle(device: CHDevices, sessionId: Int) {
        scope.launch(Dispatchers.Main.immediate) {
            if (job == null || testSessionId != sessionId || selected !== device) return@launch
            count++; prefs.edit().putInt("toggle_count", count).apply(); render()
        }
    }

    private fun startTest() {
        if (job != null) return
        val device = selected ?: return
        val action = testActionFor(device) ?: return
        val sessionId = ++testSessionId
        previous = device.delegate
        val originalDelegate = previous
        var last = device.deviceStatus
        val delegate = object : CHDeviceStatusDelegate by (previous ?: object : CHDeviceStatusDelegate {}) {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus) {
                originalDelegate?.onBleDeviceStatusChanged(device, status)
                scope.launch(Dispatchers.Main.immediate) {
                    if (action.countOnStatusChange && job != null && testSessionId == sessionId && selected === device && status != last) {
                        last = status
                        if (status == CHDeviceStatus.Locked || status == CHDeviceStatus.Unlocked) {
                            recordToggle(device, sessionId)
                        }
                    }
                }
            }
        }
        testDelegate = delegate; device.delegate = delegate
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        job = scope.launch {
            var next = SystemClock.elapsedRealtime()
            while (isActive) {
                next += interval(device) * 1000L
                action.run(ble.historyTag()) { recordToggle(device, sessionId) }
                val wait = next - SystemClock.elapsedRealtime()
                if (wait > 0) delay(wait.milliseconds) else yield()
            }
        }
        render()
    }

    private fun stopTest() {
        testSessionId += 1
        job?.cancel(); job = null
        selected?.let { if (it.delegate === testDelegate) it.delegate = previous }
        previous = null; testDelegate = null
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        render()
    }

    override fun dismiss() {
        stopTest(); super.dismiss()
    }
}
