package co.candyhouse.app.tabs.devices.ssmtouchpro.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.device.*
import co.utils.SharedPreferencesUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_sesame_touchpro_setting.*
import kotlinx.android.synthetic.main.fg_sesame_touchpro_setting.add_locker_zone
import kotlinx.android.synthetic.main.fg_sesame_touchpro_setting.devices_empty_logo
import kotlinx.android.synthetic.main.fg_sesame_touchpro_setting.recy
import kotlinx.android.synthetic.main.fg_wm2_setting.*

data class LockDeviceStatus(var id: String, var model: Byte, var status: Byte)
class SesameTouchProSettingFG : BaseDeviceSettingFG(R.layout.fg_sesame_touchpro_setting), CHSesameTouchProDelegate {

    var mDeviceList = ArrayList<LockDeviceStatus>()

    override fun onResume() {
        super.onResume()
        mDeviceModel.ssmosLockDelegates[(mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro)] = object : CHSesameTouchProDelegate {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
                if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                    device.connect { }
                }
            }

            override fun onMechStatus(device: CHDevices) {
                view?.findViewById<TextView>(R.id.battery)?.post {
                    view?.findViewById<TextView>(R.id.battery)?.text = "${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                }
            }

            override fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {
                recy?.post {
                    mDeviceList.clear()
                    mDeviceList.addAll(ssm2keys.map {
                        LockDeviceStatus(it.key, it.value[0], it.value[1])
                    })
//                    L.d("hcia", "mDeviceList:" + mDeviceList)

                    recy.adapter?.notifyDataSetChanged()
                    devices_empty_logo.visibility = if (mDeviceList.size == 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mDevice: CHSesameTouchPro = mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro
        mDevice.connect { }

        recy.apply {
            mDeviceList.clear()
            mDeviceList.addAll((mDeviceModel.ssmLockLiveData.value as CHSesameConnector).ssm2KeysMap.map {
                LockDeviceStatus(it.key, it.value[0], it.value[1])
            })
            devices_empty_logo.visibility = if (mDeviceList.size == 0) View.VISIBLE else View.GONE
            adapter = object : GenericAdapter<LockDeviceStatus>(mDeviceList) {
                override fun getLayoutId(position: Int, obj: LockDeviceStatus): Int =
                    R.layout.wm2_key_cell

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(view), Binder<LockDeviceStatus> {
                        @SuppressLint("SetTextI18n")
                        override fun bind(data: LockDeviceStatus, pos: Int) {
                            val title = itemView.findViewById<TextView>(R.id.title)
                            title.text = SharedPreferencesUtils.preferences.getString(data.id, data.id)
                            itemView.setOnClickListener {
                                AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                                    addAction(AlertAction(getString(R.string.ssm_delete), AlertActionStyle.NEGATIVE) { action ->
                                        (mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro).removeSesame(data.id) {}
                                    })
                                    show(activity as AppCompatActivity)
                                }
                            }
                        }
                    }
            }
        }
        if (mDevice.productModel == CHProductModel.SSMOpenSensor) {
            friend_recy.visibility = View.GONE
        }
        cards_zone.setOnClickListener { findNavController().navigate(R.id.to_SesameKeyboardCards) }
        fp_zone.setOnClickListener { findNavController().navigate(R.id.to_SesameKeyboardFingerprint) }
        password_zone.setOnClickListener { findNavController().navigate(R.id.to_SesameKeyboardPassword) }
        if (mDevice.productModel == CHProductModel.SSMTouch) {
            password_zone.visibility = View.GONE
        } else if (mDevice.productModel == CHProductModel.SSMOpenSensor || mDevice.productModel == CHProductModel.BLEConnector) {
            cards_zone.visibility = View.GONE
            fp_zone.visibility = View.GONE
            password_zone.visibility = View.GONE
            battery_zone.visibility = View.GONE 
        }
        view?.findViewById<View>(R.id.share_zone)?.visibility = if (mDevice.productModel == CHProductModel.SSMOpenSensor ) View.GONE else View.VISIBLE
        add_ssm_hint_by_touch_txt.text = getString(R.string.add_ssm_hint_by_touch, mDevice.productModel.modelName())
        trash_device_key_txt.text = getString(R.string.trash_device_key, mDevice.productModel.modelName())
        add_locker_zone.setOnClickListener { findNavController().navigate(R.id.to_SesameKeyboardSelectLockerListFG) }
        view?.findViewById<TextView>(R.id.battery)?.post {
            view?.findViewById<TextView>(R.id.battery)?.text =
                "${(mDeviceModel.ssmLockLiveData.value as CHSesameConnector).mechStatus?.getBatteryPrecentage()?.let { it.toString() + "%" } ?: ""}"
        }

//        trash_device_txt.text = getString(R.string.trash_device_key, mDevice.productModel.modelName())
    }

    override fun onDestroy() {
        super.onDestroy()
        mDeviceModel.ssmLockLiveData.value?.disconnect { }
    }
}