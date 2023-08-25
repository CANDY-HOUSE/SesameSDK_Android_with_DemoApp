package co.candyhouse.app.tabs.devices.ssm5.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.CandyHouseApp
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.app.tabs.devices.ssm2.setting.findSettinStringByValue
import co.candyhouse.app.tabs.devices.ssm2.setting.getSeconds
import co.candyhouse.app.tabs.devices.ssm2.setting.secondSettingValue
import co.candyhouse.sesame.open.device.*
import co.utils.L
import co.utils.SharedPreferencesUtils
import kotlinx.android.synthetic.main.fg_setting_main.*

class SSM5SettingFG : BaseDeviceSettingFG(R.layout.fg_setting_main) {

    @SuppressLint("SimpleDateFormat", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auto_open_txt.text = if (mDeviceModel.ssmLockLiveData.value!!.getIsNOHand()) getString(R.string.on) else getString(R.string.Off)
        onUIDeviceStatus(mDeviceModel.ssmLockLiveData.value!!.deviceStatus)
        no_hand_zone.setOnClickListener { findNavController().navigate(R.id.action_to_NoHandLockFG) }

        wheelview.apply {
            setItems(getSeconds())
            setInitPosition(0)
            setListener { selected ->
                val second = secondSettingValue.get(selected)
                val ss5 = (mDeviceModel.ssmLockLiveData.value as CHSesame5)
                ss5.autolock(second) { res ->
                    res.onSuccess {
                        wheelview?.post {
                            autolock_status?.text = findSettinStringByValue(second)
                            autolock_status.visibility = if (second == 0) View.GONE else View.VISIBLE
                            second_tv?.visibility = if (second == 0) View.GONE else View.VISIBLE
                            wheelview.visibility = View.GONE
                            swiperefresh.isEnabled = true
                        }
                    }
                }
            }
        }
        reset_zone.setOnClickListener {
            mDeviceModel.resetDevice {
               it.onSuccess {

               }

            }

        }
        chenge_angle_zone.setOnClickListener { findNavController().navigate(R.id.action_SSM2SettingFG_to_SSM2SetAngleFG) }

        SharedPreferencesUtils.deviceToken?.let { fcmToken ->
            (mDeviceModel.ssmLockLiveData.value as CHSesame5).isEnableNotification(fcmToken) {
                it.onSuccess {
                    noti_switch?.post {
                        noti_switch?.isActivated = false
                        noti_switch?.isChecked = it.data
                        noti_switch?.setOnClickListener {}
                        noti_switch?.setOnTouchListener { _, motionEvent ->
                            if (motionEvent.action == MotionEvent.ACTION_UP) {
                                if (noti_switch!!.isChecked == false) {
                                    L.d("hcia", "打開 enableNotification:")
                                    (mDeviceModel.ssmLockLiveData.value as CHSesame5).enableNotification(fcmToken) {
                                        it.onSuccess {
                                            noti_switch?.post { noti_switch?.isChecked = true }
                                        }
                                    }
                                } else {
                                    (mDeviceModel.ssmLockLiveData.value as CHSesame5).disableNotification(fcmToken) {
                                        it.onSuccess {
                                            noti_switch?.post { noti_switch?.isChecked = false }
                                        }
                                    }
                                }
                            }
                            true
                        }
                    }
                }
            }
        }

    } //end view created

    override fun onUIDeviceStatus(status: CHDeviceStatus) {
        if (status.value == CHDeviceLoginStatus.Login) {
            val ss5 = (mDeviceModel.ssmLockLiveData.value as CHSesame5)
            autolockSwitch?.apply {
                post {
                    autolockSwitch.isEnabled = true
                    autolock_status?.text = findSettinStringByValue(ss5.mechSetting!!.autoLockSecond.toInt())
                    autolock_status?.visibility = if ((ss5.mechSetting?.autoLockSecond?.toInt() ?: 0) == 0) View.GONE else View.VISIBLE
                    second_tv?.visibility = if ((ss5.mechSetting?.autoLockSecond?.toInt() ?: 0) == 0) View.GONE else View.VISIBLE
                    autolockSwitch?.isChecked = (ss5.mechSetting?.autoLockSecond?.toInt() ?: 0) != 0
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        wheelview?.visibility = if (isChecked) View.VISIBLE else View.GONE
                        swiperefresh.isEnabled = !isChecked
                        if (!isChecked) {
                            ss5.autolock(0) { res ->
                                wheelview?.post {
                                    res.onSuccess {
                                        wheelview.setCurrentPosition(0)
                                        wheelview.setInitPosition(0)
                                        autolock_status?.text = findSettinStringByValue(ss5.mechSetting!!.autoLockSecond.toInt())
                                        autolock_status?.visibility = if (ss5.mechSetting!!.autoLockSecond.toInt() == 0) View.GONE else View.VISIBLE
                                        second_tv?.visibility = if (ss5.mechSetting!!.autoLockSecond.toInt() == 0) View.GONE else View.VISIBLE
                                        wheelview.setItems(getSeconds())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            autolockSwitch?.isEnabled = false
        }
    }
}
