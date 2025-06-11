package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpPasscodeListBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest
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
import co.utils.hexStringToIntStr
import co.utils.isUUIDv4
import co.utils.noHashtoUUID
import co.utils.recycle.GenericAdapter
import co.utils.recycle.GenericAdapter.Binder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

data class KeyboardPassCode(var id: String, var name: String, var type: Byte, var nameUUID: String)

/**
 * 密码管理界面
 */
class SesameKeyboardPassCode : BaseDeviceFG<FgSsmTpPasscodeListBinding>(), CHWifiModule2Delegate {

    // 常量定义
    companion object {
        private const val MAX_PASSCODE_COUNT = 100
        private const val MODE_CONTROL = 0.toByte()
        private const val MODE_REGISTER = 1.toByte()
    }

    // 数据
    private var mKbSecretList = ArrayList<KeyboardPassCode>()
    private lateinit var passCodeDelegate: CHPassCodeDelegate
    private val tag = "SesameKeyboardPassCode"
    private val operationType = "passcode"

    override fun getViewBinder() = FgSsmTpPasscodeListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupEventListeners()
        setupPassCodeDelegate()
        setupRecyclerView()
        loadInitialData()
    }

    override fun onStop() {
        super.onStop()
        L.d("hcia", "onStop:")
    }

    override fun onDestroyView() {
        cleanupResources()
        super.onDestroyView()
    }

    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        bind.swiperefresh.isEnabled = false
        updatePassCodeCountDisplay()
        getBiometricBase()?.let {
            val name = it.productModel.modelName()
            bind.emptyView.text = getString(R.string.TouchEmptyPasscodeHint, name)
        }
    }

    /**
     * 更新密码数量显示
     */
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun updatePassCodeCountDisplay() {
        runOnUiThread {
            bind.menuTitle.text = "${mKbSecretList.size}/$MAX_PASSCODE_COUNT"
            bind.leaderboardList.adapter?.notifyDataSetChanged()
        }
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 添加模式按钮
        bind.imgModeAdd.setOnClickListener {
            setPassCodeMode(MODE_CONTROL)
        }

        // 验证模式按钮
        bind.imgModeVerify.setOnClickListener {
            setPassCodeMode(MODE_REGISTER)
        }
    }

    /**
     * 设置密码模式
     */
    private fun setPassCodeMode(mode: Byte) {
        getPassCodeCapable()?.keyBoardPassCodeModeSet(mode) { result ->
            result.onSuccess {
                updateModeUI(mode)
            }
        }
    }

    /**
     * 更新模式UI
     */
    private fun updateModeUI(mode: Byte) {
        runOnUiThread {
            if (mode == MODE_CONTROL) {
                bind.imgModeVerify.visibility = View.VISIBLE
                bind.imgModeAdd.visibility = View.GONE
            } else {
                bind.imgModeAdd.visibility = View.VISIBLE
                bind.imgModeVerify.visibility = View.GONE
            }
        }
    }

    /**
     * 设置密码代理
     */
    private fun setupPassCodeDelegate() {
        passCodeDelegate = createPassCodeDelegate()

        getPassCodeCapable()?.let { passCodeCapable ->
            getBiometricBase()?.let { biometricBase ->
                passCodeCapable.registerEventDelegate(biometricBase, passCodeDelegate)
            }
        }
    }

    // 异步操作， 获取服务器上的名字， 然后刷新 UI
    private fun getKeyboardPassCodeName(keyboardPassCodeNameUUID: String, keyboardPassCodeID: String, type: Byte, deviceUUID: String) {
        // TODO: 如果手机没联网， 需要提示用户？？？
        AWSStatus.getSubUUID()?.let { it ->
            (mDeviceModel.ssmLockLiveData.value as CHPassCodeCapable).keyBoardPassCodeNameGet(keyboardPassCodeID.hexStringToIntStr(), keyboardPassCodeNameUUID, it, deviceUUID) {
                it.onSuccess {
                    val name: String = if (it.data == "") { // 如果服务器上没有名字， 使用默认名称
                        getString(R.string.default_passcode_name)
                    } else {
                        it.data
                    }
                    runOnUiThread {
                        mKbSecretList.remove(mKbSecretList.find { it.id == keyboardPassCodeID })
                        mKbSecretList.add(0, KeyboardPassCode(keyboardPassCodeID, name, type, keyboardPassCodeNameUUID))
                        updatePassCodeCountDisplay()
                    }
                }
            }
        } ?: run { // 未登录用户， 没有 subUUID
            // 如果没有 AWSStatus.getSubUUID()， 直接使用默认名称
            val name: String = getString(R.string.default_passcode_name)
            runOnUiThread {
                mKbSecretList.remove(mKbSecretList.find { it.id == keyboardPassCodeID })
                mKbSecretList.add(0, KeyboardPassCode(keyboardPassCodeID, name, type, keyboardPassCodeNameUUID))
                updatePassCodeCountDisplay()
            }
        }
    }

    private fun setKeyboardPassCodeName(data: KeyboardPassCode, name: String, deviceUUID: String) {
        if (!data.nameUUID.isUUIDv4()) {
            val uuid = UUID.randomUUID().toString().lowercase()
            getPassCodeCapable()?.keyBoardPassCodeChange(data.id, uuid.replace("-", "")) { result ->
                result.onSuccess {
                    data.nameUUID = uuid
                    data.name = name
                    runOnUiThread { updatePassCodeList(data) }
                    executeKeyboardPassCodeNameSet(data, name, deviceUUID)
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }

            }
        } else {
            executeKeyboardPassCodeNameSet(data, name, deviceUUID)
        }
    }

    private fun executeKeyboardPassCodeNameSet(data: KeyboardPassCode, name: String, deviceUUID: String) {
        val keyBoardPassCodeNameRequest = CHKeyBoardPassCodeNameRequest(
            keyBoardPassCodeNameUUID = data.nameUUID,
            subUUID = UserUtils.getUserId() ?: "",
            stpDeviceUUID = deviceUUID,
            name = name,
            keyBoardPassCode = data.id.hexStringToIntStr(), // 这里需要转换成十进制
            type = data.type,
        )
        getPassCodeCapable()?.keyBoardPassCodeNameSet(keyBoardPassCodeNameRequest) { it ->
            it.onSuccess {
                L.d("harry", "keyBoardPassCodeNameSet: ${it.data}")
                if (it.data == "Ok") {
                    data.name = name
                    runOnUiThread { updatePassCodeList(data) }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(it.data) }
                }
            }
            it.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
            }
        }
    }

    private fun updatePassCodeList(passCode: KeyboardPassCode) {
        val index = mKbSecretList.indexOfFirst { it.id == passCode.id }
        if (index != -1) {
            mKbSecretList[index] = passCode
        } else {
            mKbSecretList.add(0, passCode)
        }
        updatePassCodeCountDisplay()
    }

    /**
     * 创建密码代理
     */
    private fun createPassCodeDelegate(): CHPassCodeDelegate {
        return object : CHPassCodeDelegate {
            override fun onKeyBoardReceiveStart(device: CHSesameConnector) {
                runOnUiThread {
                    mKbSecretList.clear()
                    bind.swiperefresh.isRefreshing = true
                }
            }

            override fun onKeyBoardReceive(
                device: CHSesameConnector, ID: String, name: String, type: Byte
            ) {
                L.d("harry", "onKeyBoardReceive : ID = $ID, name.length = ${name.length}, name: $name, type: $type")
                // 直接显示从BLE得到的密码。收完数据后，批量获取密码名称
                runOnUiThread {
                    mKbSecretList.remove(mKbSecretList.find { it.id == ID })
                    if ((name.length == 32) && name.isUUIDv4()) { // 是 uuid 格式的名字
                        mKbSecretList.add(0, KeyboardPassCode(ID, getString(R.string.default_passcode_name), type, name.noHashtoUUID().toString()))
                    } else {
                        val matchName = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
                        mKbSecretList.add(0, KeyboardPassCode(ID, matchName, type, matchName))
                    }
                    updatePassCodeCountDisplay()
                }
            }

            override fun onKeyBoardReceiveEnd(device: CHSesameConnector) {
                runOnUiThread {
                    bind.swiperefresh.isRefreshing = false
                    updatePassCodeCountDisplay()
                    isUpload = true
                    postListToServer(mKbSecretList)
                }
            }

            override fun onKeyBoardChanged(
                device: CHSesameConnector, ID: String, name: String, type: Byte
            ) {
                L.d("harry", "onKeyBoardChanged : ID = $ID, name.length = ${name.length}, name: $name, type: $type")

                val newPassCode = if ((name.length == 32) && name.isUUIDv4()) { // 是 uuid 格式的名字
                    KeyboardPassCode(ID, getString(R.string.default_passcode_name), type, name.noHashtoUUID().toString())
                } else {
                    val matchName = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
                    KeyboardPassCode(ID, matchName, type, matchName)
                }
                runOnUiThread {
                    updatePassCodeList(newPassCode)
                    addPasscodeToServer(ID, newPassCode.nameUUID, type)
                }
            }

            override fun onKeyBoardModeChange(device: CHSesameConnector, mode: Byte) {
                L.d("harry", "[onKeyBoardModeChange] mode: $mode")
                updateModeUI(mode)
            }

            override fun onKeyBoardDelete(device: CHSesameConnector, ID: String) {
                val passCode = mKbSecretList.find { it.id.lowercase() == ID.lowercase() }
                if (passCode != null) {
                    runOnUiThread {
                        mKbSecretList.remove(passCode)
                        updatePassCodeCountDisplay()
                    }
                    deletePassCodeFromServer(passCode)
                }
            }
        }
    }

    private fun addPasscodeToServer(passCodeID: String, nameUUID: String, type: Byte) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val authenticationData = AuthenticationData()
            authenticationData.credentialId = passCodeID
            authenticationData.nameUUID = nameUUID
            authenticationData.type = type
            credentialList.add(authenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getPassCodeCapable()?.getBoardPassCodeDataSyncCapable()?.putAuthenticationData(request) { result ->
                result.onSuccess {
                    L.d(tag, "addPasscodeToServer: success")
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun deletePassCodeFromServer(passCode: KeyboardPassCode) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val cardAuthenticationData = AuthenticationData()
            cardAuthenticationData.credentialId = passCode.id
            cardAuthenticationData.nameUUID = passCode.nameUUID
            cardAuthenticationData.type = passCode.type
            credentialList.add(cardAuthenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getPassCodeCapable()?.getBoardPassCodeDataSyncCapable()?.deleteAuthenticationData(request) { result ->
                result.onSuccess {
                    L.d(tag, "deleteAuthenticationData: success")
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun postListToServer(passCodeList: ArrayList<KeyboardPassCode>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            passCodeList.forEach { passCode ->
                val cardAuthenticationData = AuthenticationData()
                cardAuthenticationData.credentialId = passCode.id
                cardAuthenticationData.nameUUID = passCode.nameUUID
                cardAuthenticationData.name = ""
                cardAuthenticationData.type = passCode.type
                credentialList.add(cardAuthenticationData)
            }
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getPassCodeCapable()?.getBoardPassCodeDataSyncCapable()?.postAuthenticationData(request) { result ->
                result.onSuccess {
                    L.d(tag, "postListToServer: success")
                    val res = it.data
                    L.d(tag, "postCardListToServer: $res")
                    val resultList = mutableListOf<KeyboardPassCode>()
                    res.forEach {
                        resultList.add(KeyboardPassCode(it.credentialId, it.name, it.type, it.nameUUID))
                    }
                    if (res.isNotEmpty()) {
                        runOnUiThread {
                            mKbSecretList.clear()
                            mKbSecretList.addAll(resultList)
                            updatePassCodeCountDisplay()
                        }
                    } else {
                        L.d(tag, "postCardListToServer: res is empty")
                    }
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        bind.leaderboardList.adapter = createPassCodeAdapter()
    }

    /**
     * 创建密码适配器
     */
    private fun createPassCodeAdapter(): GenericAdapter<KeyboardPassCode> {
        return object : GenericAdapter<KeyboardPassCode>(mKbSecretList) {
            override fun getLayoutId(position: Int, obj: KeyboardPassCode): Int = R.layout.cell_password

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder = PassCodeViewHolder(view)
        }
    }

    /**
     * 密码ViewHolder
     */
    private inner class PassCodeViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view), Binder<KeyboardPassCode> {

        override fun bind(data: KeyboardPassCode, pos: Int) {
            val title = itemView.findViewById<TextView>(R.id.title)
            val name = itemView.findViewById<TextView>(R.id.sub_title)

            // 设置文本
            title.text = data.id.hexStringToIntStr()
            name.text = if (data.name.isEmpty()) getString(R.string.default_passcode_name)
            else data.name

            // 设置点击事件
            itemView.setOnClickListener {
                showPassCodeActionDialog(data, title.text.toString(), name.text.toString())
            }
        }
    }

    /**
     * 显示密码操作对话框
     */
    private fun showPassCodeActionDialog(data: KeyboardPassCode, titleText: String, nameText: String) {
        AlertView(titleText, "", AlertStyle.IOS).apply {
            // 添加重命名操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProPWDRename), AlertActionStyle.DEFAULT
                ) { _ ->
                    showPassCodeRenameDialog(data, nameText)
                })

            // 添加删除操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProPWDDelete), AlertActionStyle.NEGATIVE
                ) { _ ->
                    deletePassCode(data)
                })

            show(activity as? AppCompatActivity ?: return)
        }
    }

    /**
     * 显示密码重命名对话框
     */
    private fun showPassCodeRenameDialog(data: KeyboardPassCode, currentName: String) {
        context?.inputTextAlert("", currentName, data.name) {
            confirmButtonWithText("OK") { _, name ->
                dismiss()
                renamePassCode(data, name)
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    /**
     * 重命名密码
     */
    private fun renamePassCode(data: KeyboardPassCode, newName: String) {
        setKeyboardPassCodeName(data, newName, getDeviceUUID())
    }

    /**
     * 删除密码
     */
    private fun deletePassCode(data: KeyboardPassCode) {
        getPassCodeCapable()?.keyBoardPassCodeDelete(data.id, getDeviceUUID()) { }
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 获取当前模式
        getPassCodeCapable()?.keyBoardPassCodeModeGet { result ->
            result.onSuccess { data ->
                updateModeUI(data.data)
            }
        }

        // 获取密码数据
        getPassCodeCapable()?.sendKeyBoardPassCodeDataGetCmd {}
    }

    /**
     * 清理资源
     */
    private fun cleanupResources() {
        L.d("hcia", "onDestroyView:")

        // 设置默认模式
        getPassCodeCapable()?.keyBoardPassCodeModeSet(MODE_CONTROL) {}

        // 取消注册代理
        if (::passCodeDelegate.isInitialized) {
            getBiometricBase()?.let { biometricBase ->
                getPassCodeCapable()?.unregisterEventDelegate(biometricBase, passCodeDelegate)
            }
        }
    }

    /**
     * 获取密码能力
     */
    private fun getPassCodeCapable(): CHPassCodeCapable? {
        return mDeviceModel.ssmLockLiveData.value as? CHPassCodeCapable
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