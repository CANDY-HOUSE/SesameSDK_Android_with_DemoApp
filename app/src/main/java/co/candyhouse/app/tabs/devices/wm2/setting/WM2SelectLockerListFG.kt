package co.candyhouse.app.tabs.devices.wm2.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.*
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_wm2_scan_list.*

class WM2SelectLockerListFG : BaseDeviceFG(R.layout.fg_wm2_select_locker_list), CHWifiModule2Delegate {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaderboard_list.apply {
            adapter = object : GenericAdapter<CHDevices>(mDeviceModel.myChDevices.value.filter { (it is CHSesame2) || (it is CHSesame5) || (it is CHSesameBot) || (it is CHSesameBike) }) {
                override fun getLayoutId(position: Int, obj: CHDevices): Int = R.layout.key_cell
                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                        object : RecyclerView.ViewHolder(view), Binder<CHDevices> {

                            override fun bind(locker: CHDevices, pos: Int) {
                                val title = itemView.findViewById<TextView>(R.id.title)
                                val wifi_img = itemView.findViewById<View>(R.id.wifi_img)
                                wifi_img.visibility = View.GONE
                                title.text = SharedPreferencesUtils.preferences.getString(locker.deviceId.toString(), locker.productModel.modelName())
                                itemView.setOnClickListener {
                                    (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).insertSesames(locker) {
                                        it.onSuccess {
                                            activity?.runOnUiThread {
                                                SharedPreferencesUtils.preferences.edit().putString(locker.deviceId.toString(), title.text.toString()).apply()
                                                findNavController().navigateUp()
                                            }
                                        }
                                    }
                                }
                            }
                        }
            }
        } //end leaderboard_list.apply
    }

}