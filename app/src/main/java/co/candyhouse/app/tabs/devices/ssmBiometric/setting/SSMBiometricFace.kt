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
import co.candyhouse.app.databinding.FgSsmFaceFacesListBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.ble.os3.CHSesameTouchFace
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHFaceNameRequest
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

/**
 * 人脸管理界面
 */
class SesameFaceProFaces : BaseDeviceFG<FgSsmFaceFacesListBinding>() {
    // 常量定义
    companion object {
        private const val MAX_FACE_COUNT = 100
        private val MODE_REGISTER = 1.toByte()
        private val MODE_CONTROL = 0.toByte()
    }

    // 数据
    private var mFaceList = ArrayList<CHSesameTouchFace>()
    private var isRegisterModel = false
    private lateinit var faceDelegate: CHFaceDelegate
    private val tag = "SesameFaceProFaces"
    private val operationType = "face"

    override fun getViewBinder() = FgSsmFaceFacesListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupEventListeners()
        setupFaceDelegate()
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
        updateFaceCountDisplay()
    }

    /**
     * 设置操作示意图
     */
    private fun setFaceTips() {
        val device = mDeviceModel.ssmLockLiveData.value as CHSesameBiometricBase
        when (device.productModel) {
            CHProductModel.SSMFace -> bind.faceIvTips.setImageResource(R.drawable.face_tips)
            else -> bind.faceIvTips.setImageResource(R.drawable.facepro_tips)
        }
    }

    /**
     * 更新人脸数量显示
     */
    private fun updateFaceCountDisplay() {
        bind.menuTitle.text = "${mFaceList.size}/$MAX_FACE_COUNT"
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 添加模式按钮
        bind.imgModeAdd.setOnClickListener {
            setFaceMode(MODE_CONTROL)
        }

        // 验证模式按钮
        bind.imgModeVerify.setOnClickListener {
            setFaceMode(MODE_REGISTER)
        }
    }

    /**
     * 设置人脸模式
     */
    private fun setFaceMode(mode: Byte) {
        getFaceCapable()?.faceModeSet(mode) { result ->
            result.onSuccess {
                updateModeState(mode)
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
     * 设置人脸代理
     */
    private fun setupFaceDelegate() {
        faceDelegate = createFaceDelegate()

        getFaceCapable()?.let { faceCapable ->
            getBiometricBase()?.let { biometricBase ->
                faceCapable.registerEventDelegate(biometricBase, faceDelegate)
            }
        }
    }

    // 异步操作， 获取服务器上的名字， 然后刷新 UI
    private fun getFaceName(face: CHSesameTouchFace, deviceUUID: String) {
        L.d("harry", "[getFaceName] nameUUID: ${face.nameUUID}")
        AWSStatus.getSubUUID()?.let { it ->
            (mDeviceModel.ssmLockLiveData.value as CHFaceCapable).faceNameGet(face.id, face.nameUUID, it, deviceUUID) {
                L.d("harry", "【faceNameGet】 faceName: $it")
                it.onSuccess {
                    L.d("harry", "faceName:${it.data}")
                    face.name = if (it.data == "") { // 如果服务器上没有名字， 使用默认名称
                        getString(R.string.default_face_name)
                    } else {
                        it.data
                    }
                    runOnUiThread {
                        updateFaceList(face)
                    }
                }
            }
        } ?: run { // 未登录用户， 没有 subUUID
            L.d("harry", "getFaceName: AWSStatus.getSubUUID() is null")
            // 如果没有 AWSStatus.getSubUUID()， 直接使用默认名称
            face.name = getString(R.string.default_face_name)
            runOnUiThread {
                updateFaceList(face)
            }
        }
    }

    private fun setFaceName(face: CHSesameTouchFace, name: String, deviceUUID: String) {
        val faceNameRequest = CHFaceNameRequest(
            faceNameUUID = face.nameUUID,
            subUUID = UserUtils.getUserId() ?: "",
            stpDeviceUUID = deviceUUID,
            name = name,
            faceID = face.id,
            type = face.type,
        )
        getFaceCapable()?.faceNameSet(faceNameRequest) { it ->
            it.onSuccess {
                face.name = name
                if (it.data == "Ok") {
                    runOnUiThread {
                        updateFaceList(face)
                    }
                } else {
                    L.d("harry", "faceNameSet error: ${it.data}")
                }
            }
            it.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
            }
        }
    }

    /**
     * 创建人脸代理
     */
    private fun createFaceDelegate(): CHFaceDelegate {
        return object : CHFaceDelegate {
            override fun onFaceModeChanged(device: CHSesameConnector, mode: Byte) {
                updateModeUI(mode)
            }

            override fun onFaceReceive(
                device: CHSesameConnector, face: CHSesameTouchFace
            ) {
                L.d(tag, "[onFaceReceive] type: ${face.type}; idLength: ${face.idLength}; ID:${face.id}; nameLength: ${face.nameLength}; nameUUID:${face.nameUUID}")
                runOnUiThread {
                   addFaceToList(face)
                }
            }

            override fun onFaceChanged(device: CHSesameConnector, face: CHSesameTouchFace) {
                runOnUiThread {
                    updateFaceList(face)
                    addFaceToServer(face)
                }
            }

            override fun onFaceReceiveStart(device: CHSesameConnector) {
                // do nothing
            }

            override fun onFaceReceiveEnd(device: CHSesameConnector) {
                runOnUiThread {
                    updateFaceCountDisplay()
                    postListToServer(mFaceList)
                }
            }

            override fun onFaceDeleted(device: CHSesameConnector, faceID: Byte, isSuccess: Boolean) {
                if (isSuccess) {
                    val face = mFaceList.find { it.id.toInt(16) == faceID.toInt() }
                    if (face != null) {
                        runOnUiThread {
                            mFaceList.remove(face)
                            bind.leaderboardList.adapter?.notifyDataSetChanged()
                            updateFaceCountDisplay()
                        }
                        deleteFaceFromServer(face)
                    }
                } else {
                    runOnUiThread {
                        toastMSG(getString(R.string.delete_failed_tips))
                    }
                }
            }
        }
    }

    private fun deleteFaceFromServer(face: CHSesameTouchFace) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val cardAuthenticationData = AuthenticationData()
            cardAuthenticationData.credentialId = face.id
            cardAuthenticationData.nameUUID = face.nameUUID
            cardAuthenticationData.type = face.type
            credentialList.add(cardAuthenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getFaceCapable()?.getFaceDataSyncCapable()?.deleteAuthenticationData(request){ result ->
                result.onSuccess {
                    L.d(tag, "deleteAuthenticationData: success")
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun addFaceToList(face: CHSesameTouchFace) : CHSesameTouchFace {
        mFaceList.remove(mFaceList.find { it.id == face.id })
        mFaceList.add(0, face)
        updateFaceCountDisplay()
        bind.leaderboardList.adapter?.notifyDataSetChanged()
        return mFaceList.get(0)
    }

    private fun addFaceToServer(face: CHSesameTouchFace) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val authenticationData = AuthenticationData()
            authenticationData.credentialId = face.id
            authenticationData.nameUUID = face.nameUUID
            authenticationData.type = face.type
            authenticationData.name = ""
            credentialList.add(authenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getFaceCapable()?.getFaceDataSyncCapable()?.putAuthenticationData(request) { result ->
                result.onSuccess {
                    L.d(tag, "postListToServer: ${it.data}")
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun postListToServer(faceList: ArrayList<CHSesameTouchFace>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cardCredentialList = mutableListOf<AuthenticationData>()
            faceList.forEach { face ->
                val cardAuthenticationData = AuthenticationData()
                cardAuthenticationData.credentialId = face.id
                cardAuthenticationData.nameUUID = face.nameUUID
                cardAuthenticationData.name = ""
                cardAuthenticationData.type = face.type
                cardCredentialList.add(cardAuthenticationData)
            }
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = cardCredentialList
            )
            getFaceCapable()?.getFaceDataSyncCapable()?.postAuthenticationData(request) { result ->
                result.onSuccess {
                    val res = it.data
                    L.d(tag, "postListToServer: $res")
                    val resultCardList = mutableListOf<CHSesameTouchFace>()
                    res.forEach {
                        resultCardList.add(CHSesameTouchFace(it.credentialId, it.name, it.type, it.nameUUID))
                    }
                    if (resultCardList.isNotEmpty()) {
                        runOnUiThread {
                            mFaceList.clear()
                            mFaceList.addAll(resultCardList)
                            bind.leaderboardList.adapter?.notifyDataSetChanged()
                            updateFaceCountDisplay()
                        }
                    } else {
                        L.d(tag, "postListToServer: res is empty")
                    }
                }
                result.onFailure { error ->
                    L.d(tag, "postListToServer error: $error")
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    /**
     * 更新人脸列表
     */
    private fun updateFaceList(face: CHSesameTouchFace) {
        val index = mFaceList.indexOf(face)
        if (index != -1) {
            // 更新现有项
            mFaceList[index] = face
            bind.leaderboardList.adapter?.notifyItemChanged(index)
        } else {
            // 添加新项
            mFaceList.add(0, face)
            updateFaceCountDisplay()
            bind.leaderboardList.adapter?.notifyDataSetChanged()
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        getBiometricBase()?.let { bind.emptyView.text = getString(R.string.sesamefaceMsg, it.productModel.modelName()) }
        bind.leaderboardList.adapter = createFaceAdapter()
    }

    /**
     * 创建人脸适配器
     */
    private fun createFaceAdapter(): GenericAdapter<CHSesameTouchFace> {
        return object : GenericAdapter<CHSesameTouchFace>(mFaceList) {
            override fun getLayoutId(position: Int, obj: CHSesameTouchFace): Int {
                return R.layout.cell_suica
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(view), Binder<CHSesameTouchFace> {
                override fun bind(data: CHSesameTouchFace, position: Int) {
                    setupFaceItem(itemView, data)
                }
            }
        }
    }

    /**
     * 设置人脸列表项
     */
    private fun setupFaceItem(itemView: View, data: CHSesameTouchFace) {
        val title = itemView.findViewById<TextView>(R.id.title)
        val name = itemView.findViewById<TextView>(R.id.sub_title)
        val image = itemView.findViewById<ImageView>(R.id.image)

        // 设置数据
        title.text = (data.id.hexStringToByteArray()[0].toUInt().toInt() + 1).toString().padStart(3, '0')
        name.text = if (data.name == "") getString(R.string.default_face_name) else data.name
        image.setImageResource(R.drawable.camera_face)

        // 设置点击事件
        itemView.setOnClickListener {
            showFaceActionDialog(data, title.text.toString(), name.text.toString())
        }
    }

    /**
     * 显示人脸操作对话框
     */
    private fun showFaceActionDialog(data: CHSesameTouchFace, titleText: String, nameText: String) {
        AlertView(titleText, "", AlertStyle.IOS).apply {
            // 添加重命名操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProFaceRename), AlertActionStyle.DEFAULT
                ) { _ ->
                    showFaceRenameDialog(data, nameText)
                })

            // 添加删除操作
            addAction(
                AlertAction(
                    getString(R.string.DelFace), AlertActionStyle.NEGATIVE
                ) { _ ->
                    deleteFace(data)
                })

            show(activity as? AppCompatActivity ?: return)
        }
    }

    /**
     * 显示人脸重命名对话框
     */
    private fun showFaceRenameDialog(data: CHSesameTouchFace, currentName: String) {
        context?.inputTextAlert("", currentName, data.name) {
            confirmButtonWithText("OK") { _, name ->
                dismiss()
                renameFace(data, name)
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    /**
     * 重命名人脸
     */
    private fun renameFace(data: CHSesameTouchFace, newName: String) {
        L.d("harry", "id: ${data.id}; oldName: ${data.name}; newName: $newName; oldNameLength: ${data.name.length}; faceNameUUID: ${data.nameUUID}")
        setFaceName(data, newName, getDeviceUUID())
    }

    /**
     * 删除人脸
     */
    private fun deleteFace(data: CHSesameTouchFace) {
        getFaceCapable()?.faceDelete(data.id, getDeviceUUID()) {}
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 获取人脸列表
        getFaceCapable()?.faceListGet {}

        // 获取当前模式
        getCurrentFaceMode()
    }

    /**
     * 获取当前人脸模式
     */
    private fun getCurrentFaceMode() {
        getFaceCapable()?.faceModeGet { result ->
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
            getFaceCapable()?.faceModeSet(MODE_CONTROL) {}
        }

        // 取消注册代理
        if (::faceDelegate.isInitialized) {
            getBiometricBase()?.let { biometricBase ->
                getFaceCapable()?.unregisterEventDelegate(biometricBase, faceDelegate)
            }
        }
    }

    /**
     * 获取人脸能力
     */
    private fun getFaceCapable(): CHFaceCapable? {
        return mDeviceModel.ssmLockLiveData.value as? CHFaceCapable
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
