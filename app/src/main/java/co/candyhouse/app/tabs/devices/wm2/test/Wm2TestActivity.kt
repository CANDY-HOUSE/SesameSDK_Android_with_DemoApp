package co.candyhouse.app.tabs.devices.wm2.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleManagerDelegate
import co.candyhouse.sesame.ble.CHDeviceManager
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.wm2.*
import co.utils.L
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.test_wm2_ac.*


class Wm2TestActivity : AppCompatActivity(), CHWifiModule2Delegate {


    var mDeviceList = ArrayList<CHSesame2>()
    var mWM2keyList = ArrayList<CHSesame2>()

    var ssid: String = ""
    var password: String = ""

    companion object {
        @JvmField
        var wm2: CHWifiModule2? = null
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L.d("hcia", "測試 wm2 頁面")
        setContentView(R.layout.test_wm2_ac)

        CHBleManager.enableScan { }
        CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredWifiModule2(wifiModule: CHWifiModule2) {
//
                wm2 = wifiModule
                wm2?.delegate = this@Wm2TestActivity

                if (sesame_uuid.text.length < 6) {
                    sesame_uuid.setText(wm2!!.deviceId.toString())
                    connectStatus.setText(wm2!!.deviceStatus.toString() + " :" + wm2!!.deviceStatus.value.toString())
                    registerstatus.setText(if (wm2!!.isRegistered!!) "register" else "unregister")
                    connectBtn.setOnClickListener { wm2?.connect() {} }
                    disconnectBtn.setOnClickListener { wm2?.disconnect() {} }
                    setSSID.setOnClickListener {
                        ssid = findViewById<EditText>(R.id.ssid).text.toString()
                        wm2?.setWifiSSID(ssid) {}
                    }
                    setPassword.setOnClickListener {
                        password = findViewById<EditText>(R.id.password).text.toString()
                        wm2?.setWifiPassword(password) {}
                    }

                    connectWifi.setOnClickListener { wm2?.connectWifi { } }

                    test_btn.setOnClickListener { wm2?.testSSM { } }

                    wm2?.connect { }
                }
            }
        }

        lockBtn.setOnClickListener {
            wm2?.testIOTLOCK { }
        }
        unlockBtn.setOnClickListener {
            wm2?.testIOTUNLOCK { }
        }


    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        L.d("hcia", "測試 wm2 頁面 onPostCreate")

        recy.apply {
            val tmp: LinearLayoutManager = layoutManager as LinearLayoutManager
            tmp.setOrientation(LinearLayoutManager.HORIZONTAL)
            setHasFixedSize(true)
            adapter = object : GenericAdapter<CHSesame2>(mDeviceList) {
                override fun getLayoutId(position: Int, obj: CHSesame2): Int {
                    return R.layout.cell_sesame_id
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view),
                            Binder<CHSesame2> {
                        var customName = itemView.findViewById<TextView>(R.id.title)


                        @SuppressLint("SetTextI18n")
                        override fun bind(ssm: CHSesame2, pos: Int) {
                            customName.text = ssm.deviceId.toString().substring(0..7)
                            customName.setOnClickListener {
                                L.d("hcia", "ＵＩ 點擊itemView:")

                                wm2?.insertSesames(ssm) {

                                }
                            }
                        }
                    }
                }
            }
        }
        wm2_keys_recy.apply {
            val tmp: LinearLayoutManager = layoutManager as LinearLayoutManager
            tmp.setOrientation(LinearLayoutManager.HORIZONTAL)
            setHasFixedSize(true)
            adapter = object : GenericAdapter<CHSesame2>(mWM2keyList) {
                override fun getLayoutId(position: Int, obj: CHSesame2): Int {
                    return R.layout.cell_sesame_id
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view),
                            Binder<CHSesame2> {
                        var customName = itemView.findViewById<TextView>(R.id.title)

                        @SuppressLint("SetTextI18n")
                        override fun bind(ssm: CHSesame2, pos: Int) {
                            customName.text = ssm.deviceId.toString().substring(0..7)
                        }
                    }
                }
            }
        }


        CHDeviceManager.getSesame2s {
            it.onSuccess {
                mDeviceList.clear()
                mDeviceList.addAll(it.data)
                recy?.post {
                    L.d("hcia", "設定列表")
                    (recy.adapter as GenericAdapter<*>).notifyDataSetChanged()
                }
            }
        }
    }

    override fun onMechSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {
        wifi_ssid_txt.post {
            wifi_ssid_txt.text = settings.wifiSSID
            wifi_pass_txt.text = settings.wifiPassWord
        }
    }

    override fun onNetWorkStatusChanged(device: CHWifiModule2, status: CHWifiModule2NetWorkStatus) {
        register?.post {
            ap_status.setTextColor(ContextCompat.getColor(this, if (status.isAPWork) R.color.unlock_blue else R.color.lock_red))
            net_status.setTextColor(ContextCompat.getColor(this, if (status.isNetWork) R.color.unlock_blue else R.color.lock_red))
            iot_status.setTextColor(ContextCompat.getColor(this, if (status.isIOTWork) R.color.unlock_blue else R.color.lock_red))
        }
    }

    override fun onBleDeviceStatusChanged(device: CHWifiModule2, status: CHWifiModule2Status) {
        connectStatus?.post {
            connectStatus?.setText(wm2?.deviceStatus.toString() + " :" + wm2?.deviceStatus?.value.toString())
            if (status == CHWifiModule2Status.receivedBle) {
                wm2?.connect { }
            }
        }
    }
}
