package co.candyhouse.app.tabs.devices.wm2.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgWm2SelectLockerListBinding
import co.candyhouse.app.databinding.FgWm2SettingBinding
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.open.device.*
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter

class WM2SelectLockerListFG : BaseDeviceFG<FgWm2SelectLockerListBinding>(), CHWifiModule2Delegate {

    fun  isLock(it: CHDevices):Boolean{


        return   (it is CHSesame2) || (it is CHSesame5) || (it is CHSesameBot) || (it is CHSesameBike)
}
    override fun getViewBinder()= FgWm2SelectLockerListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.leaderboardList.apply {
            adapter = object : GenericAdapter<CHDevices>(mDeviceModel.myChDevices.value.filter {
              isLock(it)&&!hasAddedOrIsGuestKey(it)

            }.toMutableList())

            {
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
        } //end bind.leaderboardList.apply
    }

}