package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpFpListBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest
import co.candyhouse.sesame.server.dto.ChSubCfp
import co.candyhouse.sesame.utils.L
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.hexStringToByteArray
import co.utils.noHashtoUUID
import co.utils.recycle.GenericAdapter
import kotlin.experimental.and


data class FingerPrint(var id: String, var name: String, val type: Byte, val fingerPrintNameUUID: String)

class SSMTouchProFingerprint : BaseDeviceFG<FgSsmTpFpListBinding>() {
    var mFingers = ArrayList<FingerPrint>()
    lateinit var fingerprintDelegate: CHFingerPrintDelegate
    override fun getViewBinder() = FgSsmTpFpListBinding.inflate(layoutInflater)


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
        L.d("harry", "getFingerPrintName: $fingerPrintNameUUID")
        AWSStatus.getSubUUID()?.let { it ->
            (mDeviceModel.ssmLockLiveData.value as CHFingerPrintCapable).fingerPrintNameGet(fingerPrintID, fingerPrintNameUUID, it, deviceUUID) {
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
        } ?: run { // 未登录用户， 没有 subUUID
            L.d("harry", "getCardName: AWSStatus.getSubUUID() is null")
            // 如果没有 AWSStatus.getSubUUID()， 直接使用默认名称
            val name: String = getString(R.string.default_fingerprint_name)
            runOnUiThread {
                mFingers.remove(mFingers.find { it.id == fingerPrintID })
                mFingers.add(0, FingerPrint(fingerPrintID, name, type, fingerPrintNameUUID))
                updateFingerPrintList()
            }
        }
    }

    private fun isUUIDv4(name: String?): Boolean {
        // 如果输入为 null，直接返回 false
        if (name == null) return false

        // 十六进制字符串转换为字节数组
        val byteArray = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        // UUIDv4 的固定长度为 16 字节
        if (byteArray.size != 16) return false

        // 定义 UUIDv4 的版本号和变体的字节值
        // 版本号：第7字节的高4位为0100 (即 0x40)
        // 变体：第9字节的高2位为10 (即 0x80)
        val uuidVersionByte = 0x40.toByte() // UUIDv4 版本号
        val uuidVariantByte = 0x80.toByte() // UUIDv4 变体

        // 检查版本号和变体是否符合 UUIDv4 标准
        return (byteArray[6].and(0xF0.toByte()) == uuidVersionByte) && (byteArray[8].and(0xC0.toByte()) == uuidVariantByte)
    }

    private fun setFingerPrintName(data: FingerPrint, name: String, deviceUUID: String) {
        AWSStatus.getSubUUID()?.let { it ->
            val fingerPrintNameRequest = CHFingerPrintNameRequest(
                fingerPrintNameUUID = data.fingerPrintNameUUID,
                subUUID = it,
                stpDeviceUUID = deviceUUID,
                name = name,
                fingerPrintID = data.id,
                type = data.type,
            )
            (mDeviceModel.ssmLockLiveData.value as CHFingerPrintCapable).fingerPrintNameSet(fingerPrintNameRequest) { it ->
                it.onSuccess {
                    L.d("harry", "fingerPrintNameSet: ${it.data}")
                    if (it.data == "Ok") {
                        runOnUiThread {
                            mFingers.add(0, FingerPrint(data.id, name, data.type, data.fingerPrintNameUUID))
                            updateFingerPrintList()
                        }
                    } else {
                        L.d("harry", "fingerPrintNameSet error: ${it.data}")
                    }
                }
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
                if (name.isEmpty()) { // 没有名字， 使用默认名称
                    runOnUiThread {
                        mFingers.remove(mFingers.find { it.id == ID })
                        mFingers.add(0, FingerPrint(ID, getString(R.string.default_fingerprint_name), type, ""))
                        updateFingerPrintList()
                    }
                } else { // 有名字
                    L.d("harry", "onFingerPrintReceive : name.length = ${name.length}, name: $name")
                    if ((name.length == 32) && isUUIDv4(name)) { // 是 uuid4 格式的名字， 到服务器上拿名字
                        // 把name转成uuid
                        val fingerPrintNameUUID = name.noHashtoUUID().toString()
                        getFingerPrintName(fingerPrintNameUUID, ID, type, getDeviceUUID()) // TODO: onFingerPrintReceiveEnd 批量到服务器上一次拿完。
                    } else { // 不是 uuid 格式的名字
                        runOnUiThread {
                            val byteArray = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray() // 从十六进制字符串， 转成字节数组
                            val oldName = byteArray.toString(Charsets.UTF_8)
                            mFingers.remove(mFingers.find { it.id == ID })
                            mFingers.add(0, FingerPrint(ID, oldName, type, ""))
                            updateFingerPrintList()
                        }
                    }
                }
            }

            override fun onFingerPrintReceiveEnd(device: CHSesameConnector) {
                runOnUiThread {
                    bind.swiperefresh.isRefreshing = false
                    updateFingerPrintList()

                    isUpload = true
                    val list = mFingers.map { ChSubCfp(it.id, it.name ?: "") }
                    addAllChangeCfp("f", ArrayList(list), true)
                }
            }

            override fun onFingerPrintChanged(
                device: CHSesameConnector,
                ID: String,
                name: String,
                type: Byte
            ) {
                L.d("harry", "onFingerPrintChanged : name.length = ${name.length}, name: $name")
                if (name.isEmpty()) { // 没有名字， 使用默认名称
                    runOnUiThread {
                        mFingers.remove(mFingers.find { it.id == ID })
                        mFingers.add(0, FingerPrint(ID, getString(R.string.default_fingerprint_name), type, ""))
                        updateFingerPrintList()
                    }
                } else { // 有名字
                    L.d("harry", "onFingerPrintChanged : name.length = ${name.length}, name: $name")
                    if ((name.length == 32) && isUUIDv4(name)) { // 是 uuid4 格式的名字， 到服务器上拿名字
                        // 把name转成uuid
                        val fingerPrintNameUUID = name.noHashtoUUID().toString()
                        getFingerPrintName(fingerPrintNameUUID, ID, type, getDeviceUUID())
                    } else { // 不是 uuid 格式的名字
                        runOnUiThread {
                            val byteArray = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray() // 从十六进制字符串， 转成字节数组
                            val oldName = byteArray.toString(Charsets.UTF_8)
                            mFingers.remove(mFingers.find { it.id == ID })
                            mFingers.add(0, FingerPrint(ID, oldName, type, ""))
                            updateFingerPrintList()
                        }
                    }
                }
                addDelChangeCfp(ID, name, "f", true) // 往云端传数据， 给 biz 用的， 不要放到 UI 线程里
            }

            override fun onFingerModeChange(device: CHSesameConnector, mode: Byte) {
                updateModeUI(mode)
            }

            override fun onFingerDelete(device: CHSesameConnector, ID: String) {
                L.d("hcia", "onFingerDelete : $ID")
                view?.post {
                    mFingers.remove(mFingers.find { it.id == ID })
                    updateFingerPrintList()
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
        title.text = (data.id.hexStringToByteArray()[0].toUInt().toInt() + 1).toString()
            .padStart(3, '0')

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
        mFingers.remove(data)
        if (data.fingerPrintNameUUID == "") { // 如果没有UUID， 直接使用新的名字, 走蓝牙。 兼容旧的刷卡机固件。
            getFingerPrintCapable()?.fingerPrintsChange(data.id, newName) {}
        } else {
            setFingerPrintName(data, newName, getDeviceUUID())
        }
    }

    /**
     * 删除指纹
     */
    private fun deleteFingerPrint(data: FingerPrint) {
        getFingerPrintCapable()?.fingerPrintDelete(data.id) {
            view?.post {
                mFingers.remove(data)
                updateFingerPrintList()
            }
            addDelChangeCfp(data.id, "", "f", false)
        }
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