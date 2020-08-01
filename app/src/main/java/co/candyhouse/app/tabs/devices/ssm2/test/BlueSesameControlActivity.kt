package co.candyhouse.app.tabs.devices.ssm2.test

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleManagerDelegate
import co.candyhouse.sesame.ble.CHSesame2Status
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.sesame.deviceprotocol.*
import co.candyhouse.app.R
import kotlinx.android.synthetic.main.activity_ble_control.*


class BlueSesameControlActivity : AppCompatActivity(), CHSesame2Delegate, CHBleManagerDelegate {
    companion object {
        @JvmField
        var ssm: CHSesame2? = null
    }

    var lockDegree: Short = 0
    var unlockDegree: Short = 0
    var nowDegree: Short = 0

    override fun onResume() {
        super.onResume()
        CHBleManager.delegate = this
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_control)
        ssm?.delegate = this

        connectStatus.setText(ssm!!.deviceStatus.toString() + " :" + ssm!!.deviceStatus.value.toString())
        sesame_uuid.setText(ssm!!.deviceId.toString())
        registerstatus.setText(if (ssm!!.isRegistered) "register" else "unregister")

        register.setOnClickListener {
            ssm?.registerSesame2 { res -> }
        }


        connectBtn.setOnClickListener { ssm?.connnect() {} }
        disconnectBtn.setOnClickListener { ssm?.disconnect() {} }
        setAngle.setOnClickListener { ssm?.configureLockPosition(lockDegree, unlockDegree) {} }
        setLockAngle.setOnClickListener {
            setLockAngle.text = "" + nowDegree
            lockDegree = nowDegree
        }
        setUnLockAngle.setOnClickListener {
            setUnLockAngle.text = "" + nowDegree
            unlockDegree = nowDegree
        }
        lockBtn.setOnClickListener { ssm?.lock() {} }
        unlockBtn.setOnClickListener { ssm?.unlock() {} }
        resetSSM.setOnClickListener { ssm?.resetSesame2 { } }


        enableAutolock.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Second")
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setView(input)
            builder.setPositiveButton("OK", { dialogInterface: DialogInterface, i: Int ->
                val inputSecond = input.text.toString()
                ssm?.enableAutolock(inputSecond.toInt()) { res ->
                    res.onSuccess {
                        autolockStatus.text = it.data.toString()
                    }
                }
            })
            builder.setNegativeButton("Cancle", { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.cancel()
            })
            builder.show()

        }
        disableAutolock.setOnClickListener {
            ssm?.disableAutolock() { res ->
                res.onSuccess {
                    autolockStatus.text = it.data.toString()
                }

            }
        }
        readAutolock.setOnClickListener {
            ssm?.getAutolockSetting() { res ->
                res.onSuccess {
                    autolockStatus.text = it.data.toString()
                }

            }
        }
        firmwareVersion.setOnClickListener {
            ssm?.getVersionTag() { res ->
                res.onSuccess {
                    firmwareVersion.post {
                        firmwareVersion.setText(it.data)
                    }
                }

            }
        }
        ssm?.getVersionTag() { res ->
            res.onSuccess {
                firmwareVersion.post {
                    firmwareVersion.setText(it.data)
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
        connectStatus.setText(ssm!!.deviceStatus.toString() + " :" + ssm!!.deviceStatus.value.toString())
    }


    override fun onMechStatusChanged(device: CHSesame2, status: CHSesame2MechStatus, intention: CHSesame2Intention) {
        nowAngle.setText("angle:" + status.position)
        nowDegree = status.position
        lockState.setText(if (status.isInLockRange) "locked" else if (status.isInUnlockRange) "unlocked" else "moved")
        moveState.setText(intention.value)
    }

    override fun onMechSettingChanged(device: CHSesame2, settings: CHSesame2MechSettings) {
        setLockAngle.setText(settings.lockPosition.toString())
        setUnLockAngle.setText(settings.unlockPosition.toString())
    }


}
