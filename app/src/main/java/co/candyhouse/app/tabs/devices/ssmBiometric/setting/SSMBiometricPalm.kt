package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmFacePalmListBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.ble.os3.CHSesameTouchFace
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHPalmNameRequest
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
import kotlin.onFailure

/**
 * 掌纹管理界面
 */
class SSMBiometricPalm : BaseDeviceFG<FgSsmFacePalmListBinding>() {

    // 常量定义
    companion object {
        private const val MAX_PALM_COUNT = 100
        private val MODE_REGISTER = 1.toByte()
        private val MODE_CONTROL = 0.toByte()
    }

    // 数据
    private var mPalmList = ArrayList<CHSesameTouchFace>()
    private var isRegisterModel = false
    private lateinit var palmDelegate: CHPalmDelegate
    private val tag = "SSMBiometricPalm"
    private val operationType = "palm"

    override fun getViewBinder() = FgSsmFacePalmListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupEventListeners()
        setupPalmDelegate()
        setupRecyclerView()
        loadInitialData()
    }

    override fun onDestroy() {
        cleanupResources()
        super.onDestroy()
    }

    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        bind.swiperefresh.isEnabled = false
        setFaceTips()
        updatePalmCountDisplay()
    }

    /**
     * 设置操作示意图
     */
    private fun setFaceTips() {
        val device = mDeviceModel.ssmLockLiveData.value as CHSesameBiometricBase
        when (device.productModel) {
            CHProductModel.SSMFace -> bind.palmIvTips.setImageResource(R.drawable.palm_tips)
            else -> bind.palmIvTips.setImageResource(R.drawable.palmpro_tips)
        }
    }

    /**
     * 更新掌纹数量显示
     */
    private fun updatePalmCountDisplay() {
        bind.menuTitle.text = "${mPalmList.size}/$MAX_PALM_COUNT"
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 添加模式按钮
        bind.imgModeAdd.setOnClickListener {
            setPalmMode(MODE_CONTROL)
        }

        // 验证模式按钮
        bind.imgModeVerify.setOnClickListener {
            setPalmMode(MODE_REGISTER)
        }
    }

    /**
     * 设置掌纹模式
     */
    private fun setPalmMode(mode: Byte) {
        getPalmCapable()?.palmModeSet(mode) { result ->
            result.onSuccess {
                updateModeState(mode)
            }
            result.onFailure {
                L.d("SSMBiometricPalm", "palmModeSet fail.....mode=$mode")
            }
        }
    }

    /**
     * 更新模式状态和UI
     */
    private fun updateModeState(mode: Byte) {
        isRegisterModel = mode == MODE_REGISTER
        updateModeUI(mode)
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
     * 设置掌纹代理
     */
    private fun setupPalmDelegate() {
        palmDelegate = createPalmDelegate()

        getPalmCapable()?.let { palmCapable ->
            getBiometricBase()?.let { biometricBase ->
                palmCapable.registerEventDelegate(biometricBase, palmDelegate)
            }
        }
    }

    // 异步操作， 获取服务器上的名字， 然后刷新 UI
    private fun getPalmName(palm: CHSesameTouchFace, deviceUUID: String) {
        L.d("harry", "[getFaceName] nameUUID: ${palm.nameUUID}")
        AWSStatus.getSubUUID()?.let { it ->
            (mDeviceModel.ssmLockLiveData.value as CHPalmCapable).palmNameGet(palm.id, palm.nameUUID, it, deviceUUID) {
                L.d("harry", "【faceNameGet】 faceName: $it")
                it.onSuccess {
                    L.d("harry", "faceName:${it.data}")
                    palm.name = if (it.data == "") { // 如果服务器上没有名字， 使用默认名称
                        getString(R.string.default_palm_name)
                    } else {
                        it.data
                    }
                    runOnUiThread {
                        updatePalmList(palm)
                    }
                }
            }
        } ?: run { // 未登录用户， 没有 subUUID
            L.d("harry", "getFaceName: AWSStatus.getSubUUID() is null")
            // 如果没有 AWSStatus.getSubUUID()， 直接使用默认名称
            palm.name = getString(R.string.default_palm_name)
            runOnUiThread {
                updatePalmList(palm)
            }
        }
    }

    private fun setPalmName(palm: CHSesameTouchFace, name: String, deviceUUID: String) {
        val palmNameRequest = CHPalmNameRequest(
            palmNameUUID = palm.nameUUID,
            subUUID = UserUtils.getUserId() ?: "",
            stpDeviceUUID = deviceUUID,
            name = name,
            palmID = palm.id,
            type = palm.type,
        )
        getPalmCapable()?.palmNameSet(palmNameRequest) { it ->
            it.onSuccess {
                L.d("harry", "palmNameSet: ${it.data}")
                palm.name = name
                if (it.data == "Ok") {
                    runOnUiThread {
                        updatePalmList(palm)
                    }
                } else {
                    L.d("harry", "palmNameSet error: ${it.data}")
                }
            }
            it.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
            }
        }
    }

    /**
     * 创建掌纹代理
     */
    private fun createPalmDelegate(): CHPalmDelegate {
        return object : CHPalmDelegate {
            override fun onPalmModeChanged(device: CHSesameConnector, mode: Byte) {
                L.d(tag, "[onPalmModeChanged]ID:$mode")
                updateModeState(mode)
            }

            override fun onPalmReceive(device: CHSesameConnector, palm: CHSesameTouchFace) {
                L.d(tag, "[onPalmReceive] type: ${palm.type}; idLength: ${palm.idLength}; ID:${palm.id}; nameLength: ${palm.nameLength}; nameUUID:${palm.nameUUID}")
//                getPalmName(palm, getDeviceUUID())
                runOnUiThread {
                    addPalmToList(palm)
                }
            }

            override fun onPalmChanged(device: CHSesameConnector, palm: CHSesameTouchFace) {
                L.d(tag, "[onPalmChange] type: ${palm.type}; idLength: ${palm.idLength}; ID:${palm.id}; nameLength: ${palm.nameLength}; nameUUID:${palm.nameUUID}")
                runOnUiThread {
                    updatePalmList(palm)
                    addPalmToServer(palm.id, palm.name, palm.type)
                }
            }

            override fun onPalmReceiveStart(device: CHSesameConnector) {
                // do nothing
                L.d(tag, "[onPalmReceiveStart]")
            }

            override fun onPalmReceiveEnd(device: CHSesameConnector) {
                // do nothing
                L.d(tag, "[onPalmReceiveEnd]")
                runOnUiThread {
                    updatePalmCountDisplay()
                    postListToServer(mPalmList)
                }
            }

            override fun onPalmDeleted(device: CHSesameConnector, palmID: Byte, isSuccess: Boolean) {
                if (isSuccess) {
                    val palm = mPalmList.find { it.id.toInt(16) == palmID.toInt() }
                    if (palm != null) {
                        runOnUiThread {
                            mPalmList.remove(palm)
                            updatePalmCountDisplay()
                            bind.leaderboardList.adapter?.notifyDataSetChanged()
                        }
                        deletePalmFromServer(palm)
                    }
                } else {
                    runOnUiThread {
                        toastMSG(getString(R.string.delete_failed_tips))
                    }
                }
            }
        }
    }

    private fun deletePalmFromServer(palm: CHSesameTouchFace) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val cardAuthenticationData = AuthenticationData()
            cardAuthenticationData.credentialId = palm.id
            cardAuthenticationData.nameUUID = palm.nameUUID
            cardAuthenticationData.type = palm.type
            credentialList.add(cardAuthenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getPalmCapable()?.getPalmDataSyncCapable()?.deleteAuthenticationData(request) { result ->
                result.onSuccess {
                    L.d(tag, "deleteAuthenticationData: success")
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun addPalmToServer(palmID: String, name: String, type: Byte) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val authenticationData = AuthenticationData()
            authenticationData.credentialId = palmID
            authenticationData.nameUUID = name
            authenticationData.type = type
            credentialList.add(authenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getPalmCapable()?.getPalmDataSyncCapable()?.putAuthenticationData(request) { it ->
                it.onSuccess {
                    L.d(tag, "addCardToServer success")
                }
                it.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun postListToServer(palmList: ArrayList<CHSesameTouchFace>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            palmList.forEach { touchFace ->
                val cardAuthenticationData = AuthenticationData()
                cardAuthenticationData.credentialId = touchFace.id
                cardAuthenticationData.nameUUID = touchFace.nameUUID
                cardAuthenticationData.name = ""
                cardAuthenticationData.type = touchFace.type
                credentialList.add(cardAuthenticationData)
            }
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getPalmCapable()?.getPalmDataSyncCapable()?.postAuthenticationData(request) { it ->
                it.onSuccess {
                    L.d(tag, "postListToServer success")
                    val res = it.data
                    L.d(tag, "postListToServer: $res")
                    val resultCardList = mutableListOf<CHSesameTouchFace>()
                    res.forEach {
                        resultCardList.add(CHSesameTouchFace(it.credentialId, it.name, it.type, it.nameUUID))
                    }
                    if (resultCardList.isNotEmpty()) {
                        runOnUiThread {
                            mPalmList.clear()
                            mPalmList.addAll(resultCardList)
                            bind.leaderboardList.adapter?.notifyDataSetChanged()
                            updatePalmCountDisplay()
                        }
                    } else {
                        L.d(tag, "postListToServer: res is empty")
                    }
                }
                it.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    /**
     * 更新掌纹列表
     */
    private fun updatePalmList(palm: CHSesameTouchFace) {
        val index = mPalmList.indexOf(palm)
        if (index != -1) {
            mPalmList[index] = palm
            bind.leaderboardList.adapter?.notifyItemChanged(index)
        } else {
            mPalmList.add(0, palm)
            updatePalmCountDisplay()
            bind.leaderboardList.adapter?.notifyDataSetChanged()
        }
    }

    private fun addPalmToList(palm: CHSesameTouchFace) : CHSesameTouchFace {
        mPalmList.remove(mPalmList.find { it.id == palm.id })
        mPalmList.add(0, palm)
        updatePalmCountDisplay()
        bind.leaderboardList.adapter?.notifyDataSetChanged()
        return mPalmList.get(0)
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        getBiometricBase()?.let { bind.emptyView.text = getString(R.string.sesamepalmMsg, it.productModel.modelName()) }
        bind.leaderboardList.adapter = createPalmAdapter()
    }

    /**
     * 创建掌纹适配器
     */
    private fun createPalmAdapter(): GenericAdapter<CHSesameTouchFace> {
        return object : GenericAdapter<CHSesameTouchFace>(mPalmList) {
            override fun getLayoutId(position: Int, obj: CHSesameTouchFace): Int {
                return R.layout.cell_suica
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(view), Binder<CHSesameTouchFace> {
                    override fun bind(data: CHSesameTouchFace, position: Int) {
                        setupPalmItem(itemView, data)
                    }
                }
        }
    }

    /**
     * 设置掌纹列表项
     */
    private fun setupPalmItem(itemView: View, data: CHSesameTouchFace) {
        val title = itemView.findViewById<TextView>(R.id.title)
        val name = itemView.findViewById<TextView>(R.id.sub_title)
        val image = itemView.findViewById<ImageView>(R.id.image)

        // 设置数据
        title.text = (data.id.hexStringToByteArray()[0].toUInt().toInt() + 1).toString().padStart(3, '0')
        name.text = if (data.name == "") getString(R.string.default_palm_name) else data.name
        image.setImageResource(R.drawable.camera_palm)

        // 设置点击事件
        itemView.setOnClickListener {
            showPalmActionDialog(data, title.text.toString(), name.text.toString())
        }
    }

    /**
     * 显示掌纹操作对话框
     */
    private fun showPalmActionDialog(data: CHSesameTouchFace, titleText: String, nameText: String) {
        AlertView(titleText, "", AlertStyle.IOS).apply {
            // 添加重命名操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProPalmRename),
                    AlertActionStyle.DEFAULT
                ) { _ ->
                    showPalmRenameDialog(data, nameText)
                }
            )

            // 添加删除操作
            addAction(
                AlertAction(
                    getString(R.string.DelPalm),
                    AlertActionStyle.NEGATIVE
                ) { _ ->
                    deletePalm(data)
                }
            )

            show(activity as? AppCompatActivity ?: return)
        }
    }

    /**
     * 显示掌纹重命名对话框
     */
    private fun showPalmRenameDialog(data: CHSesameTouchFace, currentName: String) {
        context?.inputTextAlert("", currentName, data.name) {
            confirmButtonWithText("OK") { _, name ->
                dismiss()
                renamePalm(data, name)
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    /**
     * 重命名掌纹
     */
    private fun renamePalm(data: CHSesameTouchFace, newName: String) {
        L.d("harry", "id: ${data.id}; oldName: ${data.name}; newName: $newName; oldNameLength: ${data.name.length}; palmNameUUID: ${data.nameUUID}")
        setPalmName(data, newName, getDeviceUUID())
    }

    /**
     * 删除掌纹
     */
    private fun deletePalm(data: CHSesameTouchFace) {
        getPalmCapable()?.palmDelete(data.id, getDeviceUUID()) {}
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 获取掌纹列表
        getPalmCapable()?.palmListGet {}

        // 获取当前模式
        getCurrentPalmMode()
    }

    /**
     * 获取当前掌纹模式
     */
    private fun getCurrentPalmMode() {
        getPalmCapable()?.palmModeGet { result ->
            result.onSuccess { data ->
                updateModeState(data.data)
            }
        }
    }

    /**
     * 清理资源
     */
    private fun cleanupResources() {
        // 如果处于注册模式，恢复到控制模式
        if (isRegisterModel) {
            getPalmCapable()?.palmModeSet(MODE_CONTROL) {}
        }

        // 取消注册代理
        if (::palmDelegate.isInitialized) {
            getBiometricBase()?.let { biometricBase ->
                getPalmCapable()?.unregisterEventDelegate(biometricBase, palmDelegate)
            }
        }
    }

    /**
     * 获取掌纹能力
     */
    private fun getPalmCapable(): CHPalmCapable? {
        return mDeviceModel.ssmLockLiveData.value as? CHPalmCapable
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