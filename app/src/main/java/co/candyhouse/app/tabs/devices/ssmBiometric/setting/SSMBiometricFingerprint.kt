package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpFpListBinding
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest
import co.candyhouse.sesame.server.dto.AuthenticationData
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.hexStringToByteArray
import co.utils.isUUIDv4
import co.utils.noHashtoUUID
import co.utils.recycle.GenericAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


data class FingerPrint(var id: String, var name: String, var type: Byte, var fingerPrintNameUUID: String)

class SSMTouchProFingerprint : BaseDeviceFG<FgSsmTpFpListBinding>() {
    var mFingers = ArrayList<FingerPrint>()
    lateinit var fingerprintDelegate: CHFingerPrintDelegate
    override fun getViewBinder() = FgSsmTpFpListBinding.inflate(layoutInflater)
    private val tag = "SSMTouchProFingerprint"
    private val operationType = "fingerprint"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupEventListeners()
        setupFingerPrintDelegate()
        setupRecyclerView()
        loadInitialData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanupResources()
    }

    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        bind.swiperefresh.isEnabled = false
        bind.gifView.initView(R.drawable.gif_finger)
        getBiometricBase()?.let {
            val name = it.productModel.modelName()
            bind.emptyView.text = getString(R.string.TouchEmptyFingerHint, name, name)
        }
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 添加模式按钮
        bind.imgModeAdd.setOnClickListener {
            setFingerPrintMode(2)
        }

        // 验证模式按钮
        bind.imgModeVerify.setOnClickListener {
            setFingerPrintMode(1)
        }
    }

    /**
     * 设置指纹模式
     */
    private fun setFingerPrintMode(mode: Byte) {
        getFingerPrintCapable()?.fingerPrintModeSet(mode) {
            it.onSuccess {
                updateModeUI(mode)
            }
        }
    }

    /**
     * 更新模式UI
     */
    private fun updateModeUI(mode: Byte) {
        if (!isAdded) return

        view?.post {
            if (mode.toInt() == 2) {
                bind.imgModeAdd.visibility = View.GONE
                bind.imgModeVerify.visibility = View.VISIBLE
            } else {
                bind.imgModeVerify.visibility = View.GONE
                bind.imgModeAdd.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 设置指纹代理
     */
    private fun setupFingerPrintDelegate() {
        fingerprintDelegate = createFingerPrintDelegate()

        getFingerPrintCapable()?.let { fingerPrintCapable ->
            getBiometricBase()?.let { biometricBase ->
                fingerPrintCapable.registerEventDelegate(biometricBase, fingerprintDelegate)
            }
        }
    }

    // 异步操作， 获取服务器上的名字， 然后刷新 UI
    private fun getFingerPrintName(fingerPrintNameUUID: String, fingerPrintID: String, type: Byte, deviceUUID: String) {
        (mDeviceModel.ssmLockLiveData.value as CHFingerPrintCapable).fingerPrintNameGet(fingerPrintID, fingerPrintNameUUID, UserUtils.getUserId()?:"", deviceUUID) {
            L.d("harry", "fingerPrintName: $it")
            it.onSuccess {
                L.d("harry", "fingerPrintName:${it.data}")
                val name: String = if (it.data == "") { // 如果服务器上没有名字， 使用默认名称
                    getString(R.string.default_fingerprint_name)
                } else {
                    it.data
                }
                runOnUiThread {
                    mFingers.remove(mFingers.find { it.id == fingerPrintID })
                    mFingers.add(0, FingerPrint(fingerPrintID, name, type, fingerPrintNameUUID))
                    updateFingerPrintList()
                }
            }
        }
    }

    private fun setFingerPrintName(data: FingerPrint, name: String, deviceUUID: String) {
        if (!data.fingerPrintNameUUID.isUUIDv4()) {
            val uuid = UUID.randomUUID().toString().lowercase()
            getFingerPrintCapable()?.fingerPrintsChange(data.id, uuid.replace("-", "")) { result ->
                result.onSuccess {
                    data.fingerPrintNameUUID = uuid
                    data.name = name
                    executeFingerPrintNameSet(data, name, deviceUUID)
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        } else {
            executeFingerPrintNameSet(data, name, deviceUUID)
        }
    }

    private fun executeFingerPrintNameSet(data: FingerPrint, name: String, deviceUUID: String) {
        val fingerPrintNameRequest = CHFingerPrintNameRequest(
            fingerPrintNameUUID = data.fingerPrintNameUUID,
            subUUID = UserUtils.getUserId() ?:"",
            stpDeviceUUID = deviceUUID,
            name = name,
            fingerPrintID = data.id,
            type = data.type,
        )
        getFingerPrintCapable()?.fingerPrintNameSet(fingerPrintNameRequest) { it ->
            it.onSuccess {
                L.d("harry", "fingerPrintNameSet: ${it.data}")
                if (it.data == "Ok") {
                    data.name = name
                    runOnUiThread { updateFingerprintList(data) }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(it.data) }
                }
            }
            it.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
            }
        }
    }

    /**
     * 创建指纹代理
     */
    private fun createFingerPrintDelegate(): CHFingerPrintDelegate {
        return object : CHFingerPrintDelegate {
            override fun onFingerPrintReceiveStart(device: CHSesameConnector) {
                runOnUiThread {
                    mFingers.clear()
                    bind.swiperefresh.isRefreshing = true
                }
            }

            override fun onFingerPrintReceive(
                device: CHSesameConnector,
                ID: String,
                name: String,
                type: Byte
            ) {
                // 直接显示从BLE得到的指纹。收完数据后，批量获取指纹名称
                runOnUiThread {
                    mFingers.remove(mFingers.find { it.id == ID })
                    if ((name.length == 32) && name.isUUIDv4()) { // 是 uuid 格式的名字
                        mFingers.add(0, FingerPrint(ID, getString(R.string.default_fingerprint_name), type, name.noHashtoUUID().toString()))
                    } else {
                        val matchName = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
                        mFingers.add(0, FingerPrint(ID, matchName, type, matchName))
                    }
                    updateFingerPrintList()
                }
            }

            override fun onFingerPrintReceiveEnd(device: CHSesameConnector) {
                runOnUiThread {
                    bind.swiperefresh.isRefreshing = false
                    updateFingerPrintList()
                    isUpload = true
                    postListToServer(mFingers)
                }
            }

            override fun onFingerPrintChanged(
                device: CHSesameConnector,
                ID: String,
                name: String,
                type: Byte
            ) {
                L.d("harry", "onFingerPrintChanged : name.length = ${name.length}, name: $name")
                val newFingerprint = if ((name.length == 32) && name.isUUIDv4()) {
                    FingerPrint(ID, getString(R.string.default_fingerprint_name), type, name.noHashtoUUID().toString())
                } else {
                    val matchName = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
                    FingerPrint(ID, matchName, type, matchName)
                }
                runOnUiThread {
                    updateFingerprintList(newFingerprint)
                    addFingerprintToServer(ID, newFingerprint.fingerPrintNameUUID, type)
                }
            }

            override fun onFingerModeChange(device: CHSesameConnector, mode: Byte) {
                updateModeUI(mode)
            }

            override fun onFingerDelete(device: CHSesameConnector, ID: String) {
                L.d("hcia", "onFingerDelete : $ID")
                val fingerprint = mFingers.find { it.id.toInt(16) == ID.toInt(16) }
                if (fingerprint != null) {
                    runOnUiThread {
                        mFingers.remove(fingerprint)
                        updateFingerPrintList()
                    }
                    deleteFingerprintFromServer(fingerprint)
                }

            }
        }
    }

    private fun deleteFingerprintFromServer(fingerprint: FingerPrint) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val cardAuthenticationData = AuthenticationData()
            cardAuthenticationData.credentialId = fingerprint.id
            cardAuthenticationData.nameUUID = fingerprint.fingerPrintNameUUID
            cardAuthenticationData.type = fingerprint.type
            credentialList.add(cardAuthenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getFingerPrintCapable()?.getFingerPrintDataSyncCapable()?.deleteAuthenticationData(request) { result ->
                result.onSuccess {
                    L.d(tag, "deleteAuthenticationData: success")
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun getFingerprintNameInNeed(device: CHSesameConnector,fingerprintID: String, name: String, type: Byte) {
        if ((name.length == 32) && name.isUUIDv4()) { // 如果是 UUID 格式的名字
            val fingerPrintNameUUID = name.noHashtoUUID().toString()
            getFingerPrintName(fingerPrintNameUUID, fingerprintID, type, getDeviceUUID())
        }
    }

    private fun addFingerprintToServer(fingerprintID: String, nameUUID: String, type: Byte) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val authenticationData = AuthenticationData()
            authenticationData.credentialId = fingerprintID
            authenticationData.nameUUID = nameUUID
            authenticationData.type = type
            credentialList.add(authenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getFingerPrintCapable()?.getFingerPrintDataSyncCapable()?.putAuthenticationData(request) { it ->
                it.onSuccess {
                    L.d(tag, "addCardToServer success")
                }
                it.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun postListToServer(fingerprintList: ArrayList<FingerPrint>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            fingerprintList.forEach { fingerprint ->
                val cardAuthenticationData = AuthenticationData()
                cardAuthenticationData.credentialId = fingerprint.id
                cardAuthenticationData.nameUUID = fingerprint.fingerPrintNameUUID
                cardAuthenticationData.name = ""
                cardAuthenticationData.type = fingerprint.type
                credentialList.add(cardAuthenticationData)
            }
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getFingerPrintCapable()?.getFingerPrintDataSyncCapable()?.postAuthenticationData(request) { it ->
                it.onSuccess {
                    val res = it.data
                    L.d(tag, "postCardListToServer: $res")
                    val resultFingerprintList = mutableListOf<FingerPrint>()
                    res.forEach {
                        resultFingerprintList.add(FingerPrint(it.credentialId, it.name, it.type, it.nameUUID))
                    }
                    if (res.isNotEmpty()) {
                        runOnUiThread {
                            mFingers.clear()
                            mFingers.addAll(resultFingerprintList)
                            updateFingerPrintList()
                        }
                    } else {
                        L.d(tag, "postCardListToServer: res is empty")
                    }
                }
                it.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    /**
     * 更新指纹列表UI
     */
    private fun updateFingerPrintList() {
        bind.leaderboardList.adapter?.notifyDataSetChanged()
        bind.menuTitle.text = "${mFingers.size}/100"
    }


    private fun updateFingerprintList(fingerprint: FingerPrint) {
        val index = mFingers.indexOfFirst { it.id == fingerprint.id }
        if (index != -1) {
            mFingers[index] = fingerprint
        } else {
            mFingers.add(0, fingerprint)
        }
        updateFingerPrintList()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        bind.leaderboardList.adapter = createFingerPrintAdapter()
    }

    /**
     * 创建指纹适配器
     */
    private fun createFingerPrintAdapter(): GenericAdapter<FingerPrint> {
        return object : GenericAdapter<FingerPrint>(mFingers) {
            override fun getLayoutId(position: Int, obj: FingerPrint): Int =
                R.layout.cell_fingerprint

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(view), Binder<FingerPrint> {
                    override fun bind(data: FingerPrint, pos: Int) {
                        setupFingerPrintItem(itemView, data)
                    }
                }
        }
    }

    /**
     * 设置指纹列表项
     */
    private fun setupFingerPrintItem(itemView: View, data: FingerPrint) {
        val title = itemView.findViewById<TextView>(R.id.title)
        val name = itemView.findViewById<TextView>(R.id.sub_title)

        // 设置标题和名称
        title.text = (data.id.hexStringToByteArray()[0].toUInt().toInt() + 1).toString().padStart(3, '0')
        name.text = if (data.name.isEmpty())
            getString(R.string.default_fingerprint_name)
        else
            data.name

        // 设置点击事件
        itemView.setOnClickListener {
            showFingerPrintActionDialog(data, title.text.toString(), name.text.toString())
        }
    }

    /**
     * 显示指纹操作对话框
     */
    private fun showFingerPrintActionDialog(data: FingerPrint, titleText: String, nameText: String) {
        AlertView(titleText, "", AlertStyle.IOS).apply {
            // 添加修改操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProFingerModify),
                    AlertActionStyle.DEFAULT
                ) { _ ->
                    showFingerPrintRenameDialog(data, nameText)
                }
            )

            // 添加删除操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProFingerDelete),
                    AlertActionStyle.NEGATIVE
                ) { _ ->
                    deleteFingerPrint(data)
                }
            )

            show(activity as? AppCompatActivity ?: return)
        }
    }

    /**
     * 显示指纹重命名对话框
     */
    private fun showFingerPrintRenameDialog(data: FingerPrint, currentName: String) {
        context?.inputTextAlert("", currentName, data.name) {
            confirmButtonWithText("OK") { _, name ->
                dismiss()
                renameFingerPrint(data, name)
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    /**
     * 重命名指纹
     */
    private fun renameFingerPrint(data: FingerPrint, newName: String) {
        L.d("harry", "id: ${data.id}; oldName: ${data.name}; newName: $newName; oldNameLength: ${data.name.length}; fingerPrintNameUUID: ${data.fingerPrintNameUUID}")
        setFingerPrintName(data, newName, getDeviceUUID())
    }

    /**
     * 删除指纹
     */
    private fun deleteFingerPrint(data: FingerPrint) {
        getFingerPrintCapable()?.fingerPrintDelete(data.id, getDeviceUUID()) { }
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 获取当前模式
        getFingerPrintCapable()?.fingerPrintModeGet { result ->
            result.onSuccess { data ->
                if (isAdded) {
                    updateModeUI(data.data.toByte())
                }
            }
        }

        // 获取指纹列表
        getFingerPrintCapable()?.fingerPrints {}
    }

    /**
     * 清理资源
     */
    private fun cleanupResources() {
        getFingerPrintCapable()?.let { fingerPrintCapable ->
            // 设置默认模式
            fingerPrintCapable.fingerPrintModeSet(2) {}

            // 取消注册代理
            if (::fingerprintDelegate.isInitialized) {
                getBiometricBase()?.let { fingerPrintCapable.unregisterEventDelegate(it, fingerprintDelegate) }
            }
        }
    }

    /**
     * 获取指纹能力
     */
    private fun getFingerPrintCapable(): CHFingerPrintCapable? {
        return mDeviceModel.ssmLockLiveData.value as? CHFingerPrintCapable
    }

    /**
     * 获取生物识别基类
     */
    private fun getBiometricBase(): CHSesameBiometricBase? {
        return mDeviceModel.ssmLockLiveData.value as? CHSesameBiometricBase
    }

    private fun getDeviceUUID(): String {
        return (mDeviceModel.ssmLockLiveData.value as CHSesameBiometricBase).deviceId.toString().uppercase()
    }

}