package co.candyhouse.app.tabs.devices.ssm2.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSettingMainBinding
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import co.utils.safeNavigate

class SSM2SettingFG : BaseDeviceSettingFG<FgSettingMainBinding>() {
    override fun getViewBinder()= FgSettingMainBinding.inflate(layoutInflater)

    @SuppressLint("SimpleDateFormat", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

     bind.autoOpenTxt   .text = if (mDeviceModel.ssmLockLiveData.value!!.getIsNOHand()) getString(R.string.on) else getString(R.string.Off)
        onUIDeviceStatus(mDeviceModel.ssmLockLiveData.value!!.deviceStatus)
        bind.noHandZone      .setOnClickListener { findNavController().navigate(R.id.action_to_NoHandLockFG) }

        bind.    wheelview.apply {
            setItems(getSeconds())
            setInitPosition(0)
            setListener { selected ->
                val second = secondSettingValue[selected]
                (mDeviceModel.ssmLockLiveData.value as CHSesame2).enableAutolock(second) { res ->
                    res.onSuccess {
                        bind.          wheelview.post {
                            bind.     autolockStatus.text = findSettinStringByValue(second)
                            bind.    autolockStatus.visibility = if (second == 0) View.GONE else View.VISIBLE
                            bind.       secondTv.visibility = if (second == 0) View.GONE else View.VISIBLE
                            bind.        wheelview.visibility = View.GONE
                            bind.         swiperefresh.isEnabled = true
                        }
                    }
                }
            }
        }
        bind.chengeAngleZone.setOnClickListener {
            safeNavigate(R.id.action_SSM2SettingFG_to_SSM2SetAngleFG)
        }

        // 开关锁通知
        setupNotificationSwitch()
        view.findViewById<View>(R.id.opsensor_zone)?.visibility = View.GONE

    } //end view created

    @SuppressLint("ClickableViewAccessibility")
    private fun setupNotificationSwitch() {
        val device = mDeviceModel.ssmLockLiveData.value as? CHSesame2 ?: return

        SharedPreferencesUtils.deviceToken?.let { fcmToken ->
            device.isEnableNotification(fcmToken) { result ->
                result.onSuccess { response ->
                    bind.notiSwitch.post {
                        bind.notiSwitch.apply {
                            isActivated = false
                            isChecked = response.data
                            setOnClickListener {} // 防止默认点击
                        }
                        checkTvSysNotifyMsg(response.data)

                        bind.notiSwitch.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                handleSwitchToggle(device, fcmToken, !bind.notiSwitch.isChecked)
                            }
                            true
                        }
                    }
                }
            }
        } ?: L.d("sf", "FCM token unavailable. Reconnect and restart app")
    }

    private fun handleSwitchToggle(device: CHSesame2, token: String, enable: Boolean) {
        val operation = if (enable) device::enableNotification else device::disableNotification
        val subUUID = UserUtils.getUserId()?:""
        operation(token, subUUID) { result ->
            result.onSuccess {
                bind.notiSwitch.post {
                    bind.notiSwitch.isChecked = enable
                    checkTvSysNotifyMsg(enable)
                }
            }
        }
    }

    override fun onUIDeviceStatus(status: CHDeviceStatus) {
        if (status.value == CHDeviceLoginStatus.Login) {

            if (mDeviceModel.ssmLockLiveData.value is CHSesame2){

                val ss2 = (mDeviceModel.ssmLockLiveData.value as CHSesame2)
                ss2.getAutolockSetting { res ->
                    res.onSuccess {
                        bind.      autolockSwitch.apply {
                            post {
                                if(     bind.    autolockSwitch == null){
                                    return@post
                                }
                                bind.        autolockSwitch.isEnabled = true
                                bind.     autolockStatus.text = findSettinStringByValue(it.data)
                                bind.   autolockStatus.visibility = if (it.data == 0) View.GONE else View.VISIBLE
                                bind.  secondTv.visibility = if (it.data == 0) View.GONE else View.VISIBLE
                                bind.      autolockSwitch.isChecked = it.data != 0
                                setOnCheckedChangeListener { buttonView, isChecked ->
                                    bind.      wheelview.visibility = if (isChecked) View.VISIBLE else View.GONE
                                    bind.      swiperefresh.isEnabled = !isChecked
                                    if (!isChecked) {
                                        ss2.disableAutolock { res ->
                                            bind.     wheelview.post {
                                                res.onSuccess {

                                                    if (isAdded&&!isDetached){
                                                        bind.        wheelview.setCurrentPosition(0)
                                                        bind.        wheelview.setInitPosition(0)
                                                        bind.      autolockStatus.text = findSettinStringByValue(it.data)
                                                        bind.     autolockStatus.visibility = if (it.data == 0) View.GONE else View.VISIBLE
                                                        bind.       secondTv.visibility = if (it.data == 0) View.GONE else View.VISIBLE
                                                        bind.       wheelview.setItems(getSeconds())
                                                    }

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } //            L.d("hcia", "[UI] getVersionTag " + "status:" + status)
            }

        } else {
            bind.     autolockSwitch.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        checkTvSysNotifyMsg(isOnResume = true)
    }
}



