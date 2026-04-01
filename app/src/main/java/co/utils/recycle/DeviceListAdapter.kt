package co.utils.recycle

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.databinding.SesameDevicesListLayoutBinding
import co.candyhouse.app.ext.BotScriptStore
import co.candyhouse.app.ext.userKey
import co.candyhouse.app.tabs.devices.hub3.recycle.Hub3ItemView
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.createOpensensorStateText
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getRank
import co.candyhouse.app.tabs.devices.ssm2.setRank
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.CHHub3
import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
import co.candyhouse.sesame.open.devices.CHSesameBike
import co.candyhouse.sesame.open.devices.CHSesameBike2
import co.candyhouse.sesame.open.devices.CHSesameBiometricDevice
import co.candyhouse.sesame.open.devices.CHSesameBot
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.CHWifiModule2
import co.candyhouse.sesame.open.devices.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.open.devices.base.CHSesameConnector
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHSesameOpenSensorMechStatus
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.OpenSensorData
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.BotScriptItem
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.server.dto.IrRemote
import co.candyhouse.sesame.server.dto.cheyKeyToUserKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceListAdapter(
    private val mDeviceViewModel: CHDeviceViewModel,
    private val onDeviceClick: (CHDevices) -> Unit,
    private val callBackHub3: (CHHub3, IrRemote) -> Unit,
) : GenericAdapter<CHDevices>(mDeviceViewModel.myChDevices.value) {

    override fun onItemMoveFinished() {
        super.onItemMoveFinished()

        if (isUploading) return
        isUploading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                mDeviceViewModel.myChDevices.value.forEachIndexed { index, chDevices ->
                    chDevices.setRank(-index)
                }
                CHAPIClientBiz.upLoadKeys(mDeviceViewModel.myChDevices.value.map {
                    cheyKeyToUserKey(it.getKey(), it.getLevel(), it.getNickname(), it.getRank())
                }) { isUploading = false }
            } catch (e: Exception) {
                isUploading = false
                e.printStackTrace()
            }
        }
    }

    override fun getLayoutId(position: Int, obj: CHDevices): Int {
        return R.layout.sesame_devices_list_layout
    }

    override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
        return SesameViewHolder(view)
    }

    companion object {
        @Volatile
        private var isUploading = false
        private val expandStates = mutableMapOf<String, Boolean>()

        fun getExpandState(deviceId: String): Boolean = expandStates[deviceId] ?: false
        fun setExpandState(deviceId: String, state: Boolean) {
            expandStates[deviceId] = state
        }

        var CHDevices.expanded: Boolean
            get() = getExpandState(this.deviceId.toString())
            set(value) = setExpandState(this.deviceId.toString(), value)

        fun CHDevices.setExpandState(yesOrNo: Boolean) {
            expanded = yesOrNo
        }
    }

    inner class SesameViewHolder(val view: View) : RecyclerView.ViewHolder(view), Binder<CHDevices> {
        private val binding = SesameDevicesListLayoutBinding.bind(view)

        private var bot2Adapter: Bot2ItemView? = null
        private var bot2ItemTouchHelper: ItemTouchHelper? = null

        override fun bind(device: CHDevices, pos: Int) {
            bindDevice(device)
            view.setOnClickListener { onDeviceClick(device) }
        }

        private fun bindDevice(device: CHDevices) {
            resetViews()

            binding.nickName.text = device.getNickname()

            when (device) {
                is CHHub3 -> configureHub3(device)
                is CHWifiModule2 -> configureWifiModule2(device)
                is CHSesame5, is CHSesame2 -> configureLock(device)
                else -> configureDefault(device)
            }
        }

        private fun resetViews() {
            binding.apply {
                bleStatus.visibility = View.GONE
                expandFL.visibility = View.GONE
                expandFLSubView.visibility = View.GONE
                ssmLockView.visibility = View.GONE
                ssmBikeBotView.visibility = View.GONE
                openSensorStatus.visibility = View.GONE
            }
        }

        private fun configureHub3(device: CHHub3) {
            binding.apply {
                blImg.visibility = View.GONE
                updateWifiStatus(device)

                setBatteryStatus(device.userKey?.stateInfo?.batteryPercentage)

                setupExpandableView(device, 35) {
                    val irRemoteList = device.userKey?.stateInfo?.remoteList ?: emptyList()
                    Hub3ItemView(irRemoteList) { bot2Item ->
                        callBackHub3(device, bot2Item)
                    }
                }
            }
        }

        private fun configureWifiModule2(device: CHWifiModule2) {
            binding.apply {
                blImg.visibility = View.GONE
                updateWifiStatus(device)
                batteryContain.visibility = View.GONE
                batteryPercent.visibility = View.GONE
            }
        }

        private fun configureLock(device: CHDevices) {
            binding.apply {
                updateConnectionStatus(device)

                updateBatteryStatus(device)

                ssmLockView.visibility = View.VISIBLE
                ssmLockView.setLockImage(device)
                ssmLockView.setOnClickListener {
                    CHDeviceManager.vibrateDevice(view)
                    when (device) {
                        is CHSesame5 -> device.toggle(historytag = UserUtils.getUserIdWithByte()) {
                            it.onSuccess { }
                        }

                        is CHSesame2 -> device.toggle() { it.onSuccess { } }
                    }
                }

                updateBleStatusVisibility(device)
            }
        }

        private fun configureDefault(device: CHDevices) {
            binding.apply {
                if (device.getLevel() == 2) {
                    device.deviceShadowStatus = null
                }

                updateConnectionStatus(device)

                updateBatteryStatus(device)

                updateBleStatusVisibility(device)

                if (device is CHSesameBot2 && (device.productModel == CHProductModel.SesameBot2 || device.productModel == CHProductModel.SesameBot3)) {
                    val newList = getScriptList(device)

                    if (bot2Adapter == null) {
                        bot2Adapter = Bot2ItemView(
                            newList.toMutableList(),
                            callback = { bot2Item ->
                                device.click(bot2Item.id.toUByte(), UserUtils.getUserIdWithByte()) {}
                            },
                            onOrderChanged = { changedList ->
                                uploadBotScriptDisplayOrder(device, changedList)
                            }
                        )
                    } else {
                        bot2Adapter?.updateData(newList)
                    }

                    setupExpandableView(device, 90) {
                        bot2Adapter!!
                    }

                    if (bot2ItemTouchHelper == null) {
                        bot2ItemTouchHelper = ItemTouchHelper(Bot2ItemTouchHelperCallback(bot2Adapter!!))
                        binding.expandFLSubView.post {
                            bot2ItemTouchHelper?.attachToRecyclerView(binding.expandFLSubView)
                        }
                    }
                }

                if (device is CHSesameBiometricDevice) {
                    handleBiometricDevice(device)
                } else {
                    ssmBikeBotView.visibility = View.VISIBLE
                    ssmBikeBotView.setLockImage(device)
                    ssmBikeBotView.setOnClickListener {
                        CHDeviceManager.vibrateDevice(view)
                        handleBikeBotViewClick(device)
                    }
                }
            }
        }

        private fun updateWifiStatus(device: CHDevices) {
            val isWifiConnect = (device.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork ?: false

            binding.wifiImg.setImageResource(
                if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey
            )
        }

        private fun updateConnectionStatus(device: CHDevices) {
            binding.apply {
                blImg.visibility = View.VISIBLE
                val isBleConnect = device.deviceStatus.value == CHDeviceLoginStatus.logined
                blImg.setImageResource(
                    if (isBleConnect) R.drawable.bl_green else R.drawable.bl_grey
                )

                val isWifiConnect = device.deviceShadowStatus?.let { it.value == CHDeviceLoginStatus.logined } ?: false
                wifiImg.setImageResource(
                    if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey
                )
            }
        }

        private fun updateBatteryStatus(device: CHDevices) {
            val batteryLevel = device.batteryPercentage ?: device.userKey?.stateInfo?.batteryPercentage
            setBatteryStatus(batteryLevel)
        }

        private fun setBatteryStatus(batteryLevel: Int?) {
            binding.apply {
                if (batteryLevel == null) {
                    batteryContain.isVisible = false
                    batteryPercent.isVisible = false
                    return
                }

                batteryContain.isVisible = true
                batteryPercent.isVisible = true
                batteryPercent.text = "$batteryLevel%"
                btnPecent.apply {
                    progressDrawable = ContextCompat.getDrawable(
                        itemView.context,
                        when {
                            batteryLevel >= 15 -> R.drawable.progress_blue
                            else -> R.drawable.progress_red
                        }
                    )
                    progress = batteryLevel
                }
            }
        }

        private fun updateBleStatusVisibility(device: CHDevices) {
            binding.bleStatus.text = device.deviceStatus.toString()
            binding.bleStatus.visibility = when {
                device is CHSesameConnector -> {
                    if (device.deviceStatus.value == CHDeviceLoginStatus.logined ||
                        device.deviceStatus == CHDeviceStatus.NoBleSignal ||
                        device.deviceStatus == CHDeviceStatus.ReceivedAdV
                    ) View.GONE else View.VISIBLE
                }

                else -> {
                    if (device.deviceStatus.value == CHDeviceLoginStatus.logined ||
                        device.deviceStatus == CHDeviceStatus.NoBleSignal
                    ) View.GONE else View.VISIBLE
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setupExpandableView(
            device: CHDevices,
            heightDp: Int,
            adapterProvider: () -> RecyclerView.Adapter<*>
        ) {
            binding.apply {
                val adapter = adapterProvider()
                if (expandFLSubView.adapter !== adapter) {
                    expandFLSubView.adapter = adapter
                }
                expandFLSubView.setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                expandFL.visibility = View.VISIBLE
                expandFL.layoutParams.height = (heightDp * expandFL.resources.displayMetrics.density).toInt()
                dispatchExpanded(device)
                expandFL.setOnClickListener {
                    device.setExpandState(!device.expanded)
                    dispatchExpanded(device)
                }
            }
        }

        private fun handleBikeBotViewClick(device: CHDevices) {
            when (device) {
                is CHSesameBike -> device.unlock { it.onSuccess { } }
                is CHSesameBike2 -> device.unlock(historytag = UserUtils.getUserIdWithByte()) { it.onSuccess { } }
                is CHSesameBot -> device.click { it.onSuccess { } }
                is CHSesameBot2 -> {
                    val bot2ScriptCurIndexKey = "${device.deviceId}_ScriptIndex"
                    val index = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
                    device.click(index.toUByte(), UserUtils.getUserIdWithByte()) {}
                }
            }
        }

        private fun handleBiometricDevice(device: CHSesameBiometricDevice) {
            binding.apply {
                blImg.visibility = View.GONE
                ssmBikeBotView.visibility = View.GONE

                if (device.productModel == CHProductModel.SSMOpenSensor || device.productModel == CHProductModel.SSMOpenSensor2) {
                    val statusText = (device.mechStatus as? CHSesameOpenSensorMechStatus)?.data?.let { data ->
                        runCatching {
                            val sensorData = OpenSensorData.fromByteArray(data)
                            createOpensensorStateText(sensorData.Status, sensorData.TimeStamp)
                        }.getOrNull()
                    } ?: device.userKey?.stateInfo?.let { info ->
                        val status = info.CHSesame2Status
                        val timestamp = info.timestamp
                        if (status != null && timestamp != null) {
                            createOpensensorStateText(status, timestamp)
                        } else {
                            null
                        }
                    }

                    statusText?.let {
                        openSensorStatus.text = it
                        openSensorStatus.visibility = View.VISIBLE
                    }
                } else {
                    openSensorStatus.visibility = View.GONE
                }
            }
        }

        private fun getScriptList(data: CHSesameBot2): MutableList<BotItem> {
            val deviceUUID = data.deviceId.toString()

            if (data.scripts.events.isNotEmpty()) {
                return data.scripts.events.mapIndexed { index, event ->
                    val fallback = String(event.name, Charsets.UTF_8)
                    val alias = BotScriptStore.getAlias(deviceUUID, index)
                    val displayName = alias ?: fallback
                    val order = BotScriptStore.getDisplayOrder(deviceUUID, index) ?: index

                    BotItem(
                        name = displayName,
                        id = index,
                        displayOrder = order
                    )
                }.sortedBy { it.displayOrder }
                    .toMutableList()
            }

            val remoteScriptList = data.userKey?.stateInfo?.scriptList.orEmpty()
            if (remoteScriptList.isNotEmpty()) {
                return remoteScriptList.mapNotNull { item ->
                    val index = item.actionIndex.toIntOrNull() ?: return@mapNotNull null
                    val displayName = item.alias ?: "🎬 $index"
                    val order = item.displayOrder ?: index

                    BotItem(
                        name = displayName,
                        id = index,
                        displayOrder = order
                    )
                }.sortedBy { it.displayOrder }
                    .toMutableList()
            }

            val localFallback = (0..9).mapNotNull { index ->
                val alias = BotScriptStore.getAlias(deviceUUID, index) ?: return@mapNotNull null
                val order = BotScriptStore.getDisplayOrder(deviceUUID, index) ?: index

                BotItem(
                    name = alias,
                    id = index,
                    displayOrder = order
                )
            }

            return localFallback.sortedBy { it.displayOrder }.toMutableList()
        }

        private fun dispatchExpanded(device: CHDevices) {
            binding.apply {
                imageArrow.rotation = if (device.expanded) 90f else 0f
                expandFLSubView.visibility = if (device.expanded) View.VISIBLE else View.GONE
            }
        }

        private fun uploadBotScriptDisplayOrder(device: CHSesameBot2, list: List<BotItem>) {
            val deviceUUID = device.deviceId.toString()

            val metaMap = list.mapIndexed { order, item ->
                item.id to BotScriptStore.ScriptMeta(
                    alias = BotScriptStore.getAlias(deviceUUID, item.id),
                    displayOrder = order
                )
            }.toMap()
            BotScriptStore.merge(deviceUUID, metaMap)

            val batchOrders = list.mapIndexed { order, item ->
                BotScriptItem(
                    actionIndex = item.id.toString(),
                    displayOrder = order
                )
            }

            val req = BotScriptRequest(
                deviceUUID = deviceUUID.uppercase(),
                batchDisplayOrders = batchOrders
            )

            CHAPIClientBiz.updateBotScript(req) { result ->
                result.onSuccess {
                    L.d("DeviceListAdapter", "batch updateBotScript success")
                }
                result.onFailure {
                    L.e("DeviceListAdapter", "batch updateBotScript failed", it)
                }
            }
        }
    }
}

class Bot2ItemTouchHelperCallback(
    private val adapter: Bot2ItemView
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
        adapter.onItemMove(from, to)
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        adapter.onDragFinished()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}