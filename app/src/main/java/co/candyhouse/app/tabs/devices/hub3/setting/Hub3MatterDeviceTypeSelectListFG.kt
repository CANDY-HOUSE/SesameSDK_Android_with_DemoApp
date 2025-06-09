package co.candyhouse.app.tabs.devices.hub3.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgHub3MatterListBinding
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.MatterProductModel
import co.utils.recycle.GenericAdapter

data class MatterDevice(var name: String, var type: MatterProductModel = MatterProductModel.None)

class Hub3MatterDeviceTypeSelectListFG : BaseDeviceFG<FgHub3MatterListBinding>(), CHHub3Delegate {

    override fun getViewBinder() = FgHub3MatterListBinding.inflate(layoutInflater)

    var mMatterDeviceList = ArrayList<MatterDevice>()
    override fun onDestroyView() {
        super.onDestroyView()
        mMatterDeviceList.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind.leaderboardList.setEmptyView(bind.emptyView)
        bind.swiperefresh.isEnabled = false
        val ssmDevice = mDeviceViewModel.ssmDeviceLiveDataForMatter.value

        if (ssmDevice != null) {
            bind.menuTitle.text = ssmDevice.getNickname()
        } else {
            bind.menuTitle.text = getString(R.string.MatterSettings)
        }

        mMatterDeviceList.add(
            0,
            MatterDevice(getString(R.string.door_lock), MatterProductModel.DoorLock)
        )
        mMatterDeviceList.add(
            1,
            MatterDevice(getString(R.string.on_off_switch), MatterProductModel.OnOffSwitch)
        )
        bind.leaderboardList.adapter?.notifyDataSetChanged()

        getView()?.findViewById<View>(R.id.skip_matter_settings)?.setOnClickListener {
            if (ssmDevice != null) {
                (mDeviceModel.ssmLockLiveData.value!! as CHHub3).insertSesame(
                    ssmDevice,
                    ssmDevice.getNickname(),
                    MatterProductModel.None
                ) {
                    it.onSuccess { activity?.runOnUiThread { findNavController().navigateUp() } }
                }
            }
        }

        bind.leaderboardList.adapter = object : GenericAdapter<MatterDevice>(mMatterDeviceList) {
            override fun getLayoutId(position: Int, obj: MatterDevice): Int =
                R.layout.cell_matter_device

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(view), Binder<MatterDevice> {
                    override fun bind(data: MatterDevice, pos: Int) {
                        val name = itemView.findViewById<TextView>(R.id.sub_title)
                        name.text = data.name
                        itemView.setOnClickListener {
                            if (ssmDevice != null) {
                                (mDeviceModel.ssmLockLiveData.value!! as CHHub3).insertSesame(
                                    ssmDevice,
                                    ssmDevice.getNickname(),
                                    data.type
                                ) {
                                    it.onSuccess { activity?.runOnUiThread { bind.leaderboardList.post { findNavController().navigateUp() } } }
                                }
                            }
                        }
                    }
                }
        }
    }

}