package co.candyhouse.app.tabs.devices.ssm2.member

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgAddMemberBinding
import co.candyhouse.app.tabs.account.CHUserKey
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.devices.model.CHUserViewModel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.server.CHUser
import co.candyhouse.server.CHUserKeyFriendID
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import co.utils.safeNavigateBack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddMemberFG : BaseDeviceFG<FgAddMemberBinding>() {
    private val userViewModel: CHUserViewModel by activityViewModels()
    override fun getViewBinder()= FgAddMemberBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        L.d("hcia","[AddMemberFG][onViewCreated]")
        super.onViewCreated(view, savedInstanceState)

        if (userViewModel.myFriends.value.size == 0) {
            userViewModel.syncFriendsFromServer()
        }
      bind.  swiperefresh.setOnRefreshListener {
            userViewModel.syncFriendsFromServer()
        }
        bind.    recy.apply {
            setEmptyView( bind.emptyView)
            adapter = object : GenericAdapter<CHUser>(userViewModel.myFriends.value) {
                override fun getLayoutId(position: Int, obj: CHUser): Int {
                    return R.layout.friend_cell
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view), Binder<CHUser> {
                        var customName = itemView.findViewById<TextView>(R.id.title)

                        override fun bind(data: CHUser, pos: Int) {
                            customName.text = data.nickname ?: data.email
                            itemView.setOnClickListener {

                                AlertView(data.nickname ?: data.email, "", AlertStyle.IOS).apply {
                                    addAction(AlertAction(getString(R.string.owner), AlertActionStyle.DEFAULT) { action ->
                                        shareKey(data, 0)
                                    })
                                    addAction(AlertAction(getString(R.string.manager), AlertActionStyle.DEFAULT) { action ->
                                        shareKey(data, 1)
                                    })
                                    addAction(AlertAction(getString(R.string.guest), AlertActionStyle.DEFAULT) { action ->
                                        mDeviceModel.ssmLockLiveData.value!!.createGuestKey(SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())) {
                                            it.onSuccess {
                                                shareGuestKey(data, cheyKeyToUserKey(it.data, 2, mDeviceModel.ssmLockLiveData.value!!.getNickname()))

                                            }
                                        }
                                    })
                                    show(activity as AppCompatActivity)
                                }
                            }
                        }
                    }
                }
            }
        }
        userViewModel.needRefresh.observe(viewLifecycleOwner) { isR ->
            bind.    swiperefresh.isRefreshing = isR
            bind.    recy.adapter?.notifyDataSetChanged()
        }
    }

    private fun shareKey(data: CHUser, level: Int) {
        val targetDevice: CHDevices = mDeviceModel.ssmLockLiveData.value!!

        CHLoginAPIManager.giveFriendDevice(CHUserKeyFriendID(cheyKeyToUserKey(targetDevice.getKey(), level, targetDevice.getNickname()), data.sub)) {
            it.onSuccess {
                L.d("hcia", "給朋友鑰匙:" + it.data)
                view?.post {
                    if (isAdded&&!isDetached){
                        findNavController().navigateUp()
                    }

                }
            }
            it.onFailure {
                L.d("hcia", "給朋友鑰匙 失敗 it:$it")
            }
        }
        L.d("hcia", "shareKey level:$level")

    }

    private fun shareGuestKey(data: CHUser, userKey: CHUserKey) {

        CHLoginAPIManager.giveFriendDevice(CHUserKeyFriendID(userKey, data.sub)) {
            it.onSuccess {
                L.d("hcia", "給朋友鑰匙:" + it.data)
                view?.post {
                    if (isAdded){
                        safeNavigateBack()

                    }

                }
            }
            it.onFailure {
                L.d("hcia", "給朋友鑰匙 失敗 it:$it")
            }
        }
    }


}