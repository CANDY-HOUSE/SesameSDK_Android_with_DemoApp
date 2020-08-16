package co.candyhouse.app.tabs.devices

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleManagerDelegate
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.setting.DfuService
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Status
import co.utils.L
import co.utils.recycle.EmptyRecyclerView
import co.utils.recycle.GenericAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fg_rg_device.*
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.lang.Math.pow
import java.util.*

class RegisterDevicesFG : Fragment() {

    private lateinit var recyclerView: EmptyRecyclerView
    var mDeviceList = ArrayList<CHSesame2>()
    private var lastClickTime = 0L
    private var FAST_CLICK_DELAY_TIME = 500

    override fun onResume() {
        super.onResume()

        DfuServiceListenerHelper.registerProgressListener(activity!!, dfuLs)

        (activity as MainActivity).hideMenu()
        CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredSesame2s(sesames: List<CHSesame2>) {
                mDeviceList.clear()
                mDeviceList.addAll(sesames.sortedByDescending { it.rssi })
                mDeviceList.firstOrNull()?.connect { }
                mDeviceList.let {
                    recyclerView.post {
//                    L.d("hcia", "sesames:" + sesames.first().rssi)
                        if (System.currentTimeMillis() - lastClickTime >= FAST_CLICK_DELAY_TIME) {
                            (recyclerView.adapter as GenericAdapter<*>).notifyDataSetChanged()
                            lastClickTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_rg_device, container, false)

        recyclerView = view.findViewById<EmptyRecyclerView>(R.id.leaderboard_list).apply {
            setHasFixedSize(true)
            adapter = object : GenericAdapter<Any>(mDeviceList) {

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    return super.onCreateViewHolder(parent, viewType)

                }

                override fun getLayoutId(position: Int, obj: Any): Int {
                    return R.layout.cell_device_unregist
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view),
                            Binder<CHSesame2> {
                        var customName: TextView = itemView.findViewById(R.id.title)
                        var uuidTxt: TextView = itemView.findViewById(R.id.title_txt)
                        var update_img: ImageView = itemView.findViewById(R.id.update_fw)
                        var statusTxt: TextView = itemView.findViewById(R.id.subtitle_txt)

                        @SuppressLint("SetTextI18n")
                        override fun bind(data: CHSesame2, pos: Int) {
                            val sesame = data
                            update_img.setOnClickListener {
                                L.d("hcia", "點擊到更新圖片")
                                sesame.updateFirmware() { res ->
                                    res.onSuccess {
                                        val starter = DfuServiceInitiator(it.data.address)
                                        starter.setZip(R.raw.d533ef10)
                                        starter.setPacketsReceiptNotificationsEnabled(false)
                                        starter.setPrepareDataObjectDelay(400)
                                        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                                        starter.setDisableNotification(true)
                                        starter.setForeground(false)
                                        starter.start(activity!!, DfuService::class.java)
                                    }
                                }
                                return@setOnClickListener
                            }
                            itemView.setOnClickListener {

                                if (sesame.deviceStatus == CHSesame2Status.dfumode) {
                                    sesame.updateFirmware() { res ->
                                        res.onSuccess {
                                            val starter = DfuServiceInitiator(it.data.address)
                                            starter.setZip(R.raw.d533ef10)
                                            starter.setPacketsReceiptNotificationsEnabled(false)
                                            starter.setPrepareDataObjectDelay(400)
                                            starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                                            starter.setDisableNotification(true)
                                            starter.setForeground(false)
                                            starter.start(activity!!, DfuService::class.java)
                                        }
                                    }
                                    return@setOnClickListener
                                }
                                sesame.connect() {}

                                MainActivity.activity?.showProgress()
                                if (sesame.deviceStatus == CHSesame2Status.readytoRegister) {
                                    registerSSM(sesame)
                                } else {
                                    sesame.delegate = object : CHSesame2Delegate {
                                        override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
                                            if (status == CHSesame2Status.readytoRegister) {
                                                registerSSM(sesame)
                                            }
                                        }
                                    }
                                }
                            }
                            val distance: Int = (pow(10.0, ((sesame.txPowerLevel!! - sesame.rssi!!.toDouble() - 62.0) / 20.0)) * 100).toInt()
                            customName.text = "" + (if (sesame.rssi == null) "-" else distance.toString() + " cm")
                            uuidTxt.text = sesame.deviceId.toString().toUpperCase()
                            statusTxt.text = getString(R.string.Sesame2) + data.deviceStatus
                        }
                    }
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setEmptyView(empty_view)
        backicon.setOnClickListener { findNavController().navigateUp() }
    }

    private fun RecyclerView.ViewHolder.registerSSM(sesame: CHSesame2) {
        sesame.registerSesame2 { res ->

            res.onSuccess {
                L.d("hcia", "UI收到成功註冊")
                sesame.setHistoryTag("ドラえもん".toByteArray()) {}
                sesame.configureLockPosition(0, 256) {}
                DeviceListFG.instance?.refleshPage()
                activity?.runOnUiThread {
                    findNavController().navigateUp()
                }

            }
            res.onFailure {
                itemView.post {
                    Toast.makeText(context, it.toString(), Toast.LENGTH_LONG).show()
                }
            }
            MainActivity.activity?.hideProgress()
        }

    }

    val dfuLs = object : DfuProgressListener {
        override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            Snackbar.make(recyclerView, "$percent%", Snackbar.LENGTH_LONG).show()

        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {

            Snackbar.make(recyclerView, getString(R.string.onDeviceDisconnecting), Snackbar.LENGTH_LONG).show()

        }

        override fun onDeviceDisconnected(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDeviceDisconnected), Snackbar.LENGTH_LONG).show()

        }

        override fun onDeviceConnected(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDeviceConnected), Snackbar.LENGTH_LONG).show()

        }

        override fun onDfuProcessStarting(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDfuProcessStarting), Snackbar.LENGTH_LONG).show()

        }

        override fun onDfuAborted(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDfuAborted), Snackbar.LENGTH_LONG).show()

        }

        override fun onEnablingDfuMode(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onEnablingDfuMode), Snackbar.LENGTH_LONG).show()


        }

        override fun onDfuCompleted(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDfuCompleted), Snackbar.LENGTH_LONG).show()

        }


        override fun onFirmwareValidating(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onFirmwareValidating), Snackbar.LENGTH_LONG).show()


        }

        override fun onDfuProcessStarted(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDfuProcessStarted), Snackbar.LENGTH_LONG).show()

        }

        override fun onDeviceConnecting(deviceAddress: String) {

            Snackbar.make(recyclerView, getString(R.string.onDeviceConnecting), Snackbar.LENGTH_LONG).show()


        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {


            Snackbar.make(recyclerView, getString(R.string.onDfuProcessStarted) + ":" + message, Snackbar.LENGTH_LONG).show()

        }


    }

    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(activity!!, dfuLs)
    }


}
