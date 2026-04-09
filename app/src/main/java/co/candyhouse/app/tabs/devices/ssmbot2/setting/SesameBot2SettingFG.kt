package co.candyhouse.app.tabs.devices.ssmbot2.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSsmBikeBot2settingBinding
import co.candyhouse.app.ext.BotScriptStore
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.utils.alertview.fragments.toastMSG
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SesameBot2SettingFG : BaseDeviceSettingFG<FgSsmBikeBot2settingBinding>() {
    private val tag = "SesameBot2SettingFG"

    //检查类型再转换（来自firebase crash）
    private val bot2 by lazy {
        val device = mDeviceModel.ssmLockLiveData.value
        if (device is CHSesameBot2) {
            device
        } else {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("expected_type", "CHSesameBot2")
                setCustomKey("actual_type", device!!.javaClass.simpleName)
                setCustomKey("device_uuid", device.deviceId.toString())
                log("Wrong device type in SesameBot2SettingFG: ${device.javaClass.simpleName}")
            }
            toastMSG("デバイスタイプが一致しません。リストを更新してください。")
            findNavController().navigateUp()
            null
        }
    }

    private val bot2ScriptCurIndexKey by lazy {
        bot2?.deviceId.toString() + "_ScriptIndex"
    }

    private var hideWheelJob: Job? = null

    // 防抖隐藏函数（短延迟，不是倒计时超时）
    private fun hideWheelWithDebounce(delayMs: Long = 300L) {
        hideWheelJob?.cancel()
        hideWheelJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(delayMs)
            if (!isAdded) return@launch
            bind.wheelview.visibility = View.GONE
            updateScriptArrow(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (bot2 == null) {
            return
        }
        (mDeviceModel.ssmosLockDelegates[bot2!!]) = object : CHDeviceStatusDelegate {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
            }

            override fun onMechStatus(device: CHDevices) {
                if (device.deviceStatus.value == CHDeviceLoginStatus.logined && isAdded) {
                    refreshScriptNameListFromDevice()
                }
            }
        }.bindLifecycle(viewLifecycleOwner)
    }

    override fun getViewBinder() = FgSsmBikeBot2settingBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (bot2 == null) {
            return
        }
        super.onViewCreated(view, savedInstanceState)
        getView()?.findViewById<View>(R.id.ic_arrow_gray)?.setOnClickListener { findNavController().navigate(R.id.to_SesameClickScriptFG) }
        bind.llview.setOnClickListener {
            showWheel(false)
        }
        usePressText()
        bind.clickScriptArea.setOnClickListener {
            showWheel(!bind.wheelview.isVisible)
        }
        showWheel(false)

        renderWheelFromCache()

        bind.swiperefresh.addExcludedView(bind.wheelview)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadWheel(bot2: CHSesameBot2) {
        val sortedItems = getSortedScriptItems()
        bind.wheelview.setItems(sortedItems.map { it.displayName })

        val currentActionIndex = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
        val initPosition = sortedItems.indexOfFirst { it.actionIndex == currentActionIndex }
            .takeIf { it >= 0 } ?: 0

        bind.wheelview.setInitPosition(initPosition)
        bind.wheelview.setListener { selectPosition ->
            val selectedItem = sortedItems.getOrNull(selectPosition) ?: return@setListener
            val selectedActionIndex = selectedItem.actionIndex

            SharedPreferencesUtils.preferences.edit {
                putInt(bot2ScriptCurIndexKey, selectedActionIndex)
            }
            bind.scriptIdTxt.text = selectedItem.displayName
            bot2.selectScript(selectedActionIndex.toUByte()) { }

            val req = BotScriptRequest(
                deviceUUID = bot2.deviceId.toString().uppercase(),
                actionIndex = selectedActionIndex.toString(),
                alias = BotScriptStore.getAlias(bot2.deviceId.toString(), selectedActionIndex),
                isDefault = 1,
                actionData = null,
                displayOrder = BotScriptStore.getDisplayOrder(bot2.deviceId.toString(), selectedActionIndex) ?: selectedActionIndex
            )
            CHAPIClientBiz.updateBotScript(req) { result ->
                result.onSuccess { response ->
                    L.d(tag, "Response data: ${response.data}")
                }
                result.onFailure { error ->
                    L.e(tag, "Subscribe failed", error)
                }
            }

            hideWheelWithDebounce(300L)
        }
    }

    private fun showWheel(isShow: Boolean) {
        if (!isShow) hideWheelJob?.cancel()
        bind.wheelview.visibility = if (isShow) View.VISIBLE else View.GONE
        updateScriptArrow(isShow)
        if (isShow) {
            val curIdx = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
            val sortedItems = getSortedScriptItems()
            val wheelPosition = sortedItems.indexOfFirst { it.actionIndex == curIdx }
                .takeIf { it >= 0 } ?: 0
            bind.wheelview.setCurrentPosition(wheelPosition)
        }
    }

    private fun renderWheelFromCache() {
        val b = bot2 ?: return
        activity?.runOnUiThread {
            loadWheel(b)

            val curIdx = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
            bind.scriptIdTxt.text = displayNameForIndex(curIdx)

            val sortedItems = getSortedScriptItems()
            val wheelPosition = sortedItems.indexOfFirst { it.actionIndex == curIdx }.takeIf { it >= 0 } ?: 0
            bind.wheelview.setCurrentPosition(wheelPosition)
        }
    }

    private fun refreshScriptNameListFromDevice() {
        val b = bot2 ?: return
        b.getScriptNameList { rr ->
            rr.onSuccess {
                renderWheelFromCache()
            }
            rr.onFailure {
                L.w(tag, "getScriptNameList failed: ${it.message}")
            }
        }
    }

    private fun displayNameForIndex(index: Int): String {
        val b = bot2 ?: return index.toString()
        val deviceUUID = b.deviceId.toString()

        BotScriptStore.getAlias(deviceUUID, index)?.let { return it }

        val event = b.scripts.events.getOrNull(index)
        if (event != null) return String(event.name, Charsets.UTF_8)

        return index.toString()
    }

    private fun getSortedScriptItems(): List<BotScriptDisplayItem> {
        val b = bot2 ?: return emptyList()
        val deviceUUID = b.deviceId.toString()

        return b.scripts.events.mapIndexed { index, ev ->
            val fallback = String(ev.name, Charsets.UTF_8)
            val alias = BotScriptStore.getAlias(deviceUUID, index) ?: "🎬 $fallback"
            val order = BotScriptStore.getDisplayOrder(deviceUUID, index) ?: index

            BotScriptDisplayItem(
                actionIndex = index,
                displayName = alias,
                displayOrder = order
            )
        }.sortedBy { it.displayOrder }
    }

    private data class BotScriptDisplayItem(
        val actionIndex: Int,
        val displayName: String,
        val displayOrder: Int
    )

    private fun updateScriptArrow(isExpanded: Boolean) {
        bind.scriptExpandArrow.rotation = if (isExpanded) 90f else 0f
    }

}