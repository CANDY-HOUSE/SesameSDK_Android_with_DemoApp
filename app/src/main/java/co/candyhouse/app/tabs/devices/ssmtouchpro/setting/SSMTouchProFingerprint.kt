package co.candyhouse.app.tabs.devices.ssmtouchpro.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHSesameTouchPro
import co.candyhouse.sesame.open.device.CHSesameTouchProDelegate
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.hexStringToByteArray
import co.utils.hexStringToIntStr
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_ssm_tp_fp_list.*


data class FingerPrint(var id: String, var name: String)

class SSMTouchProFingerprint : BaseDeviceFG(R.layout.fg_ssm_tp_fp_list), CHWifiModule2Delegate {
    var mFingers = ArrayList<FingerPrint>()
    override fun onDestroyView() {
        super.onDestroyView()
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrintModeSet(2) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        leaderboard_list.setEmptyView(empty_view)
        swiperefresh.isEnabled = false
        img_mode_add.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrintModeSet(2) {
                it.onSuccess {
                    view.post {
                        img_mode_add.visibility = View.GONE
                        img_mode_verify.visibility = View.VISIBLE
                    }
                }
            }
        }
        img_mode_verify.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrintModeSet(1) {
                it.onSuccess {
                    view.post {
                        img_mode_verify.visibility = View.GONE
                        img_mode_add.visibility = View.VISIBLE
                    }
                }
            }
        }
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrintModeGet {
            it.onSuccess {
                if (it.data.toInt() == 2) {
                    view.post {
                        img_mode_verify.visibility = View.VISIBLE
                        img_mode_add.visibility = View.GONE
                    }
                } else {
                    view.post {
                        img_mode_add.visibility = View.VISIBLE
                        img_mode_verify.visibility = View.GONE
                    }
                }

            }
        }

        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrints {}

        mDeviceModel.ssmosLockDelegates[(mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro)] = object : CHSesameTouchProDelegate {

            override fun onFingerPrintReceiveStart(device: CHSesameConnector) {
                mFingers.clear()
                swiperefresh?.isRefreshing = true
            }
            override fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {
                mFingers.add(0, FingerPrint(ID, name))
                leaderboard_list?.adapter?.notifyDataSetChanged()
                menu_title.text = "${mFingers.size}/100"
            }
            override fun onFingerPrintReceiveEnd(device: CHSesameConnector) {
                swiperefresh?.isRefreshing = false
                leaderboard_list?.adapter?.notifyDataSetChanged()
                menu_title.text = "${mFingers.size}/100"
            }

            override fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {
                mFingers.add(0, FingerPrint(ID, name))
                leaderboard_list?.adapter?.notifyDataSetChanged()
                menu_title.text = "${mFingers.size}/100"
            }
        }

        leaderboard_list.adapter = object : GenericAdapter<FingerPrint>(mFingers) {
            override fun getLayoutId(position: Int, obj: FingerPrint): Int =
                R.layout.cell_fingerprint

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(view), Binder<FingerPrint> {
                    override fun bind(data: FingerPrint, pos: Int) {
                        val title = itemView.findViewById<TextView>(R.id.title)
                        val name = itemView.findViewById<TextView>(R.id.sub_title)
                        title.text = (data.id.hexStringToByteArray().get(0).toUInt().toInt() + 1).toString().padStart(3, '0')
                        name.text = if (data.name == "") getString(R.string.default_fingerprint_name) else data.name

                        itemView.setOnClickListener {
                            AlertView(title.text.toString(), "", AlertStyle.IOS).apply {

                                addAction(AlertAction(getString(R.string.TouchProFingerModify), AlertActionStyle.DEFAULT) { action ->
                                    view.context?.inputTextAlert("", name.text.toString(), data.name) {
                                        confirmButtonWithText("OK") { alert, name ->
                                            dismiss()
                                            mFingers.remove(data)
                                            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrintsChange(data.id, name) {}
                                        }
                                        cancelButton(getString(R.string.cancel))
                                    }?.show()
                                })
                                addAction(AlertAction(getString(R.string.TouchProFingerDelete), AlertActionStyle.NEGATIVE) { action ->
                                    (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).fingerPrintDelete(data.id) {
                                        view.post {
                                            mFingers.remove(data)
                                            leaderboard_list.adapter?.notifyDataSetChanged()
                                            menu_title.text = "${mFingers.size}/100"

                                        }
                                    }
                                })
                                show(activity as AppCompatActivity)
                            }
                        }
                    }
                }
        }
    }

}