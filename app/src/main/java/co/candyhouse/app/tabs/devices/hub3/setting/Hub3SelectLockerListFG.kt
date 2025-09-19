package co.candyhouse.app.tabs.devices.hub3.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgHub3SelectLockerListBinding
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.open.device.MatterProductModel
import co.candyhouse.sesame.utils.L
import co.utils.recycle.GenericAdapter

class Hub3SelectLockerListFG : BaseDeviceFG<FgHub3SelectLockerListBinding>(), CHWifiModule2Delegate {

    // private val mDeviceViewModel: CHDeviceViewModel by activityViewModels()
    private val targetProductModels = setOf(
        CHProductModel.SSMTouchPro,
        CHProductModel.SSMTouch,
        CHProductModel.SS5PRO,
        CHProductModel.SS5,
        CHProductModel.SS5US,
        CHProductModel.SesameBot2,
        CHProductModel.BiKeLock2,
        CHProductModel.SSMFace,
        CHProductModel.SSMFacePro,
        CHProductModel.SSMFaceProAI,
        CHProductModel.SSMFaceAI,
        CHProductModel.BLEConnector
    )


    override fun getViewBinder() = FgHub3SelectLockerListBinding.inflate(layoutInflater)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.leaderboardList.apply {
            val filteredDevices = mDeviceModel.myChDevices.value.filter {
                (it.productModel in targetProductModels) && !(it.getKey().secretKey.contains("000000")) && !hasAddedOrIsGuestKey(it)
            }
            adapter = object : GenericAdapter<CHDevices>(filteredDevices.toMutableList()) {
                override fun getLayoutId(position: Int, obj: CHDevices): Int = R.layout.hub3_select_ssm_list_cell
                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                        object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                            override fun bind(locker: CHDevices, pos: Int) {
                                /*                        val title = itemView.findViewById<TextView>(R.id.title)
                                                        title.text = SharedPreferencesUtils.preferences.getString(locker.deviceId.toString(), locker.productModel.modelName())
                        */
                                val title = itemView.findViewById<TextView>(R.id.title)
                                title.text = locker.deviceId.toString()
                                getDeviceNameById(locker.deviceId.toString())?.apply {
                                    L.d("deviceNameById", this)
                                    title.text = this
                                }
                                itemView.setOnClickListener {
                                    if ((locker.productModel == CHProductModel.SSMFace) || (locker.productModel == CHProductModel.SSMTouchPro) || (locker.productModel == CHProductModel.SSMTouch) || (locker.productModel == CHProductModel.SSMFacePro) || (locker.productModel == CHProductModel.SSMFaceProAI) || (locker.productModel == CHProductModel.SSMFaceAI) ) {
                                        (mDeviceModel.ssmLockLiveData.value!! as CHHub3).insertSesame(locker, locker.getNickname(), MatterProductModel.None) {
                                            it.onSuccess { activity?.runOnUiThread { findNavController().navigateUp() } }
                                        }
                                        return@setOnClickListener
                                    } else {
                                        when (locker.productModel) {
                                            CHProductModel.SesameBot2 -> {
                                                (mDeviceModel.ssmLockLiveData.value!! as CHHub3).insertSesame(locker, locker.getNickname(), MatterProductModel.OnOffSwitch) {
                                                    it.onSuccess { activity?.runOnUiThread { findNavController().navigateUp() } }
                                                }
                                            }
                                            CHProductModel.SSMOpenSensor, CHProductModel.SSMOpenSensor2 -> { }
                                            else -> {
                                                (mDeviceModel.ssmLockLiveData.value!! as CHHub3).insertSesame(locker, locker.getNickname(), MatterProductModel.DoorLock) {
                                                    it.onSuccess { activity?.runOnUiThread { findNavController().navigateUp() } }
                                                }
                                            }
                                        }

                                    }
                                    // mDeviceViewModel.ssmDeviceLiveDataForMatter.value = locker
                                    // safeNavigate(R.id.to_Hub3MatterDeviceTypeSelectListFG)

                                }
                            }
                        }
            }
        } // bind.leaderboardList
    }// onViewCreated
}// end Hub3SelectLockerListFG