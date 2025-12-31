
package co.candyhouse.app.tabs.devices.ssm5.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSettingMainBinding
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.setting.findSettinStringByValue
import co.candyhouse.app.tabs.devices.ssm2.setting.getSeconds
import co.candyhouse.app.tabs.devices.ssm2.setting.opsFindSettinStringByValue
import co.candyhouse.app.tabs.devices.ssm2.setting.opsFindSettingIndexByValue
import co.candyhouse.app.tabs.devices.ssm2.setting.opsGetSeconds
import co.candyhouse.app.tabs.devices.ssm2.setting.opsSecondSettingValue
import co.candyhouse.app.tabs.devices.ssm2.setting.secondSettingValue
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate

class SSM5SettingFG : BaseDeviceSettingFG<FgSettingMainBinding>() {
    override fun getViewBinder()= FgSettingMainBinding.inflate(layoutInflater)

    @SuppressLint("SimpleDateFormat", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {



        val ss5 = mDeviceModel.ssmLockLiveData.value
        if (ss5 is CHSesame5){
            val second = ss5.opsSetting?.opsLockSecond?.toInt() ?: 65535
            
            bind.opslockStatus.text = opsFindSettinStringByValue(second)
            bind.opslockStatus.visibility = View.VISIBLE
          
            bind.opsSecondTv.visibility = if ((second == 0) || (second == 65535)) View.GONE else View.VISIBLE

            var isWheelViewVisible = false
            
            bind.opsensorZone.setOnClickListener {
                if (isWheelViewVisible) {
                    
                    bind.opslockWheelview.visibility = View.GONE
                    bind.swiperefresh.isEnabled = true
                } else {
                    bind.opslockWheelview.visibility = View.VISIBLE
                    bind.swiperefresh.isEnabled = false
                }
                isWheelViewVisible = !isWheelViewVisible
            }

            super.onViewCreated(view, savedInstanceState)
            bind.autoOpenTxt.text = if (mDeviceModel.ssmLockLiveData.value!!.getIsNOHand()) getString(R.string.on) else getString(R.string.Off)
            onUIDeviceStatus(mDeviceModel.ssmLockLiveData.value!!.deviceStatus)
            bind.noHandZone.setOnClickListener { findNavController().navigate(R.id.action_to_NoHandLockFG) }

            bind. wheelview.apply {
                setItems(getSeconds())
                setInitPosition(0)
                setListener { selected ->
                    val autoLockSecond = secondSettingValue[selected]
                    ss5.autolock(autoLockSecond) { res ->
                        res.onSuccess {
                            bind.       wheelview.post {
                                bind.autolockStatus.text = findSettinStringByValue(autoLockSecond)
                                bind.autolockStatus.visibility = if (autoLockSecond == 0) View.GONE else View.VISIBLE
                                bind.secondTv.visibility = if (autoLockSecond == 0) View.GONE else View.VISIBLE
                                bind.       wheelview.visibility = View.GONE
                                bind.swiperefresh.isEnabled = true
                            }
                        }
                    }
                }
            }

            bind.opslockWheelview.apply {
                setItems(opsGetSeconds())
                setInitPosition(opsFindSettingIndexByValue(second))
                setListener { selected ->
                    val opsLockSecond = opsSecondSettingValue[selected]
                    ss5.opSensorControl(opsLockSecond) { res ->
                        res.onSuccess {
                            if (isAdded&&!isDetached){
                                bind.opslockWheelview.post {
                                    bind.opslockStatus.text = opsFindSettinStringByValue(opsLockSecond)
                                    bind.opsSecondTv.visibility = if ((opsLockSecond == 0) || (opsLockSecond == 65535)) View.GONE else View.VISIBLE
                                    bind.opslockWheelview.visibility = View.GONE
                                    bind.swiperefresh.isEnabled = true
                                }
                            }

                        }
                    }
                }
            }
            bind.chengeAngleZone.setOnClickListener {
                safeNavigate(R.id.action_SSM2SettingFG_to_SSM2SetAngleFG)
            }
        }
        bind.autolockStatus.setOnClickListener {
            L.d("ischecaksa","isChecked:"+bind.autolockSwitch.isChecked)
            if (bind.autolockSwitch.isChecked){
                bind.  wheelview.visibility = if (bind.  wheelview.visibility!=View.VISIBLE) View.VISIBLE else View.GONE
            }
        }

    } //end view created

    override fun onResume() {
        super.onResume()
    }

    override fun onUIDeviceStatus(status: CHDeviceStatus) {
        if (status.value == CHDeviceLoginStatus.logined) {
            val ss5 = mDeviceModel.ssmLockLiveData.value
            if (ss5 is CHSesame5) {

                bind.autolockSwitch.apply {
                    post {
                        bind.autolockSwitch.isEnabled = true
                        bind.autolockStatus.text = findSettinStringByValue(ss5.mechSetting!!.autoLockSecond.toInt())
                        bind.autolockStatus.visibility = if ((ss5.mechSetting?.autoLockSecond?.toInt()
                                ?: 0) == 0) View.GONE else View.VISIBLE
                        bind.   secondTv.visibility = if ((ss5.mechSetting?.autoLockSecond?.toInt()
                                ?: 0) == 0) View.GONE else View.VISIBLE
                        bind.autolockSwitch.isChecked = (ss5.mechSetting?.autoLockSecond?.toInt() ?: 0) != 0
                        setOnCheckedChangeListener { buttonView, isChecked ->
                            bind.    wheelview.visibility = if (isChecked) View.VISIBLE else View.GONE
                            bind.swiperefresh.isEnabled = !isChecked
                            if (!isChecked) {
                                bind.autolockStatus.visibility = View.GONE
                                bind.  secondTv.visibility = View.GONE
                                ss5.autolock(0) { res ->
                                    bind.  wheelview.post {
                                        res.onSuccess {
                                            if (isAdded&&!isDetached){
                                                bind.wheelview.let {
                                                    it.setCurrentPosition(0)
                                                    it.setInitPosition(0)
                                                    it.setItems(getSeconds())
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                bind.autolockSwitch.isEnabled = false
            }
        } else {
            bind.autolockSwitch.isEnabled = false
        }
    }

}
