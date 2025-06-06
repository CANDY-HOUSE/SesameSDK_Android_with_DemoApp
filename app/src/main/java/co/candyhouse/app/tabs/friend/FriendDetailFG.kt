package co.candyhouse.app.tabs.friend


import android.os.Bundle

import android.view.View

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgFriendDetailBinding
import co.candyhouse.app.tabs.devices.ssm2.level2Tag
import co.candyhouse.app.tabs.devices.model.CHUserViewModel
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.server.CHDeviceIDFriendID
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.server.CHUserKey
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class FriendDetailFG : BaseNFG<FgFriendDetailBinding>() {
    private val modelFriend: CHUserViewModel by activityViewModels()
    var mdevices = ArrayList<CHUserKey>()
    override fun getViewBinder() = FgFriendDetailBinding.inflate(layoutInflater)

    fun checkHasDelDevice(deviceUUID: String, level: Int): Boolean {
        return mDeviceViewModel.myChDevices.value.any {
            it.deviceId.toString().uppercase() == deviceUUID.uppercase() && it.getLevel() <= level
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind.swiperefresh.setOnRefreshListener {
            bind.swiperefresh.isRefreshing = false
        }
        modelFriend.userViewModel.observe(viewLifecycleOwner) { item ->
            bind.titleTxt.text = item.nickname
            bind.mailTxt.text = item.email
        }
        bind.ssmid.setOnClickListener {
            showPopupWindow(it, bind.mailTxt.text.toString())

        }

        bind.addMoreZone.setOnClickListener {
            findNavController().navigate(R.id.action_FriendDetailFG_to_friendSelectLockerListFG)
        }
        bind.resetZone.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(R.string.delete_friend),
                        AlertActionStyle.NEGATIVE
                    ) { action ->
                        CHLoginAPIManager.removeFriend(modelFriend.userViewModel.value!!.sub) {
                            it.onSuccess {
                                modelFriend.removeFriend(modelFriend.userViewModel.value!!)
                                view.post {
                                    findNavController().navigateUp()
                                }
                            }
                        }
                    })
            }.show(activity as AppCompatActivity)

        }
        bind.swiperefresh.isRefreshing = true
        CHLoginAPIManager.devicesWithFriend(modelFriend.userViewModel.value!!.sub) {


            it.onSuccess {

                L.d("dayin", it.data.size.toString() + "")
                mdevices.clear()
                mdevices.addAll(it.data)
                bind.recy.post {
                    bind.recy.adapter?.notifyDataSetChanged()
                    bind.swiperefresh.isRefreshing = false
                }
            }
        }

        bind.recy.apply {
            adapter = object : GenericAdapter<CHUserKey>(mdevices) {
                override fun getLayoutId(position: Int, obj: CHUserKey): Int =
                    R.layout.cell_friend_detail_device

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view), Binder<CHUserKey> {
                        override fun bind(device: CHUserKey, pos: Int) {
                            val title: TextView = view.findViewById(R.id.title)
                            val subtitle: TextView = view.findViewById(R.id.subtitle)
                            title.text = device.deviceName
                            subtitle.text = level2Tag(device.keyLevel)
                            view.setOnClickListener {
                                AlertView(device.deviceName.toString(), "", AlertStyle.IOS).apply {

                                    mDeviceViewModel.myChDevices.value.map {
                                        it.getKey().keyIndex
                                    }

                                    addAction(
                                        AlertAction(
                                            getString(R.string.revoke),
                                            AlertActionStyle.NEGATIVE
                                        ) { action ->
                                            L.d("hcia", "syncDeleteKey :")
                                            if (checkHasDelDevice(
                                                    device.deviceUUID,
                                                    device.keyLevel
                                                )
                                            ) {
                                                CHLoginAPIManager.removeFriendDevice(
                                                    CHDeviceIDFriendID(
                                                        device.deviceUUID,
                                                        modelFriend.userViewModel.value!!.sub
                                                    )
                                                ) {
                                                    it.onSuccess {
                                                        L.d("hcia", "移除朋友鑰匙:")
                                                        view.post {
                                                            mdevices.remove(device)
                                                            bind.recy.post { adapter?.notifyDataSetChanged() }
                                                        }
                                                    }
                                                    it.onFailure {
                                                        L.d("hcia", "移除朋友鑰匙 失敗 it:$it")
                                                    }
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
    }
}
