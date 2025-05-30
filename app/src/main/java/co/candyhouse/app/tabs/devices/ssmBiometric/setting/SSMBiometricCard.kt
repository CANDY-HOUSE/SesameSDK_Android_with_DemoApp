package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpCardListBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.ChSubCfp
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.noHashtoUUID
import co.utils.recycle.GenericAdapter
import co.utils.recycle.GenericAdapter.Binder
import kotlin.experimental.and

data class SuiCard(var id: String, var name: String, val cardType: Byte, val cardNameUUID: String)

fun SuiCard.setCardType(level: Int) {
    SharedPreferencesUtils.preferences.edit().putInt(this.id, level).apply()
}

fun SuiCard.getCardType(level: Int): Int {
    return SharedPreferencesUtils.preferences.getInt(this.id, level)
}

/**
 * 卡片管理界面
 */
class SesameNfcCards : BaseDeviceFG<FgSsmTpCardListBinding>() {

    // 常量定义
    companion object {
        private const val MAX_CARD_COUNT = 1000
        private const val MODE_CONTROL = 0.toByte()
        private const val MODE_REGISTER = 1.toByte()

        // 卡片类型
        private const val CARD_TYPE_SUICA = 1
        private const val CARD_TYPE_PASMO = 2
        private const val CARD_TYPE_OTHER = 0
    }

    // 数据
    private var mCardList = ArrayList<SuiCard>()
    private lateinit var cardDelegate: CHCardDelegate

    override fun getViewBinder() = FgSsmTpCardListBinding.inflate(layoutInflater)

    private fun subscribeNameUpdateTopic(){
        getBiometricBase()?.subscribeNameUpdateTopic(cardDelegate)
    }

    private fun unsubscribeNameUpdateTopic(){
        getBiometricBase()?.unsubscribeNameUpdateTopic()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupEventListeners()
        setupCardDelegate()
        setupRecyclerView()
        loadInitialData()
        subscribeNameUpdateTopic()
    }

    override fun onDestroyView() {
        unsubscribeNameUpdateTopic()
        cleanupResources()
        super.onDestroyView()
    }

    /**
     * 初始化UI组件
     */
    private fun initializeUI() {
        bind.swiperefresh.isEnabled = false
        getBiometricBase()?.let {
            val name = it.productModel.modelName()
            bind.emptyView.text = getString(R.string.TouchEmptyNFCHint, name, name)
        }
        updateCardCountDisplay()
    }

    /**
     * 更新卡片数量显示
     */
    private fun updateCardCountDisplay() {
        runOnUiThread {
            bind.menuTitle.text = "${mCardList.size}/$MAX_CARD_COUNT"
            bind.leaderboardList.adapter?.notifyDataSetChanged()
        }
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 添加模式按钮
        bind.imgModeAdd.setOnClickListener {
            setCardMode(MODE_CONTROL)
        }

        // 验证模式按钮
        bind.imgModeVerify.setOnClickListener {
            setCardMode(MODE_REGISTER)
        }
    }

    /**
     * 设置卡片模式
     */
    private fun setCardMode(mode: Byte) {
        getCardCapable()?.cardModeSet(mode) { result ->
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
     * 设置卡片代理
     */
    private fun setupCardDelegate() {
        cardDelegate = createCardDelegate()

        getCardCapable()?.let { cardCapable ->
            getBiometricBase()?.let { biometricBase ->
                cardCapable.registerEventDelegate(biometricBase, cardDelegate)
            }
        }
    }

    // 异步操作， 获取服务器上的名字， 然后刷新 UI
    private fun getCardName(cardNameUUID: String, cardID: String, type: Byte, deviceUUID: String) {
        L.d("harry", "getCardName: $cardNameUUID")
        AWSStatus.getSubUUID()?.let {
            (mDeviceModel.ssmLockLiveData.value as CHCardCapable).cardNameGet(cardID.uppercase(), cardNameUUID, it, deviceUUID) { it ->
                L.d("harry", "cardName: $it")
                it.onSuccess {
                    L.d("harry", "cardName:${it.data}")
                    val name: String = if (it.data == "") {
                        getString(R.string.default_card_name)
                    } else {
                        it.data
                    }
                    runOnUiThread {
                        mCardList.remove(mCardList.find { it.id == cardID })
                        mCardList.add(0, SuiCard(cardID, name, type, cardNameUUID))
                        updateCardCountDisplay()
                    }
                }
            }
        } ?: run {
            L.d("harry", "getCardName: AWSStatus.getSubUUID() is null")
            // 如果没有 AWSStatus.getSubUUID()， 直接使用默认名称
            val name: String = getString(R.string.default_card_name)
            runOnUiThread {
                mCardList.remove(mCardList.find { it.id == cardID })
                mCardList.add(0, SuiCard(cardID, name, type, cardNameUUID))
                updateCardCountDisplay()
            }
        }
    }

    private fun isCardNameUUIDv4(name: String?): Boolean {
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

    private fun setCardName(data: SuiCard, name: String, deviceUUID: String) {
        AWSStatus.getSubUUID()?.let { it ->
            val cardNameRequest = CHCardNameRequest(
                cardType = data.cardType,
                cardNameUUID = data.cardNameUUID,
                subUUID = it,
                stpDeviceUUID = deviceUUID,
                name = name,
                cardID = data.id.uppercase(),
            )
            (mDeviceModel.ssmLockLiveData.value as CHCardCapable).cardNameSet(cardNameRequest) { it ->
                it.onSuccess {
                    L.d("harry", "cardNameSet: ${it.data}")
                    if (it.data == "Ok") {
                        runOnUiThread {
                            mCardList.add(0, SuiCard(data.id, name, data.cardType, data.cardNameUUID))
                            updateCardCountDisplay()
                        }
                    } else {
                        L.d("harry", "cardNameSet error: ${it.data}")
                    }
                }
            }
        }
    }

    /**
     * 创建卡片代理
     */
    private fun createCardDelegate(): CHCardDelegate {
        return object : CHCardDelegate {
            override fun onCardReceiveStart(device: CHSesameConnector) {
                runOnUiThread {
                    mCardList.clear()
                    bind.swiperefresh.isRefreshing = true
                }
            }

            override fun onCardReceive(
                device: CHSesameConnector,
                cardID: String,
                name: String,
                type: Byte
            ) {
                L.d("harry", "【onCardReceive】 cardID: $cardID; cardName: $name; cardNameLength: ${name.length}")
                if (name.isEmpty()) { // 没有名字， 使用默认名称
                    runOnUiThread {
                        mCardList.remove(mCardList.find { it.id == cardID })
                        mCardList.add(0, SuiCard(cardID, getString(R.string.default_card_name), type, ""))
                        updateCardCountDisplay()
                    }
                } else { // 有名字
                    L.d("harry", "cardName: $name")
                    if ((name.length == 32) && isCardNameUUIDv4(name)) { // 是 uuid 格式的名字
                        // 如果cardName是uuid, 去服务器上查询卡片名称
                        L.d("harry", "cardName is uuid")

                        // 把name转成uuid
                        val cardNameUUID = name.noHashtoUUID().toString()

                        // 获取服务器上的名字， 并等待 收到服务器数据后 刷新 UI
                        getCardName(cardNameUUID, cardID, type, getDeviceUUID())
                    } else { // 不是 uuid 格式的名字
                        // 如果cardName不是uuid, 使用 刷卡机提供的名字， 兼容旧固件。
                        L.d("harry", "cardName is not uuid. cardName: $name")
                        val byteArray = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray() // 从十六进制字符串， 转成字节数组
                        val oldName = byteArray.toString(Charsets.UTF_8)

                        // 在日志中打印所有卡片信息
                        // L.d("harry", "Current card list: " + mCardList.size)
                        // for( card in mCardList) {
                        //     L.d("harry", "card: ${card.id}, name: ${card.name}, type: ${card.cardType}")
                        // }

                        runOnUiThread {
                            mCardList.remove(mCardList.find { it.id == cardID })
                            mCardList.add(0, SuiCard(cardID, oldName, type, ""))
                            updateCardCountDisplay()
                        }
                    }
                }
            }

            override fun onCardReceiveEnd(device: CHSesameConnector) {
                runOnUiThread {
                    bind.swiperefresh.isRefreshing = false
                    isUpload = true
                    val list = mCardList.map { ChSubCfp(it.id, it.name) }
                    updateCardCountDisplay()
                    addAllChangeCfp("c", ArrayList(list), true)
                }
            }

            override fun onCardChanged(
                device: CHSesameConnector,
                cardID: String,
                name: String,
                type: Byte
            ) {
                L.d("harry", "onCardChanged : $cardID; cardName: $name; cardNameLength: ${name.length}")

                if (name.isEmpty()) { // 没有名字， 使用默认名称
                    runOnUiThread {
                        mCardList.remove(mCardList.find { it.id == cardID })
                        mCardList.add(0, SuiCard(cardID, getString(R.string.default_card_name), type, ""))
                        updateCardCountDisplay()
                    }
                } else { // 有名字
                    L.d("harry", "cardName: $name")
                    // TODO: 确认 Biz 加的新卡片也一定没名字？ 可以不用到服务器上拿。
                    if ((name.length == 32) && isCardNameUUIDv4(name)) { // 是 uuid 格式的名字
                        // 如果cardName是uuid, 去服务器上查询卡片名称
                        L.d("harry", "cardName is uuid")

                        // 把name转成uuid
                        val cardNameUUID = name.noHashtoUUID().toString()

                        // 获取服务器上的名字， 并等待 收到服务器数据后 刷新 UI
                        getCardName(cardNameUUID, cardID, type, getDeviceUUID()) // TODO: onCardReceiveEnd 批量到服务器上一次拿完。
                    } else { // 不是 uuid 格式的名字
                        // 如果cardName不是uuid, 使用 刷卡机提供的名字， 兼容旧固件。
                        L.d("harry", "cardName is not uuid. cardName: $name")
                        val byteArray = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray() // 从十六进制字符串， 转成字节数组
                        val oldName = byteArray.toString(Charsets.UTF_8)
                        runOnUiThread {
                            mCardList.remove(mCardList.find { it.id == cardID })
                            mCardList.add(0, SuiCard(cardID, oldName, type, ""))
                            updateCardCountDisplay()
                        }
                    }
                }
                addDelChangeCfp(cardID, name, "c", true)  // 这个是往云端放数据给 Biz 使用， 不要放在 UI 线程.
            }

            override fun onCardModeChanged(device: CHSesameConnector, mode: Byte) {
                updateModeUI(mode)
            }

            override fun onCardDelete(device: CHSesameConnector, cardID: String) {
                L.d("hcia", "onCardDelete : $cardID")
                runOnUiThread {
                    mCardList.remove(mCardList.find { it.id == cardID })
                    updateCardCountDisplay()
                }
            }
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        bind.leaderboardList.adapter = createCardAdapter()
    }

    /**
     * 创建卡片适配器
     */
    private fun createCardAdapter(): GenericAdapter<SuiCard> {
        return object : GenericAdapter<SuiCard>(mCardList) {
            override fun getLayoutId(position: Int, obj: SuiCard): Int =
                R.layout.cell_suica

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                CardViewHolder(view, this)
        }
    }

    /**
     * 卡片ViewHolder
     */
    private inner class CardViewHolder(
        view: View,
        private val adapter: GenericAdapter<SuiCard>
    ) : RecyclerView.ViewHolder(view), Binder<SuiCard> {

        override fun bind(data: SuiCard, pos: Int) {
            val image = itemView.findViewById<ImageView>(R.id.image)
            val title = itemView.findViewById<TextView>(R.id.title)
            val name = itemView.findViewById<TextView>(R.id.sub_title)

            // 处理ID长度
            if (data.id.length > 32) {
                data.id = data.id.substring(0, 32)
            }

            // 格式化UUID
            val uuidStr = data.id.padEnd(32, 'F').noHashtoUUID().toString()

            // 获取卡片类型并设置图标
            val cardType = data.getCardType(data.cardType.toInt())
            image.setImageResource(
                when (cardType) {
                    CARD_TYPE_SUICA -> R.drawable.suica
                    CARD_TYPE_PASMO -> R.drawable.pasmo
                    else -> R.drawable.small_icon
                }
            )

            // 设置图标点击事件
            image.setOnClickListener {
                data.setCardType((cardType + 1) % 3)
                adapter.notifyDataSetChanged()
            }

            // 设置文本
            title.text = uuidStr
            name.text = if (data.name.isEmpty())
                getString(R.string.default_card_name)
            else
                data.name

            // 设置点击事件
            itemView.setOnClickListener {
                showCardActionDialog(data, title.text.toString(), name.text.toString())
            }
        }
    }

    /**
     * 显示卡片操作对话框
     */
    private fun showCardActionDialog(data: SuiCard, titleText: String, nameText: String) {
        AlertView(titleText, "", AlertStyle.IOS).apply {
            // 添加重命名操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProCardRename),
                    AlertActionStyle.DEFAULT
                ) { _ ->
                    showCardRenameDialog(data, nameText)
                }
            )

            // 添加删除操作
            addAction(
                AlertAction(
                    getString(R.string.TouchProCardDelete),
                    AlertActionStyle.NEGATIVE
                ) { _ ->
                    deleteCard(data)
                }
            )

            show(activity as? AppCompatActivity ?: return)
        }
    }

    /**
     * 显示卡片重命名对话框
     */
    private fun showCardRenameDialog(data: SuiCard, currentName: String) {
        context?.inputTextAlert("", currentName, data.name) {
            confirmButtonWithText("OK") { _, name ->
                dismiss()
                renameCard(data, name)
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    /**
     * 重命名卡片
     */
    private fun renameCard(data: SuiCard, newName: String) {
        L.d("harry", "cardName: ${data.name}; newName: $newName; cardNameLength: ${data.name.length}; cardNameUUID: ${data.cardNameUUID}")
        mCardList.remove(data)
        if (data.cardNameUUID == "") { // 如果没有UUID， 直接使用新的名字, 走蓝牙。 兼容旧的刷卡机固件。
            getCardCapable()?.cardChange(data.id, newName) {}
        } else {
            setCardName(data, newName, getDeviceUUID())
        }
    }

    /**
     * 删除卡片
     */
    private fun deleteCard(data: SuiCard) {
        getCardCapable()?.cardDelete(data.id) {
            addDelChangeCfp(data.id, "", "c", false)
            runOnUiThread {
                mCardList.remove(data)
                updateCardCountDisplay()
            }
        }
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 获取当前模式
        getCardCapable()?.cardModeGet { result ->
            result.onSuccess { data ->
                updateModeUI(data.data)
            }
        }

        // 获取卡片数据
        getBiometricBase()?.let { biometricBase ->
            val deviceUUID = biometricBase.deviceId.toString().uppercase()

            getCardCapable()?.sendNfcCardsDataGetCmd(deviceUUID) { result ->
                result.onSuccess { data ->
                    L.d("harry", "cards res: ${data.data}")
                }
            }
        }
    }

    /**
     * 清理资源
     */
    private fun cleanupResources() {
        // 设置默认模式
        getCardCapable()?.cardModeSet(MODE_CONTROL) {}

        // 取消注册代理
        if (::cardDelegate.isInitialized) {
            getBiometricBase()?.let { biometricBase ->
                getCardCapable()?.unregisterEventDelegate(biometricBase, cardDelegate)
            }
        }
    }

    /**
     * 从输入字符串中提取请求ID
     */
    private fun extractRequestId(input: String): String? {
        val regex = "\\{request_id=([a-f0-9\\-]+)\\}".toRegex()
        val matchResult = regex.find(input)
        return matchResult?.groups?.get(1)?.value
    }

    /**
     * 获取卡片能力
     */
    private fun getCardCapable(): CHCardCapable? {
        return mDeviceModel.ssmLockLiveData.value as? CHCardCapable
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
