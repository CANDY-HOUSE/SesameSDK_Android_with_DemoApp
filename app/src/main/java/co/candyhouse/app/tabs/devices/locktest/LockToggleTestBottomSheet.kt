package co.candyhouse.app.tabs.devices.locktest

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FragmentLockToggleTestSheetBinding
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame5
import co.utils.UserUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.UUID

/**
 * 开关锁强度测试
 *
 * @author frey on 2026/1/28
 */
class LockToggleTestBottomSheet : BottomSheetDialogFragment() {

    private val mDeviceModel: CHDeviceViewModel by activityViewModels()

    private var _binding: FragmentLockToggleTestSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LockAdapter

    private var selected: CHDevices? = null
    private var intervalSec: Int = 1
    private var toggleCount = 0

    private var isRunning = false
    private var toggleJob: Job? = null
    private var lastStatus: CHDeviceStatus? = null

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        const val TAG = "LockToggleTestDialog"

        private const val PREFS_NAME = "lock_toggle_test_prefs"
        private const val KEY_SELECTED_ID = "selected_id"
        private const val KEY_TOGGLE_COUNT = "toggle_count"
        private const val KEY_INTERVAL_PREFIX = "interval_"

        fun newInstance(): LockToggleTestBottomSheet = LockToggleTestBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockToggleTestSheetBinding.inflate(inflater, container, false)
        binding.root.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
        binding.root.clipToOutline = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LockAdapter(
            onSelect = ::onDeviceSelected,
            onPlus = ::onPlus,
            onMinus = ::onMinus
        )

        binding.rvLocks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLocks.adapter = adapter

        binding.btnClose.setOnClickListener {
            stopTest()
            dismiss()
        }
        binding.btnStart.setOnClickListener { startTest() }
        binding.btnStop.setOnClickListener { stopTest() }
        binding.btnReset.setOnClickListener { resetTest() }

        val locks = getAllSesame5Locks()
        adapter.submitList(locks)

        restoreFromPrefs(locks)

        adapter.setSelected(selected)
        selected?.let { intervalSec = adapter.getInterval(it) }

        refreshSubtitle()
        refreshStats()
        refreshButtonState()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener { dialogInterface ->
                val d = dialogInterface as BottomSheetDialog
                val sheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener
                sheet.setBackgroundColor(Color.TRANSPARENT)
                sheet.elevation = 0f
                sheet.clipToOutline = false

                val topGapPx = (40f * resources.displayMetrics.density).toInt()
                val screenHeight = resources.displayMetrics.heightPixels
                val targetHeight = screenHeight - topGapPx
                sheet.layoutParams = sheet.layoutParams.apply { height = targetHeight }
                sheet.requestLayout()

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.isDraggable = false
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                ViewCompat.setOnApplyWindowInsetsListener(sheet) { _, insets -> insets }
                ViewCompat.requestApplyInsets(sheet)
            }
        }
    }

    override fun getTheme(): Int = R.style.Theme_App_WifiScanBottomSheet

    override fun onDestroyView() {
        stopTest()
        super.onDestroyView()
        _binding = null
    }

    private fun onDeviceSelected(device: CHDevices) {
        if (isRunning) return
        selected = device

        intervalSec = adapter.getInterval(device)
        adapter.setSelected(device)

        persistSelected(device.deviceId!!)
        refreshSubtitle()
    }

    private fun onPlus() {
        if (isRunning) return
        val sel = selected ?: return
        intervalSec = adapter.getInterval(sel) + 1
        adapter.setIntervalForSelected(intervalSec)
        persistInterval(sel.deviceId!!, intervalSec)
        refreshSubtitle()
    }

    private fun onMinus() {
        if (isRunning) return
        val sel = selected ?: return
        intervalSec = (adapter.getInterval(sel) - 1).coerceAtLeast(1)
        adapter.setIntervalForSelected(intervalSec)
        persistInterval(sel.deviceId!!, intervalSec)
        refreshSubtitle()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshSubtitle() {
        val name = selected?.let { (it as? CHSesame5)?.getNickname() } ?: "未选择"
        safeUi {
            binding.tvSubtitle.text = "$name, ${intervalSec}s toggle"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshStats() {
        safeUi {
            binding.tvStats.text = "开关锁次数: $toggleCount 次"
        }
    }

    private fun refreshButtonState() {
        val running = isRunning
        safeUi {
            binding.btnStart.isEnabled = !running
            binding.btnStart.alpha = if (running) 0.5f else 1f

            binding.btnStop.isEnabled = running
            binding.btnStop.alpha = if (running) 1f else 0.5f

            binding.btnReset.isEnabled = !running
            binding.btnReset.alpha = if (running) 0.5f else 1f
        }
        adapter.isRunning = isRunning
    }

    private fun getAllSesame5Locks(): List<CHDevices> {
        val all: List<CHDevices> = mDeviceModel.myChDevices.value
        return all.filter { it is CHSesame5 }
    }

    private fun startTest() {
        if (isRunning) return
        val device = selected as? CHSesame5 ?: return

        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        isRunning = true
        refreshButtonState()

        intervalSec = adapter.getInterval(device)
        refreshSubtitle()

        lastStatus = device.deviceStatus

        device.delegate = object : CHDeviceStatusDelegate {
            override fun onBleDeviceStatusChanged(
                device: CHDevices,
                status: CHDeviceStatus,
                shadowStatus: CHDeviceStatus?
            ) {
                if (!isRunning) return
                if (status == lastStatus) return
                lastStatus = status

                if (status == CHDeviceStatus.Locked || status == CHDeviceStatus.Unlocked) {
                    toggleCount += 1
                    persistToggleCount(toggleCount)
                    safeUi { refreshStats() }
                }
            }
        }

        toggleJob?.cancel()

        toggleJob = viewLifecycleOwner.lifecycleScope.launch {
            val intervalMs = intervalSec * 1000L
            var next = SystemClock.elapsedRealtime()
            while (isActive && isRunning) {
                next += intervalMs
                device.toggle(historytag = UserUtils.getUserIdWithByte()) {}
                val delayMs = next - SystemClock.elapsedRealtime()
                if (delayMs > 0) delay(delayMs) else yield()
            }
        }
    }

    private fun stopTest() {
        if (!isRunning) return

        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        isRunning = false

        toggleJob?.cancel()
        toggleJob = null

        (selected as? CHSesame5)?.delegate = null
        lastStatus = null

        refreshButtonState()
    }

    private fun resetTest() {
        if (isRunning) return

        val locks = getAllSesame5Locks()

        intervalSec = 1
        toggleCount = 0
        selected = locks.firstOrNull()
        adapter.resetAll()
        adapter.setSelected(selected)

        prefs.edit {
            remove(KEY_SELECTED_ID)
            putInt(KEY_TOGGLE_COUNT, 0)

            locks.forEach { d ->
                remove(KEY_INTERVAL_PREFIX + d.deviceId.toString())
            }
        }

        refreshSubtitle()
        refreshStats()
        refreshButtonState()
    }

    private fun restoreFromPrefs(locks: List<CHDevices>) {
        toggleCount = prefs.getInt(KEY_TOGGLE_COUNT, 0)

        val selectedIdStr = prefs.getString(KEY_SELECTED_ID, null)
        val restoredSelected = selectedIdStr?.let { idStr ->
            locks.firstOrNull { it.deviceId.toString() == idStr }
        }
        selected = restoredSelected ?: locks.firstOrNull()

        locks.forEach { d ->
            val sec = prefs.getInt(KEY_INTERVAL_PREFIX + d.deviceId.toString(), 1)
            adapter.setInterval(d.deviceId!!, sec)
        }

        selected?.let { intervalSec = adapter.getInterval(it) }
    }

    private fun persistSelected(id: UUID) {
        prefs.edit { putString(KEY_SELECTED_ID, id.toString()) }
    }

    private fun persistInterval(id: UUID, sec: Int) {
        prefs.edit { putInt(KEY_INTERVAL_PREFIX + id.toString(), sec) }
    }

    private fun persistToggleCount(count: Int) {
        prefs.edit { putInt(KEY_TOGGLE_COUNT, count) }
    }

    private fun safeUi(block: () -> Unit) {
        if (!isAdded || _binding == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
                if (!isAdded || _binding == null) return@launch
                block()
            }
        }
    }

    private class LockAdapter(
        val onSelect: (CHDevices) -> Unit,
        val onPlus: () -> Unit,
        val onMinus: () -> Unit
    ) : ListAdapter<CHDevices, LockVH>(Diff) {

        private var selectedId: UUID? = null
        private val intervalMap = mutableMapOf<UUID, Int>()

        var isRunning: Boolean = false
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        @SuppressLint("NotifyDataSetChanged")
        fun setSelected(d: CHDevices?) {
            selectedId = d?.deviceId
            notifyDataSetChanged()
        }

        fun getInterval(device: CHDevices?): Int {
            return device?.deviceId?.let { intervalMap[it] } ?: 1
        }

        fun setIntervalForSelected(sec: Int) {
            selectedId?.let { id ->
                intervalMap[id] = sec
                val pos = currentList.indexOfFirst { it.deviceId == id }
                if (pos >= 0) notifyItemChanged(pos)
            }
        }

        fun setInterval(id: UUID, sec: Int) {
            intervalMap[id] = sec
        }

        @SuppressLint("NotifyDataSetChanged")
        fun resetAll() {
            intervalMap.clear()
            selectedId = null
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lock_single_select, parent, false)
            return LockVH(v)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: LockVH, position: Int) {
            val item = getItem(position)
            val isSelected = (item.deviceId == selectedId)
            val isRunning = this.isRunning
            val interval = intervalMap[item.deviceId] ?: 1

            holder.tvName.text = (item as? CHSesame5)?.getNickname() ?: "Unknown"

            holder.rbSelected.setOnCheckedChangeListener(null)
            holder.rbSelected.isChecked = isSelected

            holder.itemRoot.isEnabled = !isRunning
            holder.rbSelected.isEnabled = !isRunning
            holder.itemRoot.isClickable = !isRunning
            holder.rbSelected.isClickable = !isRunning
            holder.itemRoot.alpha = if (isRunning) 0.6f else 1f

            holder.tvSec.text = if (isSelected) "${interval}s" else ""

            holder.itemRoot.setOnClickListener { if (!isRunning) onSelect(item) }
            holder.rbSelected.setOnClickListener { if (!isRunning) onSelect(item) }

            val showControls = isSelected && !isRunning
            holder.btnPlus.isEnabled = showControls
            holder.btnMinus.isEnabled = showControls
            holder.btnPlus.alpha = if (showControls) 1f else 0.3f
            holder.btnMinus.alpha = if (showControls) 1f else 0.3f
            holder.tvSec.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            holder.btnPlus.setOnClickListener { if (showControls) onPlus() }
            holder.btnMinus.setOnClickListener { if (showControls) onMinus() }
        }

        private object Diff : DiffUtil.ItemCallback<CHDevices>() {
            override fun areItemsTheSame(oldItem: CHDevices, newItem: CHDevices) =
                oldItem.deviceId == newItem.deviceId

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: CHDevices, newItem: CHDevices) = true
        }
    }

    private class LockVH(v: View) : RecyclerView.ViewHolder(v) {
        val itemRoot: View = v.findViewById(R.id.itemRoot)
        val rbSelected: RadioButton = v.findViewById(R.id.rbSelected)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val btnMinus: Button = v.findViewById(R.id.btnMinus)
        val tvSec: TextView = v.findViewById(R.id.tvSec)
        val btnPlus: Button = v.findViewById(R.id.btnPlus)
    }
}