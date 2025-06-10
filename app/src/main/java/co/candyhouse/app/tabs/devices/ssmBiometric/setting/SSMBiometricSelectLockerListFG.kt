package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpSelectLockerListBinding
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter

class SSMBiometricSelectLockerListFG : BaseDeviceFG<FgSsmTpSelectLockerListBinding>(),
    CHWifiModule2Delegate {
    override fun getViewBinder() = FgSsmTpSelectLockerListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.leaderboardList.apply {
            val filteredDevices =
                when ((mDeviceModel.ssmLockLiveData.value as CHSesameConnector).productModel) {
                    CHProductModel.SSMOpenSensor -> {
                        val os3Lockers = listOf(
                            CHProductModel.BiKeLock2,
                            CHProductModel.SS5,
                            CHProductModel.SS5PRO,
                            CHProductModel.SS5US,
                            CHProductModel.SesameBot2
                        )
                        val sesame2KeyDevices = mDeviceModel.myChDevices.value.filter { device ->
                            (mDeviceViewModel.ssmLockLiveData.value as? CHSesameConnector)?.ssm2KeysMap?.keys?.contains(
                                device.deviceId.toString()
                            ) == true
                        }
                        val hasLockInSesame2Keys = sesame2KeyDevices.any { device ->
                            device.productModel in os3Lockers
                        }
                        val hasHub3InSesame2Keys =
                            sesame2KeyDevices.any { it.productModel == CHProductModel.Hub3 }
                        mDeviceModel.myChDevices.value.filter { it ->
                            val isAllowedProduct =
                                (it.productModel == CHProductModel.Hub3) || ((it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.SesameBot1)
                            when {
                                !isAllowedProduct -> false
                                hasLockInSesame2Keys && it.productModel == CHProductModel.Hub3 -> false
                                hasHub3InSesame2Keys && it.productModel in os3Lockers -> false
                                else -> true
                            }
                        }
                    }

                    CHProductModel.Hub3 -> mDeviceModel.myChDevices.value.filter { it.productModel == CHProductModel.SSMTouchPro || it.productModel == CHProductModel.SS5PRO || it.productModel == CHProductModel.SS5 }
                    CHProductModel.Remote -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.BiKeLock && it.productModel != CHProductModel.SesameBot1 }
                    CHProductModel.RemoteNano -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.BiKeLock && it.productModel != CHProductModel.SesameBot1 }
                    CHProductModel.SSMFace -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.BiKeLock && it.productModel != CHProductModel.SesameBot1 }
                    CHProductModel.SSMFacePro -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.BiKeLock && it.productModel != CHProductModel.SesameBot1 }
                    CHProductModel.SSMFaceProAI -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.BiKeLock && it.productModel != CHProductModel.SesameBot1 }
                    CHProductModel.SSMFaceAI -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SS2 && it.productModel != CHProductModel.SS4 && it.productModel != CHProductModel.BiKeLock && it.productModel != CHProductModel.SesameBot1 }
                    else -> mDeviceModel.myChDevices.value.filter { (it is CHSesameLock) && it.productModel != CHProductModel.SSMOpenSensor && it.productModel != CHProductModel.SesameBot1 }
                }

            adapter = object : GenericAdapter<CHDevices>(filteredDevices.filter {
                !hasAddedOrIsGuestKey(it)
            }.toMutableList()) {
                override fun getLayoutId(position: Int, obj: CHDevices): Int = R.layout.key_cell
                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                        override fun bind(locker: CHDevices, pos: Int) {
                            val title = itemView.findViewById<TextView>(R.id.title)
                            val wifi_img = itemView.findViewById<View>(R.id.wifi_img)
                            wifi_img.visibility = View.GONE
                            title.text = SharedPreferencesUtils.preferences.getString(
                                locker.deviceId.toString(),
                                locker.productModel.modelName()
                            )
                            itemView.setOnClickListener {
                                (mDeviceModel.ssmLockLiveData.value as CHSesameConnector).insertSesame(
                                    locker
                                ) {
                                    it.onSuccess {
                                        if (isAdded && !isDetached) {
                                            bind.leaderboardList.post {
                                                SharedPreferencesUtils.preferences.edit().putString(
                                                    locker.deviceId.toString(),
                                                    title.text.toString()
                                                ).apply()
                                                view.findNavController().navigateUp()
                                            }
                                        }
                                    }
                                    it.onFailure {
                                        L.d("SSMBiometricSelectLockerListFG","insert sesame error! ${title.text}")
                                    }
                                }
                            }
                        }
                    }
            }
        } // end bind.leaderboardList.apply
    }
}