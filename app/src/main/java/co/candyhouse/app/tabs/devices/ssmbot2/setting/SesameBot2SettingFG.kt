package co.candyhouse.app.tabs.devices.ssmbot2.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSsmBikeSettingBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.utils.alertview.fragments.toastMSG
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SesameBot2SettingFG : BaseDeviceSettingFG<FgSsmBikeSettingBinding>() {
    //检查类型再转换（来自firebase crash）
    private val bot2 by lazy {
        val device = mDeviceModel.ssmLockLiveData.value
        if (device is CHSesameBot2) {
            device
        } else {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("expected_type", "CHSesameBot2")
                setCustomKey("actual_type", device!!.javaClass.simpleName)
                setCustomKey("device_uuid", device.deviceId.toString())
                log("Wrong device type in SesameBot2SettingFG: ${device.javaClass.simpleName}")
            }
            toastMSG("デバイスタイプが一致しません。リストを更新してください。")
            findNavController().navigateUp()
            null
        }
    }

    private val bot2ScriptCurIndexKey by lazy {
        bot2?.deviceId.toString() + "_ScriptIndex"
    }

    override fun onResume() {
        super.onResume()
        if (bot2 == null) {
            return
        }
        (mDeviceModel.ssmosLockDelegates[bot2!!]) = object : CHDeviceStatusDelegate {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
            }

            override fun onMechStatus(device: CHDevices) {
                view?.findViewById<TextView>(R.id.battery)?.post {
                    view?.findViewById<TextView>(R.id.battery)?.text = "${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                }
            }
        }.bindLifecycle(viewLifecycleOwner)
    }

    override fun getViewBinder() = FgSsmBikeSettingBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (bot2 == null) {
            return
        }
        super.onViewCreated(view, savedInstanceState)
        getView()?.findViewById<View>(R.id.click_script_zone)?.setOnClickListener { findNavController().navigate(R.id.to_SesameClickScriptFG) }
        (mDeviceModel.ssmosLockDelegates[bot2!!]) = object : CHDeviceStatusDelegate {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                // L.d("[say]", "[BaseDeviceSettingFG.kt][onBleDeviceStatusChanged]")
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
            }

            override fun onMechStatus(device: CHDevices) {
                view.findViewById<TextView>(R.id.battery)?.post {
                    view.findViewById<TextView>(R.id.battery)?.text = "${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                }
            }
        }.bindLifecycle(viewLifecycleOwner)
        bind.llview.setOnClickListener {
            showWheel(false)
        }

        bot2?.getScriptNameList {
            it.onSuccess {
                L.d("hcia", "setting getNowScript:$it")
                activity?.runOnUiThread {
                    val curIdx: Int = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
                    it.data.curIdx = curIdx.toUByte()
                    bind.scriptIdTxt.apply {
                        text = it.data.curIdx.toString()
                        val int = textToIntValue(bind.scriptIdTxt)
                        bind.wheelview.setCurrentPosition(int)
                    }
                }
            }
        }
        usePressText()
        bind.scriptIdTxt.setOnClickListener {
            L.d("hcia", "bind.scriptIdTxt.setOnClickListener")
            showWheel(!bind.wheelview.isVisible)
        }
        loadWheel(bot2!!)

    }//end view created

    private fun textToIntValue(tv: TextView): Int {
        return try {
            tv.text?.toString()?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun loadWheel(bot2: CHSesameBot2) {
        bind.wheelview.setItems(listItem())
        bind.wheelview.setInitPosition(SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0))
        bind.wheelview.setListener { select ->
            try {
                bot2.selectScript(select.toUByte()) {
                    SharedPreferencesUtils.preferences.edit().putInt(bot2ScriptCurIndexKey, select).apply()
                }
                bind.scriptIdTxt.text = select.toString()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }

    private fun showWheel(isShow: Boolean) {
        bind.wheelview.visibility = if (isShow) View.VISIBLE else View.GONE
        bind.wheelview.setCurrentPosition(SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0))
        runFiveThread(3000, isShow) {
            if (bind.wheelview != null) {
                bind.wheelview.post {
                    bind.wheelview.visibility = View.GONE
                }
            }
        }
    }

    private var job: Job? = null

    private fun runFiveThread(time: Int, isShow: Boolean, call: () -> Unit) {
        runBlocking {
            job?.cancel()
            if (isShow) {
                job = GlobalScope.launch {
                    delay(time.toLong()) // 5 秒延迟
                    call.invoke()
                }
            }
        }
    }

    // 开启一个协程

    private fun listItem(): List<String>? {
        return bot2?.scripts?.events?.map { String(it.name, Charsets.UTF_8) }
    }
}
