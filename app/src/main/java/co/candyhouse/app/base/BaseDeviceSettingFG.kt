package co.candyhouse.app.base

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.clearNFC
import co.candyhouse.app.tabs.devices.ssm2.getFirmwareName
import co.candyhouse.app.tabs.devices.ssm2.getFirmwarePath
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNFC
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.level2Tag
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.setIsWidget
import co.candyhouse.app.tabs.devices.ssm2.setNFC
import co.candyhouse.app.tabs.devices.ssm2.setNickname
import co.candyhouse.app.tabs.devices.ssm2.setting.DfuService
import co.candyhouse.app.tabs.menu.EmbeddedWebView
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleStatusDelegate
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.safeNavigate
import com.amazonaws.mobile.client.AWSMobileClient
import kotlinx.coroutines.launch
import no.nordicsemi.android.dfu.DfuServiceInitiator

abstract class BaseDeviceSettingFG<T : ViewBinding> : BaseDeviceFG<T>(), NfcSetting,
    BleStatusUpdate, DeviceStatusChange {

    private var isToNotification = false
    private var isViewDestroyed = false

    private val refreshCounter = mutableIntStateOf(0)

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    override fun onResume() {
        super.onResume()
        onChange()

        CHBleManager.statusDelegate = object : CHBleStatusDelegate {
            override fun didScanChange(ss: CHScanStatus) {
                onChange()
            }
        }

        mDeviceModel.ssmosLockDelegates[mDeviceModel.ssmLockLiveData.value!!] =
            object : CHDeviceStatusDelegate {
                @SuppressLint("SetTextI18n")
                override fun onBleDeviceStatusChanged(
                    device: CHDevices,
                    status: CHDeviceStatus,
                    shadowStatus: CHDeviceStatus?
                ) {
                    L.d("[say]", "[BaseDeviceSettingFG.kt][onBleDeviceStatusChanged]")
                    onChange()
                    onUIDeviceStatus(status)
                    checkVersionTag(status, device)
                }

                @SuppressLint("SetTextI18n")
                override fun onMechStatus(device: CHDevices) {
                    view?.findViewById<TextView>(R.id.battery)?.post {
                        view?.findViewById<TextView>(R.id.battery)?.text =
                            "${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                    }
                }
            }.bindLifecycle(viewLifecycleOwner)

        checkTvSysNotifyWidget(isOnResume = true)
    }

    @SuppressLint("StringFormatMatches")
    fun usePressText() {
        val model = mDeviceModel.ssmLockLiveData.value
        model?.apply {
            view?.findViewById<TextView>(R.id.drop_hint_txt)?.text =
                resources.getString(R.string.drop_hint_press, model.productModel.modelName())
        }
    }

    open fun checkVersionTag(status: CHDeviceStatus, device: CHDevices) {
        if (status.value == CHDeviceLoginStatus.Login) {
            device.getVersionTag {
                it.onSuccess { va ->
                    if (isAdded && !isDetached) {
                        versionSet(device, va.data)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun versionSet(targetDevice: CHDevices, str: String) {
        if (targetDevice.productModel != CHProductModel.Hub3) {
            view?.findViewById<View>(R.id.device_version_txt)?.post {
                val zipname: String? = targetDevice.getFirmwareName()
                L.d(
                    "sf",
                    targetDevice.productModel.deviceModelName() + "------" + zipname
                )
                zipname?.apply {
                    val tailTagag = str.split("-").last()
                    val cheddd = zipname.contains(tailTagag)
                    view?.findViewById<TextView>(R.id.device_version_txt)?.text =
                        str + (if (cheddd) getString(R.string.latest) else "")
                    view?.findViewById<View>(R.id.alert_logo)?.visibility =
                        if (cheddd) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewDestroyed = false

        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ss2 ->
            getView()?.findViewById<TextView>(R.id.device_model)?.text =
                ss2.productModel.deviceModelName()
            getView()?.findViewById<TextView>(R.id.key_level_txt)?.text = level2Tag(ss2.getLevel())
            getView()?.findViewById<TextView>(R.id.name_txt)?.text = ss2.getNickname()
            getView()?.findViewById<TextView>(R.id.device_uuid_txt)?.text =
                ss2.deviceId.toString().uppercase()
            getView()?.findViewById<TextView>(R.id.histag_txt)?.text =
                ss2.getHistoryTag()?.let { String(it) }

            checkVersionTag(ss2.deviceStatus, ss2)
        }

        val targetDevice = mDeviceModel.ssmLockLiveData.value!!
        handleUI(targetDevice)
        setupListeners(targetDevice)
    }

    private fun setupListeners(targetDevice: CHDevices) {
        view?.findViewById<View>(R.id.change_name_zone)?.setOnClickListener {
            context?.inputTextAlert(
                "",
                getString(R.string.edit_name),
                SharedPreferencesUtils.preferences.getString(
                    targetDevice.deviceId.toString(),
                    targetDevice.getNickname()
                )
            ) {
                confirmButtonWithText("OK") { alert, name ->
                    targetDevice.setNickname(name)
                    CHLoginAPIManager.putKey(
                        cheyKeyToUserKey(
                            targetDevice.getKey(),
                            targetDevice.getLevel(),
                            targetDevice.getNickname()
                        )
                    ) {}
                    /* https://ap-northeast-1.console.aws.amazon.com/lambda/home?region=ap-northeast-1#/functions/user_device_put?tab=code
                    *  Line 91 - 105:  FunctionName: 'update_device_name_in_matter',
                    *  */
                    getView()?.findViewById<TextView>(R.id.name_txt)?.text = name
                    dismiss()
                }
                cancelButton(getString(R.string.cancel))
            }?.show()
        }
        view?.findViewById<View>(R.id.share_zone)?.setOnClickListener {
            AlertView(getString(R.string.share), "", AlertStyle.IOS).apply {
                val innerLevel = targetDevice.getLevel()
                if (innerLevel == 0) {
                    addAction(
                        AlertAction(
                            getString(R.string.owner) + getString(R.string.key),
                            AlertActionStyle.DEFAULT
                        ) {
                            mDeviceModel.targetShareLevel = 0
                            safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                        })
                }
                if (innerLevel == 0 || innerLevel == 1) {
                    addAction(
                        AlertAction(
                            getString(R.string.manager) + getString(R.string.key),
                            AlertActionStyle.DEFAULT
                        ) {
                            mDeviceModel.targetShareLevel = 1
                            safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                        })
                }
                addAction(
                    AlertAction(
                        getString(R.string.guest) + getString(R.string.key),
                        AlertActionStyle.DEFAULT
                    ) {
                        mDeviceModel.targetShareLevel = 2
                        safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.drop_zone)?.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(
                            R.string.trash_device_key,
                            targetDevice.productModel.modelName()
                        ), AlertActionStyle.NEGATIVE
                    ) {
                        mDeviceModel.dropDevice {
                            it.onSuccess {
                                if (isAdded && !isViewDestroyed) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        findNavController().popBackStack(R.id.deviceListPG, false)
                                    }
                                }
                            }
                            it.onFailure {
                                toastMSG(it.localizedMessage)
                                L.d("hcia", "無法刪除鑰匙倉庫:")
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.reset_zone)?.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(R.string.ssm_delete),
                        AlertActionStyle.NEGATIVE
                    ) {
                        mDeviceModel.resetDevice {
                            it.onSuccess {
                                if (isAdded) {
                                    findNavController().popBackStack(R.id.deviceListPG, false)
                                }
                            }
                            it.onFailure {
                                toastMSG(it.localizedMessage)
                                L.d("hcia", "無法刪除鑰匙倉庫:")
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.nfc_zone)?.setOnClickListener {
            if (targetDevice.getNFC()?.isEmpty() == false) {
                AlertView("", "", AlertStyle.IOS).apply {
                    addAction(
                        AlertAction(
                            getString(R.string.nfc_reset),
                            AlertActionStyle.NEGATIVE
                        ) {
                            targetDevice.clearNFC()
                            getView()?.findViewById<TextView>(R.id.nfc_id_txt)?.text =
                                getString(R.string.nfc_hint)
                        })
                    show(activity as AppCompatActivity)
                }
            }
        }
        view?.findViewById<View>(R.id.dfu_zone)?.setOnClickListener {
            //对齐iOS统一弹出升级对话框风格
            if (targetDevice.productModel != CHProductModel.Hub3
                && mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.UnLogin
            ) {
                toastMSG(getString(R.string.toastBleNotReadyForDFU))
                return@setOnClickListener
            }
            AlertView(getString(R.string.ssm_update), "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        "OK",
                        AlertActionStyle.NEGATIVE
                    ) {
                        L.d(
                            "sf",
                            "Hub3 update OTA: " + targetDevice.productModel.modelName()
                        )
                        if (targetDevice.productModel == CHProductModel.Hub3) {
                            L.d("sf", "hub3模块升级固件……")
                            mDeviceModel.ssmLockLiveData.value!!.updateFirmware { res ->
                                res.onSuccess {
                                    //(mDeviceModel.ssmLockLiveData.value!! as CHHub3).updateFirmware {}
                                }
                            }
                        } else {
                            L.d("sf", "非hub3模块升级固件……")
                            targetDevice.updateFirmware { res ->
                                L.d("hcia", "res:$res")
                                res.onSuccess {
                                    L.d("hcia", "updateFirmware:" + it.data.address)
                                    val firmwarePath = targetDevice.getFirmwarePath(requireContext())
                                    if (firmwarePath != null) {
                                        val starter = DfuServiceInitiator(it.data.address)
                                        starter.setZip(firmwarePath)
                                        starter.setPacketsReceiptNotificationsEnabled(true)
                                        starter.setPrepareDataObjectDelay(400)
                                        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                                        starter.setDisableNotification(false)
                                        starter.setForeground(false)
                                        starter.start(requireActivity(), DfuService::class.java)
                                    }
                                }
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleUI(targetDevice: CHDevices) {
        val level = targetDevice.getLevel()
        if (level == 2) {
            view?.findViewById<View>(R.id.share_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.chenge_angle_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.auto_lock_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.noti_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.opsensor_zone)?.visibility = View.GONE
        }
        targetDevice.getNFC()
            ?.apply { view?.findViewById<TextView>(R.id.nfc_id_txt)?.text = this }
        view?.findViewById<View>(R.id.reset_zone)?.visibility =
            if (BuildConfig.DEBUG) View.VISIBLE else View.GONE

        view?.findViewById<Switch>(R.id.widget_switch)?.apply {
            isChecked = targetDevice.getIsWidget()
            updataTargetDevice(isChecked, mDeviceModel.ssmLockLiveData.value)
            checkTvSysNotifyWidget(isCheck = isChecked)
            setOnCheckedChangeListener { view, isChecked ->
                targetDevice.setIsWidget(isChecked)
                updataTargetDevice(isChecked, mDeviceModel.ssmLockLiveData.value)
                checkTvSysNotifyWidget(isCheck = isChecked)
            }
        }

        view?.findViewById<RelativeLayout>(R.id.widget_rl)?.apply {
            setOnClickListener(null)
            setOnClickListener {
                openSettingNotify()
            }
        }

        view?.findViewById<TextView>(R.id.drop_hint_txt)?.text =
            getString(R.string.drop_hint, targetDevice.productModel.modelName())

        if (AWSMobileClient.getInstance().isSignedIn) {
            view?.findViewById<ComposeView>(R.id.friend_web_view)?.apply {
                disposeComposition()

                setContent {
                    EmbeddedWebView(
                        scene = "device-user",
                        deviceId = targetDevice.deviceId.toString().uppercase(),
                        height = 80.dp,
                        refreshTrigger = refreshCounter.intValue,
                        onSchemeIntercept = { uri, params ->
                            when (uri.path) {
                                "/webview/open" -> {
                                    params["url"]?.let { targetUrl ->
                                        params["notifyName"]?.let { notifyName ->
                                            L.d("EmbeddedWebView", "EmbeddedWebView-notifyName=$notifyName")
                                        }
                                        L.d("EmbeddedWebView", "EmbeddedWebView-targetUrl=$targetUrl")
                                        safeNavigate(R.id.action_DeviceMember_to_webViewFragment, Bundle().apply {
                                            putString("scene", "device-user")
                                            putString("url", targetUrl)
                                        })
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        updateFreshTop(targetDevice)
    }

    private fun updateFreshTop(targetDevice: CHDevices) {
        if (targetDevice.getLevel() == 0 || targetDevice.getLevel() == 1) {
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.setOnRefreshListener {
                refreshTop()
            }
        } else {
            view?.findViewById<RecyclerView>(R.id.friend_web_view)?.visibility = View.GONE
        }
    }

    private fun refreshTop() {
        view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.post {
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing = true
        }

        // 触发WebView刷新
        refreshCounter.intValue++

        view?.postDelayed({
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing = false
        }, 1500)
    }

    private fun updataTargetDevice(isChecked: Boolean, targetDevice: CHDevices?) {
        targetDevice?.apply {
            view?.findViewById<View>(R.id.no_hand_zone)?.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                targetDevice.setIsNOHand(false)
                if (!isAdded) return
                view?.findViewById<TextView>(R.id.auto_open_txt)?.text = getString(R.string.Off)
            }

            mDeviceModel.updateWidgets(this.deviceId.toString())
        }
    }

    override fun onNfcId(id: String) {
        requireActivity().runOnUiThread {
            mDeviceModel.ssmLockLiveData.value?.apply {
                this.setNFC(id)

                view?.apply {
                    findViewById<TextView>(R.id.nfc_id_txt)?.apply {
                        this.text = id
                    }
                }
            }
        }
    }

    override fun onChange() {
        L.d(
            "harry",
            "[onChange][status: " + mDeviceModel.ssmLockLiveData.value?.deviceStatus.toString() + "] [CHBleManager:  " + CHBleManager.getConnectRSize()
                .toString() + "]"
        )
        if (!isAdded) return
        when {
            CHBleManager.mScanning == CHScanStatus.BleClose -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.noble)
            }

            mDeviceModel.ssmLockLiveData.value!!.deviceStatus == CHDeviceStatus.NoBleSignal -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                if (CHBleManager.getConnectRSize() >= 7) {
                    view?.findViewById<TextView>(R.id.err_title)?.text =
                        getString(R.string.BleTooManyConnections)
                } else {
                    view?.findViewById<TextView>(R.id.err_title)?.text =
                        getString(R.string.NoBleSignal)
                }
            }

            mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.UnLogin -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text =
                    mDeviceModel.ssmLockLiveData.value!!.deviceStatus.toString()
            }

            else -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.GONE
            }
        }
    }

    private fun isNotifyEnable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = getSystemService(
                requireContext(),
                NotificationManager::class.java
            ) as NotificationManager
            return notificationManager.areNotificationsEnabled()
        } else {
            return true
        }
    }

    private fun openSettingNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.apply {
                val intent = Intent()
                isToNotification = true
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
                startActivity(intent)
            }
        }
    }

    private fun checkTvSysNotifyWidget(isCheck: Boolean = false, isOnResume: Boolean = false) {
        view?.findViewById<TextView>(R.id.tvSysNotifyWidget)?.apply {
            text =
                if (isNotifyEnable()) getString(R.string.android_notifica_permis_on) else getString(
                    R.string.android_notifica_permis_off
                )
            if (!isOnResume) {
                isEnabled = isCheck
            } else {
                if (isToNotification) {
                    isToNotification = false
                    updataTargetDevice(true, mDeviceModel.ssmLockLiveData.value)
                }
            }
            isSelected = isEnabled && !isNotifyEnable()
        }
    }

    fun checkTvSysNotifyMsg(isCheck: Boolean = false, isOnResume: Boolean = false) {
        view?.findViewById<View>(R.id.noti_zone)?.apply {
            setOnClickListener(null)
            setOnClickListener {
                openSettingNotify()
            }
        }
        view?.findViewById<TextView>(R.id.tvSysNotifyMsg)?.apply {
            text =
                if (isNotifyEnable()) getString(R.string.android_notifica_permis_on) else getString(
                    R.string.android_notifica_permis_off
                )
            if (!isOnResume) {
                isEnabled = isCheck
            }
            isSelected = isEnabled && !isNotifyEnable()

        }
    }
}

interface NfcSetting {
    fun onNfcId(id: String)
}

interface BleStatusUpdate {
    fun onChange()
}

interface DeviceStatusChange {
    fun onUIDeviceStatus(status: CHDeviceStatus) {}
}