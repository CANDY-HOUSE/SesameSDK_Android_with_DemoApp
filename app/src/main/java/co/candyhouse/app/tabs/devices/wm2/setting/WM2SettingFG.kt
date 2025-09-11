package co.candyhouse.app.tabs.devices.wm2.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.server.CHLoginAPIManager
import co.utils.SharedPreferencesUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import co.utils.alerts.ext.inputTextAlert
import co.candyhouse.app.base.BleStatusUpdate
import co.candyhouse.app.databinding.FgWm2SettingBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleStatusDelegate
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate

class WM2SettingFG : BaseDeviceFG<FgWm2SettingBinding>(), CHWifiModule2Delegate, BleStatusUpdate {

    var mDeviceList = ArrayList<String>()
    var versionTag: MutableLiveData<String?> = MutableLiveData()
    override fun getViewBinder() = FgWm2SettingBinding.inflate(layoutInflater)

    override fun onResume() {
        super.onResume()
        onChange()

        CHBleManager.statusDelegate = object : CHBleStatusDelegate {
            override fun didScanChange(ss: CHScanStatus) {
                onChange()
            }
        }
        val device = mDeviceModel.ssmLockLiveData.value
        if (device is CHWifiModule2) {
            mDeviceModel.ssmosLockDelegates[device] = object : CHWifiModule2Delegate {
                override fun onSSM2KeysChanged(
                    device: CHWifiModule2,
                    ssm2keys: Map<String, String>
                ) {
                    if (!isAdded) return
                    mDeviceList.clear()
                    mDeviceList.addAll(device.ssm2KeysMap.map { it.key })

                    bind.recy.post {
                        (bind.recy.adapter as GenericAdapter<*>).notifyDataSetChanged()
                        bind.devicesEmptyLogo.visibility = if (mDeviceList.size == 0) View.VISIBLE else View.GONE
                        bind.tvAddSsmLogo.visibility = if (mDeviceList.size == 0) View.GONE else View.VISIBLE
                    }
                }

                override fun onAPSettingChanged(
                    device: CHWifiModule2,
                    settings: CHWifiModule2MechSettings
                ) {
                    bind.wifiSsidTxt.text = settings.wifiSSID
                    bind.wifiPassTxt.text = settings.wifiPassWord
                }

                override fun onMechStatus(device: CHDevices) {
                    val status = device.mechStatus as? CHWifiModule2NetWorkStatus ?: return
                    bind.iotOkLine.visibility =
                        if (status.isIOTWork == true) View.VISIBLE else View.GONE
                    bind.netOkLine.visibility =
                        if (status.isNetWork == true) View.VISIBLE else View.GONE

                    bind.apIcon.isSelected = status.isAPWork == true
                    bind.netIcon.isSelected = status.isNetWork == true
                    bind.iotIcon.isSelected = status.isIOTWork == true

                    bind.apingIcon.visibility =
                        if (status.isAPConnecting) View.VISIBLE else View.GONE
                    bind.netingIcon.visibility =
                        if (status.isConnectingNet) View.VISIBLE else View.GONE
                    bind.iotIngIcon.visibility =
                        if (status.isConnectingIOT) View.VISIBLE else View.GONE
                    bind.ssidLogo.visibility =
                        if (status.isAPCheck == false) View.VISIBLE else View.GONE
                }

                /*override fun onNetWorkStatusChanged(device: CHWifiModule2, status: CHWifiModule2NetWorkStatus) {
                    ap_icon?.isSelected = status.isAPWork == true
                    iot_ok_line?.visibility = if (status.isIOTWork== true) View.VISIBLE else View.GONE
                    net_ok_line?.visibility = if (status.isNetWork == true) View.VISIBLE else View.GONE

                    ap_icon?.isSelected = status.isAPWork == true
                    net_icon?.isSelected = status.isNetWork == true
                    iot_icon?.isSelected = status.isIOTWork == true

                    aping_icon?.visibility = if (status.isAPConnecting) View.VISIBLE else View.GONE
                    neting_icon?.visibility = if (status.isConnectingNet) View.VISIBLE else View.GONE
                    bind.iotIngIcon?.visibility = if (status.isConnectingIOT) View.VISIBLE else View.GONE
                    ssid_logo?.visibility = if (status.isAPCheck == false) View.VISIBLE else View.GONE
                }*/

                override fun onBleDeviceStatusChanged(
                    device: CHDevices,
                    status: CHDeviceStatus,
                    shadowStatus: CHDeviceStatus?
                ) {
                    if (device is CHWifiModule2) {
                        if (status.value == CHDeviceLoginStatus.Login) {
                            device.getVersionTag { versionTag.postValue(it.getOrNull()?.data) }
                        }
                        if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                            device.connect { }
                        }
                        onChange()
                    }
                }

                override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
                    bind.wm2VersionTxt.post { bind.wm2VersionTxt.text = percent.toString() }
                }
            }.bindLifecycle(viewLifecycleOwner)
        }

    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val device = mDeviceModel.ssmLockLiveData.value
        device?.apply {
            if (device is CHWifiModule2) {
                bind.dropHintTxt.text = getString(R.string.drop_hint, getString(R.string.WM2))
                bind.resetZone.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
                if (device.getIsJustRegister()) {
                    device.setIsJustRegister(false)
                    safeNavigate(R.id.action_WM2SettingFG_to_WM2ScanFG)
                }
                mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { wm2 ->
                    wm2.connect { }
                    bind.deviceModel.text = wm2.productModel.deviceModelName()
                    bind.nameTxt.text = wm2.getNickname()
                    bind.wm2IdTxt.text = wm2.deviceId.toString()
                    bind.wifiSsidTxt.text = (wm2 as CHWifiModule2).mechSetting?.wifiSSID
                    bind.wifiPassTxt.text = wm2.mechSetting?.wifiPassWord
                    bind.apIcon.isSelected =
                        ((wm2.mechStatus as? CHWifiModule2NetWorkStatus)?.isAPWork == true)
                    bind.netIcon.isSelected =
                        ((wm2.mechStatus as? CHWifiModule2NetWorkStatus)?.isNetWork == true)
                    bind.iotIcon.isSelected =
                        ((wm2.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true)
                    bind.ssidLogo.visibility =
                        if ((wm2.mechStatus as? CHWifiModule2NetWorkStatus)?.isAPCheck == false) View.VISIBLE else View.GONE
                    bind.iotOkLine.visibility =
                        if (bind.iotIcon.isSelected) View.VISIBLE else View.GONE
                    bind.netOkLine.visibility =
                        if (bind.netIcon.isSelected) View.VISIBLE else View.GONE
                    wm2.getVersionTag { versionTag.postValue("version:" + it.getOrNull()?.data) }

                }
                versionTag.observe(viewLifecycleOwner) { bind.wm2VersionTxt.text = it }

                bind.dfuZone.setOnClickListener {
                    AlertView("", "", AlertStyle.IOS).apply {
                        addAction(
                            AlertAction(
                                getString(R.string.wm2_update),
                                AlertActionStyle.NEGATIVE
                            ) { action ->
                                (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).updateFirmware {}
                            })
                        show(activity as AppCompatActivity)
                    }
                }
                bind.recy.apply {
                    mDeviceList.clear()
                    mDeviceList.addAll(device.ssm2KeysMap.map { it.key })
                    bind.devicesEmptyLogo.visibility =
                        if (mDeviceList.size == 0) View.VISIBLE else View.GONE
                    bind.tvAddSsmLogo.visibility = if (mDeviceList.size == 0) View.GONE else View.VISIBLE
                    adapter = object : GenericAdapter<String>(mDeviceList) {
                        override fun getLayoutId(position: Int, obj: String): Int =
                            R.layout.wm2_key_cell

                        override fun getViewHolder(
                            view: View,
                            viewType: Int
                        ): RecyclerView.ViewHolder =
                            object : RecyclerView.ViewHolder(view), Binder<String> {
                                val title = itemView.findViewById<TextView>(R.id.title)
                                override fun bind(keyId: String, pos: Int) {
                                    title.text =
                                        SharedPreferencesUtils.preferences.getString(keyId, keyId)
                                    itemView.setOnClickListener {
                                        AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                                            addAction(
                                                AlertAction(
                                                    getString(R.string.ssm_delete),
                                                    AlertActionStyle.NEGATIVE
                                                ) { action ->
                                                    (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).removeSesame(
                                                        keyId
                                                    ) {}
                                                })
                                            show(activity as AppCompatActivity)
                                        }
                                    }
                                }
                            }
                    }
                }

                bind.changeNameZone.setOnClickListener {
                    context?.inputTextAlert(
                        null,
                        getString(R.string.edit_name),
                        device.getNickname()
                    ) {
                        confirmButtonWithText("OK") { alert, name ->
                            (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).setNickname(name)
                            CHLoginAPIManager.putKey(
                                cheyKeyToUserKey(
                                    device.getKey(),
                                    device.getLevel(),
                                    device.getNickname()
                                )
                            ) {}
                            getView()?.findViewById<TextView>(R.id.name_txt)?.text = name
                            dismiss()
                        }
                        cancelButton(getString(R.string.cancel))
                    }?.show()
                }


                bind.apScanZone.setOnClickListener { safeNavigate(R.id.action_WM2SettingFG_to_WM2ScanFG) }
                bind.addLockerZone.setOnClickListener {

                    if (isAdded) {
                        if (mDeviceList.isNotEmpty()) {
                            val strs = ArrayList<String>()
                            mDeviceList.forEach {
                                strs.add(it)
                            }
                            val bundle = Bundle()

                            bundle.putStringArrayList("data", strs)
                            safeNavigate(R.id.action_WM2SettingFG_to_WM2SelectLockerFG, bundle)
                        } else {
                            safeNavigate(R.id.action_WM2SettingFG_to_WM2SelectLockerFG)
                        }
                    }
                    //  safeNavigate(R.id.action_WM2SettingFG_to_WM2SelectLockerFG)

                }
                bind.dropZone.setOnClickListener {
                    AlertView("", "", AlertStyle.IOS).apply {
                        addAction(
                            AlertAction(
                                getString(R.string.TrashTheWifiModule2Key),
                                AlertActionStyle.NEGATIVE
                            ) { action ->
                                mDeviceModel.dropDevice {
                                    it.onSuccess {
                                        if (isAdded) {
                                            findNavController().navigateUp()
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

                bind.resetZone.setOnClickListener {
                    AlertView("", "", AlertStyle.IOS).apply {
                        addAction(
                            AlertAction(
                                getString(R.string.ResetWifiModule2),
                                AlertActionStyle.NEGATIVE
                            ) { action ->
                                mDeviceModel.resetDevice {
                                    if (isAdded) {
                                        it.onSuccess {
                                            findNavController().navigateUp()
                                        }
                                        it.onFailure {
                                            toastMSG(it.localizedMessage)
                                            L.d("hcia", "無法刪除鑰匙倉庫:")
                                        }
                                    }

                                }
                            })
                        show(activity as AppCompatActivity)
                    }
                }
            }
        }

    }//end view created

    override fun onChange() {
        val device = mDeviceModel.ssmLockLiveData.value
        when {
            CHBleManager.mScanning == CHScanStatus.BleClose -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.noble)
            }

            device is CHWifiModule2 && device.deviceStatus == CHDeviceStatus.NoBleSignal -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.NoBleSignal)
            }

            device is CHWifiModule2 && device.deviceStatus.value == CHDeviceLoginStatus.UnLogin -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = device.deviceStatus.toString()
            }

            else -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.GONE
            }
        }
    }

}