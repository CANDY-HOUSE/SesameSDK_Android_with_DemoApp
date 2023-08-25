package co.candyhouse.app.base

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.CandyHouseApp
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.app.tabs.devices.ssm2.setting.DfuService

import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleStatusDelegate
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.utils.CHUser
import co.utils.L
import co.utils.SharedPreferencesUtils

import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.convertStringToColor
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_setting_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.dfu.DfuServiceInitiator


interface NfcSetting {
    fun onNfcId(id: String)
}

interface BleStatusUpdate {
    fun onChange()
}

interface DeviceStatusChange {
    fun onUIDeviceStatus(status: CHDeviceStatus) {}
}

open class BaseDeviceSettingFG(layout: Int) : BaseDeviceFG(layout), NfcSetting, BleStatusUpdate, DeviceStatusChange {

    var mKeyUser = ArrayList<Any>()
    override fun onResume() {
        super.onResume()
        onChange()

        CHBleManager.statusDelegate = object : CHBleStatusDelegate {
            override fun didScanChange(ss: CHScanStatus) {
                onChange()
            }
        }

        mDeviceModel.ssmosLockDelegates[mDeviceModel.ssmLockLiveData.value!!] = object : CHDeviceStatusDelegate {
            @SuppressLint("SetTextI18n")
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
            }

            override fun onMechStatus(device: CHDevices) {
                view?.findViewById<TextView>(R.id.battery)?.post {
                    view?.findViewById<TextView>(R.id.battery)?.text ="${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                }
            }
        }
    }

    fun checkVersionTag(status: CHDeviceStatus, device: CHDevices) {
        L.l("x",isAdded.toString())
        if (!isAdded)return
        if (status.value == CHDeviceLoginStatus.Login) {

            device.getVersionTag {
                it.onSuccess {
                    view?.findViewById<TextView>(R.id.device_version_txt)?.post {
                        val zipname = resources.getResourceEntryName(device.getFirZip())
                        val tail_tag = it.data.split("-").last()
                        val cheddd = (zipname.contains(tail_tag))
                        //                                L.d("hcia", "zipname:" + zipname)
                        //                                L.d("hcia", "tail_tag:" + tail_tag)
                        //                                L.d("hcia", "cheddd:" + cheddd)
                        device_version_txt?.text = "${it.data}${
                            if (cheddd) {
                                getString(R.string.latest)
                            } else ""
                        }"
                        getView()?.findViewById<View>(R.id.alert_logo)?.visibility = if (cheddd) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        getView()?.findViewById<View>(R.id.share_zone)?.apply {
            this.visibility=View.GONE
        }
        getView()?.findViewById<View>(R.id.rlRole)?.apply {
            this.visibility=View.GONE
        }
        getView()?.findViewById<View>(R.id.friend_recy)?.apply {
            this.visibility=View.GONE
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mKeyUser.add("add")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetDevice = mDeviceModel.ssmLockLiveData.value!!
        val level = targetDevice.getLevel()
        if (level == 2) {
            getView()?.findViewById<View>(R.id.share_zone)?.visibility = View.GONE
            getView()?.findViewById<View>(R.id.chenge_angle_zone)?.visibility = View.GONE
            getView()?.findViewById<View>(R.id.auto_lock_zone)?.visibility = View.GONE
            getView()?.findViewById<View>(R.id.noti_zone)?.visibility = View.GONE
        }

        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ss2 ->
            getView()?.findViewById<TextView>(R.id.device_model)?.text = ss2.productModel.deviceModelName()
            getView()?.findViewById<TextView>(R.id.key_level_txt)?.text = level2Tag(ss2.getLevel())
            getView()?.findViewById<TextView>(R.id.name_txt)?.text = ss2.getNickname()
            getView()?.findViewById<TextView>(R.id.device_uuid_txt)?.text = ss2.deviceId.toString().uppercase()

            getView()?.findViewById<TextView>(R.id.histag_txt)?.text = ss2.getHistoryTag()?.let { String(it) }
            ss2.getVersionTag { res ->
                res.onSuccess {
                    context?.resources?.let { ctx ->
                        device_version_txt?.post {
                            val zipname = ctx.getResourceEntryName(targetDevice.getFirZip())
                            val tail_tag = it.data.split("-").last()
                            val cheddd = zipname.contains(tail_tag)
                            device_version_txt?.text = it.data + (if (cheddd) getString(R.string.latest) else "")
                            getView()?.findViewById<View>(R.id.alert_logo)?.visibility = if (cheddd) View.GONE else View.VISIBLE
                        }
                    }

                }
            }

        }

        getView()?.findViewById<View>(R.id.reset_zone)?.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE


        getView()?.findViewById<View>(R.id.share_zone)?.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                val level = targetDevice.getLevel()
                if (level == 0) {
                    addAction(AlertAction(getString(R.string.owner), AlertActionStyle.DEFAULT) { action ->
                        mDeviceModel.targetShareLevel = 0
                        findNavController().navigate(R.id.action_SesameSetting_to_myKEYFG)
                    })
                }
                if (level == 0 || level == 1) {
                    addAction(AlertAction(getString(R.string.manager), AlertActionStyle.DEFAULT) { action ->
                        mDeviceModel.targetShareLevel = 1
                        findNavController().navigate(R.id.action_SesameSetting_to_myKEYFG)
                    })
                }
                addAction(AlertAction(getString(R.string.guest), AlertActionStyle.DEFAULT) { action ->
                    mDeviceModel.targetShareLevel = 2
                    findNavController().navigate(R.id.action_SesameSetting_to_myKEYFG)
                })
                show(activity as AppCompatActivity)
            }
        }
        getView()?.findViewById<View>(R.id.drop_zone)?.setOnClickListener {
            mDeviceModel.dropDevice {
                it.onSuccess {
                    CandyHouseApp.app.ts("Reset Success.")
                    findNavController().popBackStack(R.id.deviceListPG, false)
                }

            }
        }
        getView()?.findViewById<View>(R.id.change_name_zone)?.setOnClickListener {
            context?.inputTextAlert("", getString(R.string.change_sesame_name), SharedPreferencesUtils.preferences.getString(targetDevice.deviceId.toString(), targetDevice.getNickname())) {
                confirmButtonWithText("OK") { alert, name ->
                    targetDevice.setNickname(name)
                //    CHLoginAPIManager.putKey(CheyKeyToUserKey(targetDevice.getKey(), targetDevice.getLevel(), targetDevice.getNickname())) {}
                    getView()?.findViewById<TextView>(R.id.name_txt)?.text = name
                    dismiss()
                }
                cancelButton(getString(R.string.cancel))
            }?.show()
        }
        getView()?.findViewById<View>(R.id.reset_zone)?.setOnClickListener {
            mDeviceModel.resetDevice {
                it.onSuccess {
                    CandyHouseApp.app.ts("Reset Success.")
                    findNavController().popBackStack(R.id.deviceListPG, false)
                }

            }

        }
        targetDevice.getNFC()?.apply { getView()?.findViewById<TextView>(R.id.nfc_id_txt)?.text = this }
        getView()?.findViewById<View>(R.id.nfc_zone)?.setOnClickListener {
            if (targetDevice.getNFC()?.isEmpty() == false) {
                AlertView("", "", AlertStyle.IOS).apply {
                    addAction(AlertAction(getString(R.string.nfc_reset), AlertActionStyle.NEGATIVE) { action ->
                        targetDevice.clearNFC()
                        getView()?.findViewById<TextView>(R.id.nfc_id_txt)?.text = getString(R.string.nfc_hint)

                    })
                    show(activity as AppCompatActivity)
                }
            }
        }

        getView()?.findViewById<Switch>(R.id.widget_switch)?.apply {
            isChecked = targetDevice.getIsWidget()
            getView()?.findViewById<View>(R.id.no_hand_zone)?.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                targetDevice.setIsNOHand(false)
                getView()?.findViewById<TextView>(R.id.auto_open_txt)?.text = getString(R.string.Off)
            }
            setOnCheckedChangeListener { view, isChecked ->
                targetDevice.setIsWidget(isChecked)
                getView()?.findViewById<Switch>(R.id.widget_switch)?.isChecked = targetDevice.getIsWidget()
                getView()?.findViewById<View>(R.id.no_hand_zone)?.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (!isChecked) {
                    targetDevice.setIsNOHand(false)
                    getView()?.findViewById<TextView>(R.id.auto_open_txt)?.text = getString(R.string.Off)
                }
                mDeviceModel.updateWidgets()
            }
        }

        getView()?.findViewById<View>(R.id.dfu_zone)?.setOnClickListener {
            if(mDeviceModel.ssmLockLiveData.value!!.deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                toastMSG(getString(R.string.toastBleNotReadyForDFU))
                return@setOnClickListener
            }
            AlertView(resources.getResourceEntryName(targetDevice.getFirZip()), "", AlertStyle.IOS).apply {
                addAction(AlertAction(getString(R.string.ssm_update), AlertActionStyle.NEGATIVE) { action ->
                    targetDevice.updateFirmware { res ->
                        L.d("hcia", "res:" + res)
                        res.onSuccess {
                            L.d("hcia", "updateFirmware:" + it.data.address)

                            val starter = DfuServiceInitiator(it.data.address)
                            starter.setZip(targetDevice.getFirZip())
                            starter.setPacketsReceiptNotificationsEnabled(true)
                            starter.setPrepareDataObjectDelay(400)
                            starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                            starter.setDisableNotification(false)
                            starter.setForeground(false)
                            starter.start(requireActivity(), DfuService::class.java)
                        }
                    }
                })
                show(activity as AppCompatActivity)
            }
        }

        getView()?.findViewById<RecyclerView>(R.id.friend_recy)?.apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = object : GenericAdapter<Any>(mKeyUser) {
                override fun getLayoutId(position: Int, obj: Any): Int {
                    return when (obj) {
                        is String -> R.layout.device_member_add
                        is ItemHead<*> -> R.layout.device_member_guest
                        is ItemMember<*> -> R.layout.device_guest_member_cell

                        else -> R.layout.device_member_cell
                    }
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    when (viewType) {
                        R.layout.device_member_guest -> return object : RecyclerView.ViewHolder(view), Binder<ItemHead<CHGuestKeyCut>> {

                            override fun bind(friend: ItemHead<CHGuestKeyCut>, pos: Int) {
                                val title: TextView = view.findViewById(R.id.title)
                                val subtitle: TextView = view.findViewById(R.id.subtitle)
                                val cell_back: View = view.findViewById(R.id.cell_back)
                                title.text = friend.data.keyName
                                subtitle.text = level2Tag(2) //                                title.text = friend.total.toString()
                                val gtag = friend.data.guestKeyId.replace("00000000", "") //                                subtitle.setTextColor(Color.parseColor(convertStringToColor(friend.guestKeyId)))
                                cell_back.setBackgroundColor(Color.parseColor(convertStringToColor(friend.data.guestKeyId.replace("00000000", ""))))
                                val mRadius = 25f
                                val drawable = GradientDrawable()
                                drawable.shape = GradientDrawable.RECTANGLE
                                if (friend.total == 0) {
                                    drawable.cornerRadii = floatArrayOf(mRadius, mRadius, mRadius, mRadius, mRadius, mRadius, mRadius, mRadius)
                                } else {
                                    drawable.cornerRadii = floatArrayOf(mRadius, mRadius, 0f, 0f, 0f, 0f, mRadius, mRadius)
                                }
                                drawable.setColor(Color.parseColor(convertStringToColor(gtag)))
                                cell_back.background = drawable
                                view.setOnClickListener {
                                    AlertView(friend.data.keyName, "", AlertStyle.IOS).apply {
                                        addAction(AlertAction(getString(R.string.modifyGuestKeyTag), AlertActionStyle.DEFAULT) { action ->
                                            context?.inputTextAlert("", getString(R.string.modifyGuestKeyTag), friend.data.keyName) {
                                                confirmButtonWithText("OK") { alert, name ->
                                                    targetDevice.updateGuestKey(friend.data.guestKeyId, name) {
                                                        it.onSuccess {
                                                            getView()?.findViewById<View>(R.id.friend_recy)?.post {
                                                                friend.data.keyName = name
                                                                adapter?.notifyDataSetChanged()
                                                                dismiss()
                                                            }
                                                        }
                                                    }
                                                }
                                                cancelButton(getString(R.string.cancel))
                                            }?.show()
                                        })

                                        addAction(AlertAction(getString(R.string.share_key), AlertActionStyle.DEFAULT) { action ->
                                            mDeviceModel.targetShareLevel = 2
                                            mDeviceModel.guestKeyId = friend.data.guestKeyId
                                            findNavController().navigate(R.id.action_SesameSetting_to_myKEYFG)
                                        })

                                        addAction(AlertAction(getString(R.string.revoke), AlertActionStyle.NEGATIVE) { action ->
                                            targetDevice.removeGuestKey(friend.data.guestKeyId) {
                                                it.onSuccess {
                                                    view.post {
                                                        mKeyUser.remove(friend)
                                                        getView()?.findViewById<RecyclerView>(R.id.friend_recy)?.adapter?.notifyDataSetChanged()
                                                        refreshTop()
                                                    }
                                                }
                                            }
                                        })

                                        show(activity as AppCompatActivity)
                                    }
                                }
                            }
                        }
                        R.layout.device_guest_member_cell -> return object : RecyclerView.ViewHolder(view), Binder<ItemMember<CHUser>> {
                            var title: TextView = view.findViewById(R.id.title)
                            val cell_back: View = view.findViewById(R.id.cell_back)

                            override fun bind(guestItem: ItemMember<CHUser>, pos: Int) {
                                val friend = guestItem.data
                                title.text = friend.nickname ?: friend.email
                                friend.gtag?.let {
                                    val mRadius = 25f
                                    val drawable = GradientDrawable()
                                    drawable.shape = GradientDrawable.RECTANGLE
                                    if ((guestItem.index + 1) == guestItem.total) {
                                        drawable.cornerRadii = floatArrayOf(0f, 0f, mRadius, mRadius, mRadius, mRadius, 0f, 0f)
                                    } else {
                                        //                                        drawable.cornerRadii = floatArrayOf(mRadius, mRadius, 0f, 0f, 0f, 0f, mRadius, mRadius)
                                    }
                                    drawable.setColor(Color.parseColor(convertStringToColor(it)))
                                    cell_back.background = drawable //                                    cell_back.setBackgroundColor(Color.parseColor(convertStringToColor(it)))
                                }

                                view.setOnClickListener {

                                }
                            }
                        }
                        R.layout.device_member_cell -> return object : RecyclerView.ViewHolder(view), Binder<CHUser> {
                            override fun bind(friend: CHUser, pos: Int) {
                                val title: TextView = view.findViewById(R.id.title)
                                val subtitle: TextView = view.findViewById(R.id.subtitle)
                                title.text = friend.nickname ?: friend.email
                                subtitle.text = level2Tag(friend.keyLevel)

                                view.setOnClickListener {
                                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                                    }
                                }
                            }
                        }
                        else -> return object : RecyclerView.ViewHolder(view), Binder<String> {
                            override fun bind(friend: String, pos: Int) {
                                view.setOnClickListener { findNavController().navigate(R.id.action_SettingFG_to_addMemberFG) }

                            }
                        }
                    }
                }
            }
        }
        getView()?.findViewById<RecyclerView>(R.id.friend_recy)?.adapter?.notifyDataSetChanged()

        getView()?.findViewById<TextView>(R.id.drop_hint_txt)?.text = getString(R.string.drop_hint, targetDevice.productModel.modelName())

    }

    private fun refreshTop() {

        view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.post {
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing = true
        }

    }

    override fun onNfcId(id: String) {
        mDeviceModel.ssmLockLiveData.value!!.setNFC(id)
        view?.findViewById<TextView>(R.id.nfc_id_txt)?.text = id
    }

    override fun onChange() {
        when {
            CHBleManager.mScanning == CHScanStatus.BleClose -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.noble)
            }
            mDeviceModel.ssmLockLiveData.value!!.deviceStatus == CHDeviceStatus.NoBleSignal -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.NoBleSignal)
            }
            mDeviceModel.ssmLockLiveData.value!!.deviceStatus.value == CHDeviceLoginStatus.UnLogin -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = mDeviceModel.ssmLockLiveData.value!!.deviceStatus.toString()
            }
            else -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.GONE
            }
        }

    }

}


class ItemHead<T>(ss: T) {
    val data: T = ss
    var total: Int = 0
}

class ItemMember<T>(ss: T) {
    val data: T = ss
    var index: Int = 0
    var total: Int = 0
}