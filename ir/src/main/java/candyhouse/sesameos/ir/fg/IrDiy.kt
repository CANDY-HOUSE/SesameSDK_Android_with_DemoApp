package candyhouse.sesameos.ir.fg

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.IrGridAdapter
import candyhouse.sesameos.ir.base.BaseIr
import candyhouse.sesameos.ir.base.CHHub3IRCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.databinding.FgIrDiyBinding
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import candyhouse.sesameos.ir.server.CHIRAPIManager
import candyhouse.sesameos.ir.viewModel.IRDeviceViewModel
import candyhouse.sesameos.ir.widget.AlertStyle
import candyhouse.sesameos.ir.widget.AlertView
import candyhouse.sesameos.ir.widget.dialog.AlertAction
import candyhouse.sesameos.ir.widget.dialog.AlertActionStyle
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.utils.L
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class IrDiy : IrBaseFG<FgIrDiyBinding>() {
    private val gTag = "IrDiy"
    val viewModel: IRDeviceViewModel by activityViewModels()
    override fun getViewBinder() = FgIrDiyBinding.inflate(layoutInflater)
    val listCodes = mutableListOf<CHHub3IRCode>()

    var isNewDevice = false
    var editable = true
    lateinit var adapter: IrGridAdapter
    lateinit var chHub3Delegate: CHHub3Delegate
    var isRegisterMode = false
    val MODEL_REGISTER = 0x01 // 自学习模式
    val MODEL_CONTROL = 0x00  // 控制模式

    private var shouldContinue = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(Config.isNewDevice)) {
                isNewDevice = it.getBoolean(Config.isNewDevice, false)
            }
            if (it.containsKey(Config.editable)) {
                editable = it.getBoolean(Config.editable, true)
            }
        }
        val defaultName = getString(R.string.ir_study)
        val irRemote: IrRemote =
            arguments?.getParcelableCompat<IrRemote>(Config.irDevice)
                ?: IrRemote(
                    model = "",
                    alias = defaultName,
                    uuid = UUID.randomUUID().toString().uppercase(),
                    state = "",
                    timestamp = 0L,
                    type = IRType.DEVICE_REMOTE_CUSTOM,
                    code = 0,
                    direction = ""
                )
        viewModel.setRemoteDevice(irRemote)
        // 来自firebase crash：BaseIr.hub3Device.value有空的情况，做安全处理.
        try {
            viewModel.setCHHub3(BaseIr.hub3Device.value!!)
        } catch (e: NullPointerException) {
            // 处理异常信息
            handleHub3DeviceValueException(e)
            // 回退
            parentFragmentManager.popBackStack()
            return
        }
    }

    private fun handleHub3DeviceValueException(e: NullPointerException) {
        L.d("sf", "BaseIr.hub3Device.value is null. Must return!")

        shouldContinue = false

        val previousLabel = findNavController().previousBackStackEntry?.destination?.label
        L.d("sf", "导航来自：${previousLabel ?: "IrListProductFg"}")

        FirebaseCrashlytics.getInstance().apply {
            log("自学习界面初始化：BaseIr.hub3Device.value is null.")
            log("来自：$previousLabel")
            recordException(e)
        }

        Toast.makeText(
            context,
            "Unexpected error. Back to home, pull to refresh",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!shouldContinue) return
        setupTitleView()
        setupIrCodeViews()
        setupHub3Delegate()
        L.d(
            gTag,
            "device status=" + viewModel.getCHHub3().deviceStatus.name + " / " + viewModel.getCHHub3().deviceStatus.value + "  isNewDevice" + (isNewDevice)
        )
        showLoadingView()
        getIrCodes()
        getIrModel()
        observeUiState()
        innitDevice()
    }

    private fun getIrModel() {
        viewModel.getCHHub3().irModeGet {
            it.onSuccess {
                L.d("hub3", "[UI][getMode][OK]")
                view?.post {
                    if (it.data.toInt() == MODEL_CONTROL) { // IR_MODE_CONTROL
                        bind.topTitle.imgRight.isSelected = false
                        isRegisterMode = false
                    } else {    // IR_MODE_LEARN
                        bind.topTitle.imgRight.isSelected = true
                        isRegisterMode = true
                    }
                }
            }
        }
    }

    private fun setupTitleView() {
        if (editable) {
            tvTitleOnclick(viewModel.irRemoteDeviceLiveData.value!!.alias) {
                showCustomDialog(
                    getString(R.string.ir_set_controller_name),
                    viewModel.irRemoteDeviceLiveData.value!!.alias,
                    getString(R.string.ir_edit_limit)
                ) { value ->
                    showOverLayLoadingView()
                    viewModel.modifyIrRemoteInfo(
                        viewModel.getCHHub3().deviceId.toString().uppercase(),
                        alias = value,
                        onSuccess = {
                            showContentView()
                        },
                        onError = {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.ir_change_fail),
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                }
            }
        }
        bind.topTitle.imgRight.visibility = if (editable) View.VISIBLE else View.GONE
        bind.topTitle.imgRight.setImageResource(R.drawable.selector_img_add_del)
        bind.topTitle.imgRight.setOnClickListener { mView ->
            if (!isBluetoothEnabled(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.ir_ble_noable),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (viewModel.getCHHub3().deviceStatus.value == CHDeviceLoginStatus.Login) {
                switchModel(mView.isSelected)
                return@setOnClickListener
            }
            Toast.makeText(
                requireContext(),
                getString(R.string.ir_ble_invalid),
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    private fun setupIrCodeViews() {
        adapter = IrGridAdapter(
            requireContext(),
            listCodes, editable, onClickItem = { item ->
                emitIrCode(item)
                handleActionOnIoThread {
                    viewModel.getCHHub3().getIrLearnedData() { it ->
                        it.onSuccess {
                            L.d("getIrLearnedData", "[IO][getLearnedData][OK]")
                            L.d("getIrLearnedData", "[hub3][deviceUUID] " + viewModel.getCHHub3().deviceId)
                            L.d("getIrLearnedData", "[irCode][UUID] " + item.irID)
                            val irDataNameUUID = UUID.randomUUID().toString()
                            // 旧的数据， 发射一次后， 固件会把数据发到云端， APP接收后， 生成 UUID ， 存到云端， 以后从云端发数据， 而不是发 id。
                            CHIRAPIManager.addHub3LearnedIrData(it.data, viewModel.getCHHub3().deviceId.toString().uppercase(), irDataNameUUID, item.uuid, item.irID)
                            // this.chHub3Delegate.onIRCodeChanged(viewModel.getCHHub3(), id, item.name)
                        }
                    }
                }
            },
            onLongClickItem = { position, item ->
                if (editable) {
                    showLongClick(position, item)
                }
            })
        bind.ryView.adapter = adapter
        if (!editable) {
            bind.textViewEmpty.text = getString(R.string.ir_no_code)
        }
    }

    private fun emitIrCode(item: CHHub3IRCode) {
        viewModel.emitIrCode(
            viewModel.getCHHub3().deviceId.toString().uppercase(),
            item.irID,
            viewModel.irRemoteDeviceLiveData.value!!.uuid,
            onSuccess = {
                handleActionOnMainThread {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.ir_send_success), Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onError = { message ->
                handleActionOnMainThread {
                    Toast.makeText(
                        requireContext(),
                        message, Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    fun switchModel(isClose: Boolean) {
        if (isClose) {
            isRegisterMode = false
            viewModel.getCHHub3().unsubscribeLearnData()
            viewModel.getCHHub3().irModeSet(MODEL_CONTROL.toByte()) {
                it.onSuccess {
                    handleActionOnMainThread {
                        bind.topTitle.imgRight.isSelected = false
                    }
                }

            }
        } else {
            isRegisterMode = true
            viewModel.getCHHub3().irModeSet(MODEL_REGISTER.toByte()) { it ->
                it.onSuccess {
                    handleActionOnMainThread {
                        bind.topTitle.imgRight.isSelected = true
                    }
                    handleActionOnIoThread {
                        viewModel.getCHHub3().getIrLearnedData() { it ->
                            it.onSuccess {
                                L.d("getIrLearnedData", "[IO][getLearnedData][OK]")
                                L.d("getIrLearnedData", "[hub3][deviceUUID] " + viewModel.getCHHub3().deviceId)
                                val irDataNameUUID = UUID.randomUUID().toString() // 新学习的 UUID 格式的红外码的数据， 直接上云。 固件不再存到 Flash 里。
                                CHIRAPIManager.addHub3LearnedIrData(it.data, viewModel.getCHHub3().deviceId.toString().uppercase(), irDataNameUUID, viewModel.irRemoteDeviceLiveData.value!!.uuid, irDataNameUUID)
                                this.chHub3Delegate.onIRCodeChanged(viewModel.getCHHub3(), irDataNameUUID, "")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showContentView() {
        L.d(gTag, "showContentView")
        handleActionOnMainThread {
            bind.progressG.visibility = View.GONE
            if (bind.ryView.visibility != View.VISIBLE) {
                bind.ryView.visibility = View.VISIBLE
            }
            bind.textViewEmpty.visibility =
                if (listCodes.isEmpty()) View.VISIBLE else View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.GONE
        }
    }

    private fun showLoadingView() {
        L.d(gTag, "showLoadingView")
        handleActionOnMainThread {
            bind.textViewEmpty.visibility = View.GONE
            bind.ryView.visibility = View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.GONE
            bind.progressG.visibility = View.VISIBLE
        }
    }

    private fun showOverLayLoadingView() {
        L.d(gTag, "showOverLayLoadingView")
        handleActionOnMainThread {
            bind.textViewEmpty.visibility = View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.GONE
            bind.progressG.visibility = View.VISIBLE
        }
    }

    private fun showErrorView(onclick: () -> Unit = {}) {
        L.d(gTag, "showErrorView")
        handleActionOnMainThread {
            bind.progressG.visibility = View.GONE
            bind.ryView.visibility = View.GONE
            bind.textViewEmpty.visibility = View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.VISIBLE
            bind.linearLayoutNetworkRetry.setOnClickListener {
                onclick()
            }
        }
    }

    private fun setupHub3Delegate() {
        chHub3Delegate = object : CHHub3Delegate {
            override fun onBleDeviceStatusChanged(
                device: CHDevices,
                status: CHDeviceStatus,
                shadowStatus: CHDeviceStatus?
            ) {
                L.d(
                    gTag,
                    "onBleDeviceStatusChanged status=${status.value} / ${status.name}"
                )
                if (status.value == CHDeviceLoginStatus.Login) {
                    viewModel.getCHHub3().getIRCodes {}
                }
            }

            override fun onIRCodeChanged(
                device: CHHub3,
                id: String,
                name: String
            ) { // 从设备端接收到新增的红外码，直接进入正式队列
                val irCode = CHHub3IRCode(
                    id,
                    name,
                    uuid = viewModel.irRemoteDeviceLiveData.value!!.uuid.uppercase(),
                    deviceId = viewModel.getCHHub3().deviceId.toString().uppercase()
                )
                addIrCode(irCode)
                postIrCode(irCode)
                L.d(gTag, "onIRCodeChanged id=" + id + " name=" + name)
                switchModel(true)
                showContentView()
            }

            override fun onIRCodeReceive(
                device: CHHub3,
                id: String,
                name: String
            ) { // 从设备端接收到列表的红外码，先进入临时队列
                L.d(gTag, "onIRCodeReceive id:" + id + " _--name:" + name)
            }

            override fun onIRModeReceive(device: CHHub3, mode: Int) {
                L.d(gTag, "onIRModeReceive: $mode")
                bind.topTitle.imgRight.isSelected = mode != MODEL_CONTROL
                isRegisterMode = mode != MODEL_CONTROL
            }

            override fun onIRCodeReceiveStart(device: CHHub3) {
                L.d(gTag, "onIRCodeReceiveStart")
            }

            override fun onIRCodeReceiveEnd(device: CHHub3) {
                L.d(gTag, "onIRCodeReceiveEnd")
            }
        }
        viewModel.getCHHub3().multicastDelegate.addDelegate(chHub3Delegate)
    }

    private fun postIrCode(irCode: CHHub3IRCode) {
        viewModel.addIRDeviceInfo(
            viewModel.getCHHub3(),
            listCodes,
            onSuccess = {},
            onError = {})
    }

    private fun getIrCodes() {
        viewModel.getIrCodeListInfo(
            onSuccess = {
                showContentView()
            }, onError = {
                showErrorView({
                    getIrCodes()
                })
            })
    }


    private fun observeUiState() {

        viewModel.irRemoteDeviceLiveData.observe(viewLifecycleOwner) { irRomteDevice ->
            handleActionOnMainThread {
                setTitle(irRomteDevice.alias)
            }

        }
        viewModel.irCodeLiveData.observe(viewLifecycleOwner) {
            handleActionOnMainThread {
                if (viewModel.getCHHub3().deviceStatus.value != CHDeviceLoginStatus.Login) {


                }
                L.d(gTag, "listCodes: ${listCodes.toString()}")

                listCodes.clear()
                listCodes.addAll(it)
                adapter.notifyDataSetChanged()
            }
        }
    }


    private fun innitDevice() {
        if (isNewDevice) {
            L.d(gTag, "innitDevice isNewDevice = $isNewDevice")
            showLoadingView()
            viewModel.addIRDeviceInfo(
                viewModel.getCHHub3(), mutableListOf(),
                onSuccess = {
                    showContentView()
                },
                onError = {
                    showErrorView()
                })
        }
    }

    private fun addIrCode(baseIrCode: CHHub3IRCode?, isRefresh: Boolean = true) {
        handleActionOnMainThread {
            if (baseIrCode != null) {
                val isFind = listCodes.any { it.irID == baseIrCode.irID }
                if (isFind) return@handleActionOnMainThread
                listCodes.add(baseIrCode)
                if (isRefresh) {
                    viewModel.matchIrCode(
                        listCodes,
                        irRemoteId = viewModel.irRemoteDeviceLiveData.value!!.uuid.uppercase(),
                        deviceId = viewModel.getCHHub3().deviceId.toString().uppercase()
                    )
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showLongClick(position: Int, item: CHHub3IRCode) {
        AlertView("", "", AlertStyle.IOS).apply {
            val actionTitle: String = getString(R.string.ir_modify_key_name)
            val dialogTips: String = getString(R.string.ir_edit_limit)
            addAction(AlertAction(actionTitle, AlertActionStyle.DEFAULT) { action ->
                showCustomDialog(actionTitle, item.name, dialogTips) { name ->
                    changeIrCode(position, item, name)
                }
            })
            addAction(
                AlertAction(
                    getString(R.string.ir_delete),
                    AlertActionStyle.NEGATIVE
                ) { action ->
                    showOverLayLoadingView()
                    viewModel.deleteIRCode(
                        item,
                        onSuccess = {
                            handleActionOnMainThread {
                                listCodes.remove(item)
                                adapter.notifyDataSetChanged()
                                showContentView()
                            }
                        }, onDeleteError = {
                            handleActionOnMainThread {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.ir_delete_fail),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                })
            show(activity as AppCompatActivity)
        }
    }

    private fun changeIrCode(position: Int, item: CHHub3IRCode, name: String) {
        // viewModel.getCHHub3().irCodeChange(item.irID, name) {}
        showOverLayLoadingView()
        viewModel.changeIRCode(item, name, onSuccess = {
            handleActionOnMainThread {
                listCodes[position].name = name
                adapter.notifyItemChanged(position)
                showContentView()
            }
        }, onChangeError = {
            handleActionOnMainThread {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.ir_change_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.runCatching {
            if (isRegisterMode) {
                getCHHub3().irModeSet(MODEL_CONTROL.toByte()) {}
            }

            getCHHub3().unsubscribeLearnData()

            if (::chHub3Delegate.isInitialized) {
                getCHHub3().multicastDelegate.removeDelegate(chHub3Delegate)
            }
        }.onFailure { e ->
            L.e(gTag, "${e.message}")
            safeNavigateBack()
        }
        setFragmentResult(
            Config.learningIrDeviceResult, bundleOf(
                Config.irDevice to viewModel.getIrRemoteDevice()
            )
        )
    }

    private fun handleActionOnMainThread(action: () -> Unit) {
        if (view == null) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            action()
        }
    }

    private fun handleActionOnIoThread(action: () -> Unit) {
        if (view == null) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            action()
        }
    }

    /**
     * 判断蓝牙是否已打开
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * 获取蓝牙状态
     */
    fun getBluetoothState(context: Context): Boolean {
        val bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        return when {
            bluetoothAdapter == null -> false
            !bluetoothAdapter.isEnabled -> false
            else -> return true
        }
    }


}
