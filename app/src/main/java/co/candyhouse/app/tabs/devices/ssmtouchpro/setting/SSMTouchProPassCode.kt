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
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.hexStringToIntStr
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_ssm_tp_passcode_list.*

data class KeyboardPassCode(var id: String, var name: String)

class SesameKeyboardPassCode : BaseDeviceFG(R.layout.fg_ssm_tp_passcode_list), CHWifiModule2Delegate {
    var mKbSecretList = ArrayList<KeyboardPassCode>()
    override fun onStop() {
        super.onStop()
        L.d("hcia", "onStop:" )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        L.d("hcia", "onDestroyView:" )
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCodeModeSet(0) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        leaderboard_list.setEmptyView(empty_view)
        swiperefresh.isEnabled = false
        img_mode_add.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCodeModeSet(0) {
                it.onSuccess {
                    view.post {
                        img_mode_add.visibility = View.GONE
                        img_mode_verify.visibility = View.VISIBLE
                    }
                }
            }
        }
        img_mode_verify.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCodeModeSet(1) {
                it.onSuccess {
//                    L.d("hcia", "[UI][setMode][OK]")
                    view.post {
                        img_mode_verify.visibility = View.GONE
                        img_mode_add.visibility = View.VISIBLE
                    }
                }
            }
        }


        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCodeModeGet {
            it.onSuccess {
//                L.d("hcia", "[UI][getMode][OK]" + it.data)
                if (it.data.toInt() == 0) {
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
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCode {}
        mDeviceModel.ssmosLockDelegates[(mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro)] = object : CHSesameTouchProDelegate {

            override fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {
                mKbSecretList.add(0, KeyboardPassCode(ID, name))
                leaderboard_list?.adapter?.notifyDataSetChanged()
                L.d("hcia", "mKbSecretList:" + mKbSecretList)
                menu_title.text = "${mKbSecretList.size}/100"
            }

            override fun onKeyBoardReceiveEnd(device: CHSesameConnector) {
                leaderboard_list?.adapter?.notifyDataSetChanged()
                swiperefresh?.isRefreshing = false
                menu_title.text = "${mKbSecretList.size}/100"
            }

            override fun onKeyBoardReceiveStart(device: CHSesameConnector) {
                mKbSecretList.clear()
                swiperefresh?.isRefreshing = true
            }

            override fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {
                mKbSecretList.add(0, KeyboardPassCode(ID, name))
                leaderboard_list?.adapter?.notifyDataSetChanged()
                L.d("hcia", "mKbSecretList:" + mKbSecretList)
                menu_title.text = "${mKbSecretList.size}/100"

            }
        }

        leaderboard_list.adapter = object : GenericAdapter<KeyboardPassCode>(mKbSecretList) {
            override fun getLayoutId(position: Int, obj: KeyboardPassCode): Int =
                R.layout.cell_password

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(view), Binder<KeyboardPassCode> {
                    override fun bind(data: KeyboardPassCode, pos: Int) {
                        val title = itemView.findViewById<TextView>(R.id.title)
                        val name = itemView.findViewById<TextView>(R.id.sub_title)

                        title.text = data.id.hexStringToIntStr()
                        name.text = if (data.name == "") getString(R.string.default_passcode_name) else data.name

                        itemView.setOnClickListener {
                            AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                                addAction(AlertAction(getString(R.string.TouchProPWDModify), AlertActionStyle.DEFAULT) { action ->
                                    view.context?.inputTextAlert("", name.text.toString(), data.name) {
                                        confirmButtonWithText("OK") { alert, name ->
                                            dismiss()
                                            mKbSecretList.remove(data)
                                            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCodeChange(data.id, name) {}
                                        }
                                        cancelButton(getString(R.string.cancel))
                                    }?.show()
                                })
                                addAction(AlertAction(getString(R.string.TouchProPWDDelete), AlertActionStyle.NEGATIVE) { action ->
                                    (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).keyBoardPassCodeDelete(data.id) {
                                        view.post {
                                            mKbSecretList.remove(data)
                                            leaderboard_list.adapter?.notifyDataSetChanged()
                                            menu_title.text = "${mKbSecretList.size}/100"
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