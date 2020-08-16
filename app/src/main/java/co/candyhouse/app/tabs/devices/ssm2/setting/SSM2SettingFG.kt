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
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.login.toastMSG
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Status
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
    lateinit var mWheelView: WheelView<String>
    lateinit var mAdvWheelView: WheelView<String>
    lateinit var mTxpWheelView: WheelView<String>
    lateinit var histagTxt: TextView

    var mTxpower: Byte? = null
    var mAdvInterval: Double? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_setting_main, container, false)
        mWheelView = view.findViewById(R.id.wheelview)
        mAdvWheelView = view.findViewById(R.id.adv_itv_wh)
        mTxpWheelView = view.findViewById(R.id.adv_txp_wh)
        histagTxt = view.findViewById(R.id.histag_txt)


        val secondSetting = arrayOf(
                getString(R.string.sec3),
                getString(R.string.sec5),
                getString(R.string.sec7),
                getString(R.string.sec10),
                getString(R.string.sec15),
                getString(R.string.sec30),
                getString(R.string.min1),
                getString(R.string.min2),
                getString(R.string.min5),
                getString(R.string.min10),
                getString(R.string.min15),
                getString(R.string.min30),
                getString(R.string.hr1)
        )
        val advIntervals = arrayListOf<String>(

                "546.25 ms",
                "760 ms",
                "852.5 ms",
                "1022.5 ms",
                "1285 ms",
                "20.0 ms",
                "152.5 ms",
                "211.25 ms",
                "318.75 ms",
                "417.5 ms"
        )
        val advintervalSettingValue: Array<Double> = arrayOf(
                546.25.toDouble(),
                760.toDouble(),
                852.5.toDouble(),
                1022.5.toDouble(),
                1285.toDouble(),
                20.0.toDouble(),
                152.5.toDouble(),
                211.25.toDouble(),
                318.75.toDouble(),
                417.5.toDouble()
        )
        val secondSettingValue = arrayOf(
                3,
                5,
                7,
                10,
                15,
                30,
                60,
                60 * 2,
                60 * 5,
                60 * 10,
                60 * 15,
                60 * 30,
                60 * 60
        )
        val dBmsValus: Array<Byte> = arrayOf(
                -4, 0, 3, 4, -40, -20, -16, -12, -8
        )
        val dBmsSetting: Array<String> = arrayOf(
                "-4 dBm", "0 dBm", "3 dBm", "4 dBm", "-40 dBm", "-20 dBm", "-16 dBm", "-12 dBm", "-8 dBm"
        )
        mWheelView.setAdapter(WheelviewAdapter(secondSetting.toList()))
        mWheelView.setWheelScrollListener(object : WheelView.WheelScrollListener {
            override fun changed(selected: Int, name: Any?) {
                val second = secondSettingValue.get(selected)
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

        mAdvWheelView.setAdapter(WheelviewAdapter(advIntervals.toList()))
        mAdvWheelView.setWheelScrollListener(object : WheelView.WheelScrollListener {
            override fun changed(selected: Int, name: Any?) {
                val interval = advintervalSettingValue.get(selected)
                mAdvInterval = interval
//                L.d("hcia", "mAdvInterval:" + mAdvInterval)
//                L.d("hcia", "selected:" + selected)
            }
        }, true)

        mTxpWheelView.setAdapter(WheelviewAdapter(dBmsSetting.toList()))
        mTxpWheelView.setWheelScrollListener(object : WheelView.WheelScrollListener {
            override fun changed(selected: Int, name: Any?) {
                val txp = dBmsValus.get(selected)
                mTxpower = txp
//                L.d("hcia", "mTxpower:" + mTxpower)
//                L.d("hcia", "selected:" + selected)
            }
        }, true)

        return view
    }

    override fun onResume() {
        super.onResume()
        DfuServiceListenerHelper.registerProgressListener(activity!!, dfuLs)
        mSesame?.delegate = object : CHSesame2Delegate {
            override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
                if (status == CHSesame2Status.receivedBle) {
                    device.connect() {}
                }
            }
        }

    }


    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titlec?.text = SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())
        check_adv.setOnClickListener {
            L.d("hcia", "mTxpower:" + mTxpower + " mAdvInterval:" + mAdvInterval!!.toShort())
            mSesame?.updateBleAdvParameter(mAdvInterval!!, mTxpower!!) {
                it.onSuccess {
                    adv_interval_txt?.post {
                        adv_interval_txt?.text = mAdvInterval.toString() + " ms"
                        adv_txpower_txt?.text = mTxpower.toString() + " dBm"
                        toastMSG("reboot! advInterval:" + mAdvInterval + " txpower:" + mTxpower)
                    }
                }
            }
        }
        adv_interval_zone.setOnClickListener {
            adv_itv_zone.visibility = View.VISIBLE
        }
        adv_txpower_zone.setOnClickListener {
            adv_itv_zone.visibility = View.VISIBLE
        }
        cancle_adv.setOnClickListener {
            L.d("hcia", "check_adv:")
            adv_itv_zone.visibility = View.GONE
        }
        ssmid_txt.text = mSesame?.deviceId.toString().toUpperCase()
        histag_txt?.text = mSesame?.getHistoryTag()?.let { String(it) }
        change_ssm_fr_zone.setOnClickListener {
            val alert = AlertView("", "", AlertStyle.IOS)
            alert.addAction(AlertAction(getString(R.string.ssm_update), AlertActionStyle.NEGATIVE) { action ->
                mSesame?.updateFirmware() { res ->
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
            })
            alert.show(activity as AppCompatActivity)

        }
        mSesame?.getBleAdvParameter { res ->
            res.getOrNull()?.data?.let {
                adv_interval_txt?.post {
                    adv_interval_txt?.text = it.interval.toString() + " ms"
                    adv_txpower_txt?.text = it.txPower.toString() + " dBm"
                }

//                L.d("hcia", "it.interval:" + it.interval)
//                L.d("hcia", "it.txPower:" + it.txPower)
            }
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
            context?.inputTextAlert("Change history tag", "Change history tag", mSesame?.getHistoryTag()?.let { String(it) }) {
                confirmButtonWithText("OK") { alert, changeTag ->
                    L.d("hcia", "changeTag:" + changeTag)

                    mSesame?.setHistoryTag(changeTag.toByteArray()) {
                        L.d("hcia", "it:" + it)
                        L.d("hcia", "histag_txt:" + histagTxt)
                        histagTxt?.post {
                            histagTxt?.text = changeTag
                        }
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
                    titlec?.text = name
                    dismiss()
                }
                cancelButton("Cancel")
            }?.show()
        }
        delete_zone.setOnClickListener {
            val alert = AlertView("", "", AlertStyle.IOS)
            alert.addAction(AlertAction(getString(R.string.ssm_delete), AlertActionStyle.NEGATIVE) { action ->
                mSesame?.resetSesame2 {
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
                mSesame?.dropKey() {}
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


    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(activity!!, dfuLs)
    }

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

