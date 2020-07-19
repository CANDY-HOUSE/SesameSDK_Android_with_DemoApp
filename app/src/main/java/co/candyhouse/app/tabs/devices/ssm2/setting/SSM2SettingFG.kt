package co.candyhouse.app.tabs.devices.ssm2.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.app.R
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.wheelview.WheelView
import co.utils.wheelview.WheelviewAdapter
import co.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alertview.AlertView
import co.utils.alertview.objects.AlertAction
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_setting_main.*
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import pe.startapps.alerts.ext.inputTextAlert

class SSM2SettingFG : BaseSSMFG() {
    var titleTextView: TextView? = null
    var histagTextView: TextView? = null
    lateinit var mWheelView: WheelView<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_setting_main, container, false)
        titleTextView = view.findViewById(R.id.titlec)
        mWheelView = view.findViewById(R.id.wheelview)
        histagTextView = view.findViewById(R.id.histag)

        val secondSetting = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
        val providerAdapter = WheelviewAdapter(secondSetting.toList())
        mWheelView.setAdapter(providerAdapter)
        mWheelView.setWheelScrollListener(object : WheelView.WheelScrollListener {
            override fun changed(selected: Int, name: Any?) {
                val second = selected + 1
                mSesame?.enableAutolock(second) { res ->
                    res.onSuccess {
                        mWheelView?.post {
                            autolock_status.text = second.toString()
                            autolock_status.visibility = if (second == 0) View.GONE else View.VISIBLE
                            second_tv?.visibility = if (second == 0) View.GONE else View.VISIBLE
                            mWheelView.visibility = View.GONE
                        }
                    }
                }
            }
        })

        titleTextView?.text = SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())


        return view
    }

    override fun onResume() {
        super.onResume()
        DfuServiceListenerHelper.registerProgressListener(activity!!, dfuLs)
        mSesame?.delegate = object : CHSesame2Delegate {
            override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {

                if (status == CHSesame2Status.receiveBle) {
                    device.connnect() {}
                }
//                if (status.value == CHDeviceLoginStatus.logined) {
//                    mSesame?.getAutolockSetting() { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, second: UShort? ->
//                        autolock_status?.text = second.toString()
//                        autolock_status?.visibility = if (second!!.toInt() == 0) View.GONE else View.VISIBLE
//                        second_tv?.visibility = if (second!!.toInt() == 0) View.GONE else View.VISIBLE
//
//                        autolockSwitch?.apply {
//
//                            post {
//                                autolockSwitch.isChecked = second?.toInt() != 0
//                                setOnCheckedChangeListener { buttonView, isChecked ->
//                                    if (isChecked) {
//                                        mWheelView.visibility = View.VISIBLE
//                                    } else {
//                                        mSesame?.disableAutolock() { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, second: UShort? ->
//                                            autolock_status.text = second.toString()
//                                            autolock_status?.visibility = if (second!!.toInt() == 0) View.GONE else View.VISIBLE
//                                            second_tv?.visibility = if (second!!.toInt() == 0) View.GONE else View.VISIBLE
//
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }

            }
        }

    }

    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(activity!!, dfuLs)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ssmid_txt.text = mSesame?.deviceId.toString().toUpperCase()
        histagTextView?.text = mSesame?.getHistoryTag()?.let { String(it) }
        change_ssm_fr_zone.setOnClickListener {
            val alert = AlertView("", "", AlertStyle.IOS)
            alert.addAction(AlertAction(getString(R.string.ssm_update), AlertActionStyle.NEGATIVE) { action ->
                mSesame?.updateFirmware() { res ->
                    res.onSuccess {
                        val starter = DfuServiceInitiator(it.data.address)
                        starter.setZip(R.raw.a487e6c9)
                        starter.setPacketsReceiptNotificationsEnabled(false)
                        starter.setPrepareDataObjectDelay(400)
                        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                        starter.setDisableNotification(true)
                        starter.setForeground(false)
                        starter.start(activity!!, DfuService::class.java)
                    }
                }
            })
            alert.show(activity as AppCompatActivity)

        }
        mSesame?.getAutolockSetting() { res ->
            res.onSuccess {

                autolockSwitch?.apply {
                    post {
                        autolock_status?.text = it.data.toString()
                        autolock_status?.visibility = if (it.data!!.toInt() == 0) View.GONE else View.VISIBLE
                        second_tv?.visibility = if (it.data!!.toInt() == 0) View.GONE else View.VISIBLE
                        autolockSwitch.isChecked = it.data?.toInt() != 0
                        setOnCheckedChangeListener { buttonView, isChecked ->
                            if (isChecked) {
                                mWheelView?.visibility = View.VISIBLE
                            } else {
                                mSesame?.disableAutolock() { res ->
                                    mWheelView?.post {
                                        res.onSuccess {
                                            autolock_status?.text = it.data.toString()
                                            autolock_status?.visibility = if (it.data.toInt() == 0) View.GONE else View.VISIBLE
                                            second_tv?.visibility = if (it.data.toInt() == 0) View.GONE else View.VISIBLE
                                            mWheelView?.visibility = View.GONE
                                        }
                                    }


                                }
                            }
                        }
                    }
                }
            }


        }
        mSesame?.getVersionTag() { res ->
            res.onSuccess {

                firmwareVersion?.post {
                    firmwareVersion?.text = it.data
                }
            }


        }
        backicon.setOnClickListener { findNavController().navigateUp() }
        chenge_angle_zone.setOnClickListener {
            findNavController().navigate(R.id.action_SSM2SettingFG_to_SSM2SetAngleFG)
        }
        change_ssm_histag_zone.setOnClickListener {
            context?.inputTextAlert("change history tag", "change history tag", mSesame?.getHistoryTag()?.let { String(it) }) {
                confirmButtonWithText("OK") { alert, changeTag ->
                    L.d("hcia", "changeTag:" + changeTag)

                    mSesame?.setHistoryTag(changeTag.toByteArray()) {
                        L.d("hcia", "it:" + it)
                        L.d("hcia", "histag:" + histag)
                        histagTextView?.post { histagTextView?.text = changeTag }
                    }


                    dismiss()
                }
                cancelButton("Cancel")
            }?.show()
        }
        change_ssm_name_zone.setOnClickListener {
            context?.inputTextAlert(getString(R.string.give_cool_name), getString(R.string.change_sesame_name), SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())) {
                confirmButtonWithText("OK") { alert, name ->
                    SharedPreferencesUtils.preferences.edit().putString(mSesame?.deviceId.toString(), name).apply()
                    titleTextView?.text = name
                    dismiss()
                }
                cancelButton("Cancel")
            }?.show()
        }
        delete_zone.setOnClickListener {
            val alert = AlertView("", "", AlertStyle.IOS)
            alert.addAction(AlertAction(getString(R.string.ssm_delete), AlertActionStyle.NEGATIVE) { action ->
                mSesame?.resetSesame {
                    SharedPreferencesUtils.preferences.edit().remove(mSesame?.deviceId.toString()).apply()
                    findNavController().navigateUp()
                    findNavController().navigateUp()
                }
            })
            alert.show(activity as AppCompatActivity)
        }
        drop_zone.setOnClickListener {
            val alert = AlertView("", "", AlertStyle.IOS)
            alert.addAction(AlertAction(getString(R.string.ssm_delete), AlertActionStyle.NEGATIVE) { action ->
                mSesame?.dropKey()
                SharedPreferencesUtils.preferences.edit().remove(mSesame?.deviceId.toString()).apply()
                findNavController().navigateUp()
                findNavController().navigateUp()
            })
            alert.show(activity as AppCompatActivity)
        }
        share_zone.setOnClickListener {
            MyKEYFG.ssm2key = mSesame?.getKey()
            MyKEYFG.keyname = mSesame?.deviceId.toString()
            findNavController().navigate(R.id.action_SSM2SettingFG_to_myKEYFG)

        }

    }//end view created


    val dfuLs = object : DfuProgressListener {
        override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            firmwareVersion.post {
                firmwareVersion.text = "$percent%"
            }
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDeviceDisconnecting)//初期化中…
            }
        }

        override fun onDeviceDisconnected(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDeviceDisconnected)//初期化中…
            }
        }

        override fun onDeviceConnected(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDeviceConnected)//初期化中…
            }
        }

        override fun onDfuProcessStarting(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDfuProcessStarting)//初期化中…
            }
        }

        override fun onDfuAborted(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDfuAborted)//初期化中…
            }
        }

        override fun onEnablingDfuMode(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onEnablingDfuMode)//初期化中…
            }
        }

        override fun onDfuCompleted(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDfuCompleted)//完了


//                mSesame?.getVersionTag() { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, tag_ts: Pair<String, Long>? ->
//                    firmwareVersion.post {
//                        firmwareVersion.text = tag_ts?.first
//                    }
//                }
            }
//            firmwareVersion.postDelayed(Runnable {
//                mSesame?.getVersionTag() { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, tag_ts: Pair<String, Long>? ->
//                    firmwareVersion.post {
//                        firmwareVersion.text = tag_ts?.first
//                    }
//                }
//            }, 5000)
        }

        override fun onFirmwareValidating(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onFirmwareValidating)//初期化中…
            }
        }

        override fun onDfuProcessStarted(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDfuProcessStarted)//初期化中…
            }
        }

        override fun onDeviceConnecting(deviceAddress: String) {
            firmwareVersion.post {
                firmwareVersion.text = getString(R.string.onDeviceConnecting)//初期化中…
            }
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
            firmwareVersion.post {
                L.d("hcia", "errorType:" + errorType)
                L.d("hcia", "message:" + message)
                L.d("hcia", "error:" + error)
                L.d("hcia", "deviceAddress:" + deviceAddress)
                firmwareVersion.text = getString(R.string.onDfuProcessStarted) + ":" + message
            }
        }


    }

}

