package co.candyhouse.app.tabs.friend

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
//import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgFriendSelectLockerListBinding
//import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.devices.model.CHUserViewModel
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.server.CHUser
import co.candyhouse.server.CHUserKey
import co.candyhouse.server.CHUserKeyFriendID
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter

import java.text.SimpleDateFormat
import java.util.*

class FriendSelectLockerListFG : BaseDeviceFG<FgFriendSelectLockerListBinding>(), CHWifiModule2Delegate {

    private val userViewModel: CHUserViewModel by activityViewModels()
    override fun getViewBinder()= FgFriendSelectLockerListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.leaderboardList.apply {
            adapter = object : GenericAdapter<CHDevices>(mDeviceModel.myChDevices.value.filter {
                it.getLevel() != 2 &&
                        it.productModel.productType()!= CHProductModel.WM2.productType()}.toMutableList()

            ) {
                override fun getLayoutId(position: Int, obj: CHDevices): Int = R.layout.key_cell
                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                        var title = itemView.findViewById<TextView>(R.id.title)
                        var wifi_img = itemView.findViewById<View>(R.id.wifi_img)
                        override fun bind(locker: CHDevices, pos: Int) {
                            wifi_img.visibility = View.GONE
                            title.text = SharedPreferencesUtils.preferences.getString(locker.deviceId.toString(), locker.productModel.modelName())
                            itemView.setOnClickListener {
                                AlertView(title.text.toString(), "", AlertStyle.IOS).apply {


                                    if (locker.getLevel() == 0) {
                                        addAction(AlertAction(getString(R.string.owner), AlertActionStyle.DEFAULT) {
                                            shareKey(locker, 0)
                                        })
                                    }
                                    if (locker.getLevel() == 0 || locker.getLevel() == 1) {
                                        addAction(AlertAction(getString(R.string.manager), AlertActionStyle.DEFAULT) {
                                            shareKey(locker, 1)
                                        })
                                    }

                                    addAction(AlertAction(getString(R.string.guest), AlertActionStyle.DEFAULT) {
                                        if (locker.getLevel() == 2) {
                                            return@AlertAction
                                        }

                                        locker.createGuestKey(SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())) {
                                            it.onSuccess {
                                                shareGuestKey(userViewModel.userViewModel.value!!, cheyKeyToUserKey(it.data, 2, locker.getNickname()))
//                                                    shareKey(locker, 2)
                                            }
                                        }


                                    })
                                    show(activity as AppCompatActivity)
                                }
                            }
                        }
                    }
            }
        }//end bind.leaderboardList.apply
    }

    private fun shareGuestKey(data: CHUser, userKey: CHUserKey) {
        CHLoginAPIManager.giveFriendDevice(CHUserKeyFriendID(userKey, data.sub)) {
            it.onSuccess {
                L.d("hcia", "給朋友鑰匙:" + it.data)
                view?.post {
                    findNavController().navigateUp()
                }
            }
            it.onFailure {
                L.d("hcia", "給朋友鑰匙 失敗 it:$it")
            }
        }

    }

    private fun shareKey(targetDevice: CHDevices, level: Int) {
        val friendID = userViewModel.userViewModel.value!!.sub


        val key = cheyKeyToUserKey(targetDevice.getKey(), level, targetDevice.getNickname())
        L.d("hcia", "CHUserKeyFriendID(key, friendID):" + CHUserKeyFriendID(key, friendID))
        CHLoginAPIManager.giveFriendDevice(CHUserKeyFriendID(key, friendID)) {
            it.onSuccess {
                L.d("hcia", "給朋友鑰匙:" + it.data)
                view?.post {
                    findNavController().navigateUp()
                }
            }
            it.onFailure {
                L.d("hcia", "給朋友鑰匙 失敗 it:$it")
            }
        }


    }

}