package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSetAngleBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils

class SSM2SetAngleFG : BaseDeviceFG<FgSetAngleBinding>() {

    private val tag = "SSM2SetAngleFG"
    private var useSlidingDoorUi: Boolean = false
    private var longPressRunnable: Runnable? = null
    private var didTrigger5s: Boolean = false
    private val LONG_PRESS_MS = 5000L

    override fun getViewBinder() = FgSetAngleBinding.inflate(layoutInflater)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDeviceModel.ssmLockLiveData.value.apply {
            (this as? CHSesame2)?.let {
                updateLockView(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        updateLockView(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
            }
            (this as? CHSesame5)?.let {
                updateLockView(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        updateLockView(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
            }
            bind.ssmView.setOnClickListener {
                (this as? CHSesame2)?.toggle() {}
                (this as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {}
            }
            bind.slidingDoorView.setOnClickListener {
                (this as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {}
            }
            bind.setunlockZone.setOnClickListener {
                if ((this as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }

                (this as? CHSesame2)?.let { device ->
                    device.configureLockPosition(
                        device.mechSetting!!.lockPosition,
                        device.mechStatus!!.position
                    ) {
                        setLockFromDevice(device)
                    }
                }
                (this as? CHSesame5)?.let { device ->
                    device.configureLockPosition(
                        device.mechSetting!!.lockPosition,
                        device.mechStatus!!.position
                    ) {
                        setLockFromDevice(device, useSlidingDoorUi)
                    }
                }
            }
            bind.setlockZone.setOnClickListener {
                if ((this as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }
                (this as? CHSesame2)?.let { device ->
                    device.configureLockPosition(
                        device.mechStatus!!.position,
                        device.mechSetting!!.unlockPosition
                    ) {
                        setLockFromDevice(device)
                    }
                }
                (this as? CHSesame5)?.let { device ->
                    device.configureLockPosition(
                        device.mechStatus!!.position,
                        device.mechSetting!!.unlockPosition
                    ) {
                        setLockFromDevice(device, useSlidingDoorUi)
                    }
                }
            }
            bind.magnetZone.setOnClickListener {
                (this as? CHSesame5)?.magnet {}
            }
            bind.magnetZone.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        didTrigger5s = false
                        longPressRunnable?.let { bind.magnetZone.removeCallbacks(it) }

                        val dev = this
                        val ssm5 = (this as? CHSesame5)
                        if (dev == null || ssm5 == null) return@setOnTouchListener false

                        longPressRunnable = Runnable {
                            didTrigger5s = true
                            CHDeviceManager.vibrateDevice(view)
                            val (targetUiSliding, advType) = when (dev.productModel) {
                                CHProductModel.SS6ProSLiDingDoor -> false to 21.toByte()
                                CHProductModel.SS6Pro -> true to 32.toByte()
                                else -> return@Runnable
                            }

                            ssm5.sendAdvProductTypeCommand(data = byteArrayOf(advType)) { res ->
                                res.onSuccess {
                                    L.d(tag, "sendAdvProductTypeCommand success advType=$advType")
                                    activity?.runOnUiThread {
                                        useSlidingDoorUi = targetUiSliding
                                        bind.ssmView.visibility = if (useSlidingDoorUi) View.GONE else View.VISIBLE
                                        bind.slidingDoorView.visibility = if (useSlidingDoorUi) View.VISIBLE else View.GONE
                                        setLockFromDevice(dev, useSlidingDoorUi)
                                    }
                                }
                                res.onFailure { err ->
                                    L.e("SSM2SetAngleFG", "切换失败（message=${err.message}）")
                                }
                            }
                        }

                        bind.magnetZone.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        longPressRunnable?.let { bind.magnetZone.removeCallbacks(it) }
                        longPressRunnable = null
                        if (!didTrigger5s && event.actionMasked == MotionEvent.ACTION_UP) {
                            bind.magnetZone.performClick()
                        }
                        didTrigger5s = false
                        true
                    }

                    else -> true
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateLockView(device: CHDevices) {
        useSlidingDoorUi = device.productModel == CHProductModel.SS6ProSLiDingDoor

        bind.angleTv.text = (device.mechStatus?.position ?: 0).toString() + "°"
        bind.ssmView.visibility = if (useSlidingDoorUi) View.GONE else View.VISIBLE
        bind.slidingDoorView.visibility = if (useSlidingDoorUi) View.VISIBLE else View.GONE
        bind.magnetZone.visibility = if (device is CHSesame5) View.VISIBLE else View.GONE

        setLockFromDevice(device, useSlidingDoorUi)
    }

    private fun setLockFromDevice(device: CHDevices, showSliding: Boolean = false) {
        when (device) {
            is CHSesame2 -> {
                bind.ssmView.setLock(device)
            }

            is CHSesame5 -> {
                if (showSliding) {
                    bind.slidingDoorView.setLock(
                        pos = (device.mechStatus?.position ?: 0).toInt(),
                        lockPos = (device.mechSetting?.lockPosition ?: 0).toInt(),
                        unlockPos = (device.mechSetting?.unlockPosition ?: 0).toInt()
                    )
                } else {
                    bind.ssmView.setLock(device)
                }
            }
        }
    }

    override fun onDestroyView() {
        longPressRunnable?.let { bind.magnetZone.removeCallbacks(it) }
        longPressRunnable = null
        super.onDestroyView()
    }
}