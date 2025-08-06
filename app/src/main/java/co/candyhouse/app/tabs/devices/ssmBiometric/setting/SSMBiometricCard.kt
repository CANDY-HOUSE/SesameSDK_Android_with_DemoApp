package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpCardListBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.AuthenticationData
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.isUUIDv4
import co.utils.noHashtoUUID
import co.utils.recycle.GenericAdapter
import co.utils.recycle.GenericAdapter.Binder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

data class SuiCard(var id: String, var name: String, var cardType: Byte, var cardNameUUID: String)

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
    private val operationType = "nfc_card"
    private val tag = "SesameNfcCards"

    override fun getViewBinder() = FgSsmTpCardListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
        setupEventListeners()
        setupCardDelegate()
        setupRecyclerView()
        loadInitialData()
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

        if (BuildConfig.DEBUG) {
            bind.menuTitle.setOnLongClickListener {
                addCardToSTP()
                true
            }
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

    private fun addCardToSTP() {
        getCardCapable()?.cardAdd(byteArrayOf(0x7c.toByte(), 0x47.toByte(), 0x72.toByte(), 0xdf.toByte()), "4 bytes NFC") { result ->
            result.onSuccess {
                L.d(tag, "addCardToSTP success")
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
        AWSStatus.getSubUUID()?.let {
            (mDeviceModel.ssmLockLiveData.value as CHCardCapable).cardNameGet(cardID.uppercase(), cardNameUUID, it, deviceUUID) { it ->
                it.onSuccess {
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
            // 如果没有 AWSStatus.getSubUUID()， 直接使用默认名称
            val name: String = getString(R.string.default_card_name)
            runOnUiThread {
                mCardList.remove(mCardList.find { it.id == cardID })
                updateCardList(SuiCard(cardID, name, type, cardNameUUID))
                updateCardCountDisplay()
            }
        }
    }

    private fun setCardName(data: SuiCard, name: String, deviceUUID: String) {
        if (!data.cardNameUUID.isUUIDv4()) {
            val uuid = UUID.randomUUID().toString().lowercase()
            getCardCapable()?.cardChange(data.id, uuid.replace("-", "")) { result ->
                result.onSuccess { res ->
                    data.cardNameUUID = uuid
                    executeCardNameSet(data, name, deviceUUID)
                }
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        } else {
            executeCardNameSet(data, name, deviceUUID)
        }
    }

    private fun executeCardNameSet(data: SuiCard, name: String, deviceUUID: String) {
        val cardNameRequest = CHCardNameRequest(
            cardType = data.cardType,
            cardNameUUID = data.cardNameUUID,
            subUUID = UserUtils.getUserId() ?: "",
            stpDeviceUUID = deviceUUID,
            name = name,
            cardID = data.id,
        )
        getCardCapable()?.cardNameSet(cardNameRequest) { it ->
            it.onSuccess {
                if (it.data == "Ok") {
                    runOnUiThread {
                        data.name = name
                        updateCardList(data)
                    }
                }
            }
            it.onFailure {
                lifecycleScope.launch(Dispatchers.Main) { toastMSG(it.message) }
            }
        }
    }

    private fun updateCardList(card: SuiCard) {
        val index = mCardList.indexOfFirst { it.id.lowercase().equals(card.id.lowercase()) }
        if (index != -1) {
            mCardList[index] = card
        } else {
            mCardList.add(0, SuiCard(card.id, card.name, card.cardType, card.cardNameUUID))
        }
        updateCardCountDisplay()
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
                // 直接显示从BLE得到的卡片。收完数据后，批量获取卡片名称
                runOnUiThread {
                    mCardList.remove(mCardList.find { it.id == cardID })
                    if ((name.length == 32) && name.isUUIDv4()) { // 是 uuid 格式的名字
                        mCardList.add(0, SuiCard(cardID, getString(R.string.default_card_name), type, name.noHashtoUUID().toString()))
                    } else {
                        val matchName = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
                        mCardList.add(0, SuiCard(cardID, matchName, type, matchName))
                    }
                    updateCardCountDisplay()
                }
            }

            override fun onCardReceiveEnd(device: CHSesameConnector) {
                runOnUiThread {
                    bind.swiperefresh.isRefreshing = false
                    isUpload = true
                    updateCardCountDisplay()
                    postCardListToServer(mCardList)
                }
            }

            override fun onCardChanged(
                device: CHSesameConnector,
                cardID: String,
                name: String,
                type: Byte
            ) {
                val newCard = if ((name.length == 32) && name.isUUIDv4()) { // 是 uuid 格式的名字
                    SuiCard(cardID, getString(R.string.default_card_name), type, name.noHashtoUUID().toString())
                } else {
                    val matchName = name.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
                    SuiCard(cardID, matchName, type, matchName)
                }
                updateCardList(newCard)
                runOnUiThread {
                    updateCardCountDisplay()
                    addCardToServer(cardID, newCard.cardNameUUID, type)
                }
            }

            override fun onCardModeChanged(device: CHSesameConnector, mode: Byte) {
                updateModeUI(mode)
            }

            override fun onCardDelete(device: CHSesameConnector, cardID: String) {
                L.d("hcia", "onCardDelete : $cardID")
                val card = mCardList.find { it.id.uppercase() == cardID.uppercase() }
                if (card != null) {
                    runOnUiThread {
                        mCardList.remove(card)
                        bind.leaderboardList.adapter?.notifyDataSetChanged()
                        updateCardCountDisplay()
                    }
                    deleteCardFromServer(card)
                }
            }
        }
    }

    private fun deleteCardFromServer(card: SuiCard) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val credentialList = mutableListOf<AuthenticationData>()
            val cardAuthenticationData = AuthenticationData()
            cardAuthenticationData.credentialId = card.id
            cardAuthenticationData.nameUUID = card.cardNameUUID
            cardAuthenticationData.type = card.cardType
            credentialList.add(cardAuthenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = credentialList
            )
            getCardCapable()?.getCardDataSyncCapable()?.deleteAuthenticationData(request) { result ->
                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun addCardToServer(cardID: String, nameUUID: String, type: Byte) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cardCredentialList = mutableListOf<AuthenticationData>()
            val cardAuthenticationData = AuthenticationData()
            cardAuthenticationData.credentialId = cardID
            cardAuthenticationData.nameUUID = nameUUID
            cardAuthenticationData.type = type
            cardCredentialList.add(cardAuthenticationData)
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = cardCredentialList
            )
            getCardCapable()?.getCardDataSyncCapable()?.putAuthenticationData(request) {
                it.onSuccess {
                    L.d(tag, "addCardToServer success")
                }
                it.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) { toastMSG(error.message) }
                }
            }
        }
    }

    private fun getCardNameInNeed(device: CHSesameConnector, cardID: String, name: String, type: Byte) {
        if ((name.length == 32) && name.isUUIDv4()) { // 如果是 UUID 格式的名字
            val cardNameUUID = name.noHashtoUUID().toString()
            getCardName(cardNameUUID, cardID, type, getDeviceUUID())
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
        setCardName(data, newName, getDeviceUUID())
    }

    /**
     * 删除卡片
     */
    private fun deleteCard(data: SuiCard) {
        viewLifecycleOwner.lifecycleScope.launch {
            val subId = AWSStatus.getSubUUID()

            val subUUID = UserUtils.getUserId()
            getCardCapable()?.cardDelete(data.id) {}
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

    private fun postCardListToServer(cardList: ArrayList<SuiCard>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cardAuthenticationList = mutableListOf<AuthenticationData>()
            cardList.forEach { card ->
                val cardAuthenticationData = AuthenticationData()
                cardAuthenticationData.credentialId = card.id
                cardAuthenticationData.nameUUID = card.cardNameUUID
                cardAuthenticationData.name = ""
                cardAuthenticationData.type = card.cardType
                cardAuthenticationList.add(cardAuthenticationData)
            }
            val request = AuthenticationDataWrapper(
                operation = operationType,
                deviceID = getDeviceUUID(),
                credentialList = cardAuthenticationList
            )
            getCardCapable()?.getCardDataSyncCapable()?.postAuthenticationData(request) { result ->
                result.onSuccess {
                    val res = it.data
                    L.d(tag, "postCardListToServer: $res")
                    val resultCardList = mutableListOf<SuiCard>()
                    res.forEach {
                        resultCardList.add(SuiCard(it.credentialId, it.name, it.type, it.nameUUID))
                    }
                    if (res.isNotEmpty()) {
                        runOnUiThread {
                            mCardList.clear()
                            mCardList.addAll(resultCardList)
                            updateCardCountDisplay()
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

    private fun getDeviceUUID(): String {
        return (mDeviceModel.ssmLockLiveData.value as CHSesameBiometricBase).deviceId.toString().uppercase()
    }
}
