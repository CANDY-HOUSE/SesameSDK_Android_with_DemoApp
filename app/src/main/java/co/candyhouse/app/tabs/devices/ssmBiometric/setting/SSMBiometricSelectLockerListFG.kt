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
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter

class SSMBiometricSelectLockerListFG : BaseDeviceFG<FgSsmTpSelectLockerListBinding>(),
    CHWifiModule2Delegate {
    override fun getViewBinder() = FgSsmTpSelectLockerListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.leaderboardList.apply {
            val allLocks = listOf(
                CHProductModel.SS2,
                CHProductModel.SS4,
                CHProductModel.BiKeLock,
                CHProductModel.BiKeLock2,
                CHProductModel.BiKeLock3,
                CHProductModel.SS5,
                CHProductModel.SS5PRO,
                CHProductModel.SS5US,
                CHProductModel.SesameBot1,
                CHProductModel.SesameBot2,
                CHProductModel.BLEConnector,
                CHProductModel.SS6Pro,
                CHProductModel.SSM_MIWA
            )

            val currentProductModel = (mDeviceModel.ssmLockLiveData.value as CHSesameConnector).productModel

            val filteredDevices = when (currentProductModel) {
                CHProductModel.SSMOpenSensor, CHProductModel.SSMOpenSensor2 -> {
                    val sesame2KeyDevices = mDeviceModel.myChDevices.value.filter { device ->
                        (mDeviceModel.ssmLockLiveData.value as? CHSesameConnector)?.ssm2KeysMap?.keys?.contains(
                            device.deviceId.toString()
                        ) == true
                    }

                    val hasLockInSesame2Keys = sesame2KeyDevices.any { it.productModel in allLocks }
                    val hasHub3InSesame2Keys = sesame2KeyDevices.any { it.productModel == CHProductModel.Hub3 }

                    val allowedProducts = when {
                        hasLockInSesame2Keys -> allLocks
                        hasHub3InSesame2Keys -> listOf(CHProductModel.Hub3)
                        else -> listOf(CHProductModel.Hub3) + allLocks
                    }

                    mDeviceModel.myChDevices.value.filter { allowedProducts.contains(it.productModel) }
                }

                else -> {
                    mDeviceModel.myChDevices.value.filter { allLocks.contains(it.productModel) }
                }
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
                                        L.d("SSMBiometricSelectLockerListFG", "insert sesame error! ${title.text}")
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}