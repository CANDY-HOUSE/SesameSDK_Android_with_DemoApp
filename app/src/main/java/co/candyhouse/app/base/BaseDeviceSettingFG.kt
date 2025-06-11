package co.candyhouse.app.base

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.clearNFC
import co.candyhouse.app.tabs.devices.ssm2.getFirZip
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNFC
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.level2Tag
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.setIsWidget
import co.candyhouse.app.tabs.devices.ssm2.setNFC
import co.candyhouse.app.tabs.devices.ssm2.setNickname
import co.candyhouse.app.tabs.devices.ssm2.setting.DfuService
import co.candyhouse.server.CHDeviceIDFriendID
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.server.CHUser
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleStatusDelegate
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.convertStringToColor
import co.utils.recycle.GenericAdapter
import co.utils.safeNavigate
import com.amazonaws.mobile.client.AWSMobileClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.dfu.DfuServiceInitiator
import java.util.UUID

abstract class BaseDeviceSettingFG<T : ViewBinding> : BaseDeviceFG<T>(), NfcSetting,
    BleStatusUpdate, DeviceStatusChange {

    private var isToNotification = false
    private var isViewDestroyed = false
    var mKeyUser = ArrayList<Any>()

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    override fun onResume() {
        super.onResume()
        onChange()

        CHBleManager.statusDelegate = object : CHBleStatusDelegate {
            override fun didScanChange(ss: CHScanStatus) {
                onChange()
            }
        }

        mDeviceModel.ssmosLockDelegates[mDeviceModel.ssmLockLiveData.value!!] =
            object : CHDeviceStatusDelegate {
                @SuppressLint("SetTextI18n")
                override fun onBleDeviceStatusChanged(
                    device: CHDevices,
                    status: CHDeviceStatus,
                    shadowStatus: CHDeviceStatus?
                ) {
                    L.d("[say]", "[BaseDeviceSettingFG.kt][onBleDeviceStatusChanged]")
                    onChange()
                    onUIDeviceStatus(status)
                    checkVersionTag(status, device)
                }

                @SuppressLint("SetTextI18n")
                override fun onMechStatus(device: CHDevices) {
                    view?.findViewById<TextView>(R.id.battery)?.post {
                        view?.findViewById<TextView>(R.id.battery)?.text =
                            "${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                    }
                }
            }.bindLifecycle(viewLifecycleOwner)

        checkTvSysNotifyWidget(isOnResume = true)
    }

    @SuppressLint("StringFormatMatches")
    fun usePressText() {
        val model = mDeviceModel.ssmLockLiveData.value
        model?.apply {
            view?.findViewById<TextView>(R.id.drop_hint_txt)?.text =
                resources.getString(R.string.drop_hint_press, model.productModel.modelName())
        }
    }

    open fun checkVersionTag(status: CHDeviceStatus, device: CHDevices) {
        if (status.value == CHDeviceLoginStatus.Login) {
            device.getVersionTag {
                it.onSuccess { va ->
                    if (isAdded && !isDetached) {
                        versionSet(device, va.data)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mKeyUser.add("add")
    }

    @SuppressLint("SetTextI18n")
    private fun versionSet(targetDevice: CHDevices, str: String) {
        if (targetDevice.productModel != CHProductModel.Hub3) {
            view?.findViewById<View>(R.id.device_version_txt)?.post {
                val zipname: String? =
                    context?.resources?.getResourceEntryName(targetDevice.getFirZip())
                L.d(
                    "sf",
                    targetDevice.productModel.deviceModelName() + "------" + zipname
                )
                zipname?.apply {
                    val tailTagag = str.split("-").last()
                    val cheddd = zipname.contains(tailTagag)
                    view?.findViewById<TextView>(R.id.device_version_txt)?.text =
                        str + (if (cheddd) getString(R.string.latest) else "")
                    view?.findViewById<View>(R.id.alert_logo)?.visibility =
                        if (cheddd) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewDestroyed = false

        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ss2 ->
            getView()?.findViewById<TextView>(R.id.device_model)?.text =
                ss2.productModel.deviceModelName()
            getView()?.findViewById<TextView>(R.id.key_level_txt)?.text = level2Tag(ss2.getLevel())
            getView()?.findViewById<TextView>(R.id.name_txt)?.text = ss2.getNickname()
            getView()?.findViewById<TextView>(R.id.device_uuid_txt)?.text =
                ss2.deviceId.toString().uppercase()
            getView()?.findViewById<TextView>(R.id.histag_txt)?.text =
                ss2.getHistoryTag()?.let { String(it) }

            checkVersionTag(ss2.deviceStatus, ss2)
        }

        val targetDevice = mDeviceModel.ssmLockLiveData.value!!
        handleUI(targetDevice)
        setupListeners(targetDevice)
    }

    private fun setupListeners(targetDevice: CHDevices) {
        view?.findViewById<View>(R.id.change_name_zone)?.setOnClickListener {
            context?.inputTextAlert(
                "",
                getString(R.string.edit_name),
                SharedPreferencesUtils.preferences.getString(
                    targetDevice.deviceId.toString(),
                    targetDevice.getNickname()
                )
            ) {
                confirmButtonWithText("OK") { alert, name ->
                    targetDevice.setNickname(name)
                    CHLoginAPIManager.putKey(
                        cheyKeyToUserKey(
                            targetDevice.getKey(),
                            targetDevice.getLevel(),
                            targetDevice.getNickname()
                        )
                    ) {}
                    /* https://ap-northeast-1.console.aws.amazon.com/lambda/home?region=ap-northeast-1#/functions/user_device_put?tab=code
                    *  Line 91 - 105:  FunctionName: 'update_device_name_in_matter',
                    *  */
                    getView()?.findViewById<TextView>(R.id.name_txt)?.text = name
                    dismiss()
                }
                cancelButton(getString(R.string.cancel))
            }?.show()
        }
        view?.findViewById<View>(R.id.share_zone)?.setOnClickListener {
            AlertView(getString(R.string.share), "", AlertStyle.IOS).apply {
                val innerLevel = targetDevice.getLevel()
                if (innerLevel == 0) {
                    addAction(
                        AlertAction(
                            getString(R.string.owner) + getString(R.string.key),
                            AlertActionStyle.DEFAULT
                        ) {
                            mDeviceModel.targetShareLevel = 0
                            safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                        })
                }
                if (innerLevel == 0 || innerLevel == 1) {
                    addAction(
                        AlertAction(
                            getString(R.string.manager) + getString(R.string.key),
                            AlertActionStyle.DEFAULT
                        ) {
                            mDeviceModel.targetShareLevel = 1
                            safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                        })
                }
                addAction(
                    AlertAction(
                        getString(R.string.guest) + getString(R.string.key),
                        AlertActionStyle.DEFAULT
                    ) {
                        mDeviceModel.targetShareLevel = 2
                        safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.drop_zone)?.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(
                            R.string.trash_device_key,
                            targetDevice.productModel.modelName()
                        ), AlertActionStyle.NEGATIVE
                    ) {
                        mDeviceModel.dropDevice {
                            it.onSuccess {
                                if (isAdded && !isViewDestroyed) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        findNavController().popBackStack(R.id.deviceListPG, false)
                                    }
                                }
                            }
                            it.onFailure {
                                toastMSG(it.localizedMessage)
                                L.d("hcia", "無法刪除鑰匙倉庫:")
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.reset_zone)?.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(R.string.ssm_delete),
                        AlertActionStyle.NEGATIVE
                    ) {
                        mDeviceModel.resetDevice {
                            it.onSuccess {
                                if (isAdded) {
                                    findNavController().popBackStack(R.id.deviceListPG, false)
                                }
                            }
                            it.onFailure {
                                toastMSG(it.localizedMessage)
                                L.d("hcia", "無法刪除鑰匙倉庫:")
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.nfc_zone)?.setOnClickListener {
            if (targetDevice.getNFC()?.isEmpty() == false) {
                AlertView("", "", AlertStyle.IOS).apply {
                    addAction(
                        AlertAction(
                            getString(R.string.nfc_reset),
                            AlertActionStyle.NEGATIVE
                        ) {
                            targetDevice.clearNFC()
                            getView()?.findViewById<TextView>(R.id.nfc_id_txt)?.text =
                                getString(R.string.nfc_hint)
                        })
                    show(activity as AppCompatActivity)
                }
            }
        }
        view?.findViewById<View>(R.id.dfu_zone)?.setOnClickListener {
            //对齐iOS统一弹出升级对话框风格
            if (targetDevice.productModel != CHProductModel.Hub3
                && mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.UnLogin
            ) {
                toastMSG(getString(R.string.toastBleNotReadyForDFU))
                return@setOnClickListener
            }
            AlertView(getString(R.string.ssm_update), "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        "OK",
                        AlertActionStyle.NEGATIVE
                    ) {
                        L.d(
                            "sf",
                            "Hub3 update OTA: " + targetDevice.productModel.modelName()
                        )
                        if (targetDevice.productModel == CHProductModel.Hub3) {
                            L.d("sf", "hub3模块升级固件……")
                            mDeviceModel.ssmLockLiveData.value!!.updateFirmware { res ->
                                res.onSuccess {
                                    //(mDeviceModel.ssmLockLiveData.value!! as CHHub3).updateFirmware {}
                                }
                            }
                        } else {
                            L.d("sf", "非hub3模块升级固件……")
                            targetDevice.updateFirmware { res ->
                                L.d("hcia", "res:$res")
                                res.onSuccess {
                                    L.d("hcia", "updateFirmware:" + it.data.address)
                                    val starter = DfuServiceInitiator(it.data.address)
                                    starter.setZip(targetDevice.getFirZip())
                                    starter.setPacketsReceiptNotificationsEnabled(true)
                                    starter.setPrepareDataObjectDelay(400)
                                    starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(
                                        true
                                    )
                                    starter.setDisableNotification(false)
                                    starter.setForeground(false)
                                    starter.start(requireActivity(), DfuService::class.java)
                                }
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleUI(targetDevice: CHDevices) {
        val level = targetDevice.getLevel()
        if (level == 2) {
            view?.findViewById<View>(R.id.share_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.chenge_angle_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.auto_lock_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.noti_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.opsensor_zone)?.visibility = View.GONE
        }
        targetDevice.getNFC()
            ?.apply { view?.findViewById<TextView>(R.id.nfc_id_txt)?.text = this }
        view?.findViewById<View>(R.id.reset_zone)?.visibility =
            if (BuildConfig.DEBUG) View.VISIBLE else View.GONE

        view?.findViewById<Switch>(R.id.widget_switch)?.apply {
            isChecked = targetDevice.getIsWidget()
            updataTargetDevice(isChecked, mDeviceModel.ssmLockLiveData.value)
            checkTvSysNotifyWidget(isCheck = isChecked)
            setOnCheckedChangeListener { view, isChecked ->
                targetDevice.setIsWidget(isChecked)
                updataTargetDevice(isChecked, mDeviceModel.ssmLockLiveData.value)
                checkTvSysNotifyWidget(isCheck = isChecked)
            }
        }

        view?.findViewById<RelativeLayout>(R.id.widget_rl)?.apply {
            setOnClickListener(null)
            setOnClickListener {
                openSettingNotify()
            }
        }

        view?.findViewById<RecyclerView>(R.id.friend_recy)?.apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = object : GenericAdapter<Any>(mKeyUser) {
                override fun getLayoutId(position: Int, obj: Any): Int {
                    return when (obj) {
                        is String -> R.layout.device_member_add
                        is ItemHead<*> -> R.layout.device_member_guest
                        is ItemMember<*> -> R.layout.device_guest_member_cell
                        is CHUser -> R.layout.device_member_cell
                        else -> R.layout.device_member_cell
                    }
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    when (viewType) {
                        R.layout.device_member_guest -> return object :
                            RecyclerView.ViewHolder(view), Binder<ItemHead<CHGuestKeyCut>> {

                            @SuppressLint("NotifyDataSetChanged")
                            override fun bind(data: ItemHead<CHGuestKeyCut>, pos: Int) {
                                val title: TextView = view.findViewById(R.id.title)
                                val subtitle: TextView = view.findViewById(R.id.subtitle)
                                val cell_back: View = view.findViewById(R.id.cell_back)
                                title.text = data.data.keyName
                                subtitle.text = level2Tag(2)
                                val gtag = data.data.guestKeyId.replace(
                                    "00000000",
                                    ""
                                )
                                cell_back.setBackgroundColor(
                                    convertStringToColor(
                                        data.data.guestKeyId.replace("00000000", "")
                                    ).toColorInt()
                                )
                                val mRadius = 25f
                                val drawable = GradientDrawable()
                                drawable.shape = GradientDrawable.RECTANGLE
                                if (data.total == 0) {
                                    drawable.cornerRadii = floatArrayOf(
                                        mRadius,
                                        mRadius,
                                        mRadius,
                                        mRadius,
                                        mRadius,
                                        mRadius,
                                        mRadius,
                                        mRadius
                                    )
                                } else {
                                    drawable.cornerRadii = floatArrayOf(
                                        mRadius,
                                        mRadius,
                                        0f,
                                        0f,
                                        0f,
                                        0f,
                                        mRadius,
                                        mRadius
                                    )
                                }
                                drawable.setColor(convertStringToColor(gtag).toColorInt())
                                cell_back.background = drawable
                                view.setOnClickListener {
                                    AlertView(data.data.keyName, "", AlertStyle.IOS).apply {
                                        addAction(
                                            AlertAction(
                                                getString(R.string.modifyGuestKeyTag),
                                                AlertActionStyle.DEFAULT
                                            ) {
                                                context?.inputTextAlert(
                                                    "",
                                                    getString(R.string.modifyGuestKeyTag),
                                                    data.data.keyName
                                                ) {
                                                    confirmButtonWithText("OK") { alert, name ->
                                                        targetDevice.updateGuestKey(
                                                            data.data.guestKeyId,
                                                            name
                                                        ) {
                                                            it.onSuccess {
                                                                getView()?.findViewById<android.view.View>(
                                                                    co.candyhouse.app.R.id.friend_recy
                                                                )
                                                                    ?.post {
                                                                        data.data.keyName = name
                                                                        adapter?.notifyDataSetChanged()
                                                                        dismiss()
                                                                    }
                                                            }
                                                        }
                                                    }
                                                    cancelButton(getString(R.string.cancel))
                                                }?.show()
                                            })

                                        addAction(
                                            AlertAction(
                                                getString(R.string.share_key),
                                                AlertActionStyle.DEFAULT
                                            ) {
                                                mDeviceModel.targetShareLevel = 2
                                                mDeviceModel.guestKeyId = data.data.guestKeyId
                                                safeNavigate(R.id.action_SesameSetting_to_myKEYFG)
                                            })

                                        addAction(
                                            AlertAction(
                                                getString(R.string.revoke),
                                                AlertActionStyle.NEGATIVE
                                            ) {
                                                targetDevice.removeGuestKey(data.data.guestKeyId) {
                                                    it.onSuccess {
                                                        view.post {
                                                            mKeyUser.remove(data)
                                                            getView()?.findViewById<androidx.recyclerview.widget.RecyclerView>(
                                                                co.candyhouse.app.R.id.friend_recy
                                                            )?.adapter?.notifyDataSetChanged()
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

                        R.layout.device_guest_member_cell -> return object :
                            RecyclerView.ViewHolder(view), Binder<ItemMember<CHUser>> {
                            var title: TextView = view.findViewById(R.id.title)
                            val cell_back: View = view.findViewById(R.id.cell_back)

                            @SuppressLint("NotifyDataSetChanged")
                            override fun bind(data: ItemMember<CHUser>, pos: Int) {
                                val friend = data.data
                                title.text = friend.nickname ?: friend.email
                                friend.gtag?.let {
                                    val mRadius = 25f
                                    val drawable = GradientDrawable()
                                    drawable.shape = GradientDrawable.RECTANGLE
                                    if ((data.index + 1) == data.total) {
                                        drawable.cornerRadii = floatArrayOf(
                                            0f,
                                            0f,
                                            mRadius,
                                            mRadius,
                                            mRadius,
                                            mRadius,
                                            0f,
                                            0f
                                        )
                                    }
                                    drawable.setColor(convertStringToColor(it).toColorInt())
                                    cell_back.background = drawable
                                }

                                view.setOnClickListener {
                                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                        val checkKeylevel =
                                            (targetDevice.getLevel() <= friend.keyLevel!!)
                                        AWSMobileClient.getInstance().userAttributes["sub"]?.let {
                                            if (friend.sub != it && checkKeylevel) {
                                                AlertView(
                                                    friend.email,
                                                    friend.nickname ?: friend.email,
                                                    AlertStyle.IOS
                                                ).apply {
                                                    addAction(
                                                        AlertAction(
                                                            getString(R.string.revoke),
                                                            AlertActionStyle.NEGATIVE
                                                        ) {
                                                            getView()?.findViewById<SwipeRefreshLayout>(
                                                                R.id.swiperefresh
                                                            )?.isRefreshing = true
                                                            CHLoginAPIManager.removeFriendDevice(
                                                                CHDeviceIDFriendID(
                                                                    targetDevice.deviceId.toString(),
                                                                    friend.sub
                                                                )
                                                            ) {
                                                                it.onSuccess {
                                                                    getView()?.findViewById<View>(R.id.friend_recy)
                                                                        ?.post {
                                                                            mKeyUser.remove(
                                                                                data
                                                                            )
                                                                            getView()?.findViewById<RecyclerView>(
                                                                                R.id.friend_recy
                                                                            )?.adapter?.notifyDataSetChanged()
                                                                            getView()?.findViewById<SwipeRefreshLayout>(
                                                                                R.id.swiperefresh
                                                                            )?.isRefreshing = false
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

                        R.layout.device_member_cell -> return object :
                            RecyclerView.ViewHolder(view), Binder<CHUser> {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun bind(data: CHUser, pos: Int) {
                                val title: TextView = view.findViewById(R.id.title)
                                val subtitle: TextView = view.findViewById(R.id.subtitle)
                                title.text = data.nickname ?: data.email
                                subtitle.text = level2Tag(data.keyLevel)

                                view.setOnClickListener {
                                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                        val checkKeylevel =
                                            (targetDevice.getLevel() <= data.keyLevel!!)
                                        AWSMobileClient.getInstance().userAttributes["sub"]?.let {
                                            if (data.sub != it && checkKeylevel) {
                                                AlertView(
                                                    getString(R.string.revoke) + " " + data.email,
                                                    "",
                                                    AlertStyle.IOS
                                                ).apply {
                                                    addAction(
                                                        AlertAction(
                                                            getString(R.string.revoke),
                                                            AlertActionStyle.NEGATIVE
                                                        ) {
                                                            getView()?.post {
                                                                getView()?.findViewById<SwipeRefreshLayout>(
                                                                    R.id.swiperefresh
                                                                )?.isRefreshing = true
                                                                CHLoginAPIManager.removeFriendDevice(
                                                                    CHDeviceIDFriendID(
                                                                        targetDevice.deviceId.toString(),
                                                                        data.sub
                                                                    )
                                                                ) {
                                                                    it.onSuccess {
                                                                        getView()?.findViewById<View>(
                                                                            R.id.friend_recy
                                                                        )?.post {
                                                                            mKeyUser.remove(data)
                                                                            getView()?.findViewById<RecyclerView>(
                                                                                R.id.friend_recy
                                                                            )?.adapter?.notifyDataSetChanged()
                                                                            getView()?.findViewById<SwipeRefreshLayout>(
                                                                                R.id.swiperefresh
                                                                            )?.isRefreshing = false
                                                                        }
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

                        else -> return object : RecyclerView.ViewHolder(view), Binder<String> {
                            override fun bind(data: String, pos: Int) {
                                view.setOnClickListener { safeNavigate(R.id.action_SettingFG_to_addMemberFG) }
                            }
                        }
                    }
                }
            }
        }

        view?.findViewById<RecyclerView>(R.id.friend_recy)?.adapter?.notifyDataSetChanged()

        view?.findViewById<TextView>(R.id.drop_hint_txt)?.text =
            getString(R.string.drop_hint, targetDevice.productModel.modelName())

        updateFreshTop(targetDevice)
    }

    private fun updateFreshTop(targetDevice: CHDevices) {
        if (!AWSMobileClient.getInstance().isSignedIn) {
            view?.findViewById<RecyclerView>(R.id.friend_recy)?.visibility = View.GONE
        } else if (targetDevice.getLevel() == 0 || targetDevice.getLevel() == 1) {
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.setOnRefreshListener {
                refreshTop()
            }

            refreshTop()
        } else {
            view?.findViewById<RecyclerView>(R.id.friend_recy)?.visibility = View.GONE
        }
    }

    private fun updataTargetDevice(isChecked: Boolean, targetDevice: CHDevices?) {
        targetDevice?.apply {
            view?.findViewById<View>(R.id.no_hand_zone)?.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                targetDevice.setIsNOHand(false)
                if (!isAdded) return
                view?.findViewById<TextView>(R.id.auto_open_txt)?.text = getString(R.string.Off)
            }

            mDeviceModel.updateWidgets(this.deviceId.toString())
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshTop() {
        view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.post {
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing = true
        }

        mDeviceModel.ssmLockLiveData.value?.apply {
            this.getTimeSignature()?.apply {
                CHLoginAPIManager.getDeviceMember(
                    mDeviceModel.ssmLockLiveData.value!!.deviceId.toString(),
                    this
                ) {
                    it.onSuccess { mebData ->
                        mKeyUser.clear()
                        mKeyUser.add("add")

                        val mySub = AWSMobileClient.getInstance().userAttributes["sub"]
                        mKeyUser.addAll(sortedMembers(mySub, mebData.data))

                        view?.post { view?.findViewById<RecyclerView>(R.id.friend_recy)?.adapter?.notifyDataSetChanged() }

                        mDeviceModel.ssmLockLiveData.value!!.getGuestKeys {/// 拿取訪客鑰匙
                            it.onSuccess {
                                it.data.forEach { guestKey ->
                                    sortedGuests(guestKey, mebData.data)
                                }
                                view?.post { view?.findViewById<RecyclerView>(R.id.friend_recy)?.adapter?.notifyDataSetChanged() }
                            }
                        }
                    }
                    it.onFailure {
                        L.d("hcia", "it: 拿取成員失敗 <--")
                        view?.post {
                            view?.findViewById<RecyclerView>(R.id.friend_recy)?.visibility =
                                View.GONE
                        }
                    }
                    view?.post {
                        view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing =
                            false
                    }
                }
            }
        }
    }

    /**
     * 群主、管理员排序
     */
    private fun sortedMembers(topSubId: String?, members: Array<CHUser>): List<Any> {
        val mySelf = mutableListOf<CHUser>()
        val friends = mutableListOf<CHUser>()
        if (topSubId != null) {
            val subId = UUID.fromString(topSubId)
            mySelf.addAll(members.filter { UUID.fromString(it.sub) == subId })
            friends.addAll(members.filter { UUID.fromString(it.sub) != subId })
        } else {
            friends.addAll(members)
        }

        friends.sortBy { UUID.fromString(it.sub).toString() }

        return mySelf +
                friends.filter { it.keyLevel == 0 }
                    .sortedBy { it.sub } +
                friends.filter { it.keyLevel == 1 }
                    .sortedBy { it.sub }
    }

    /**
     * 访客排序
     */
    private fun sortedGuests(guestKey: CHGuestKeyCut, members: Array<CHUser>) {
        val keyHead = ItemHead(guestKey)
        mKeyUser.add(keyHead)
        val sss = members.filter {
            it.keyLevel == 2 && guestKey.guestKeyId.contains(it.gtag!!)
        }
        sss.forEach { user ->
            val guestMember = ItemMember(user)
            guestMember.index = keyHead.total++
            guestMember.total = sss.count()
            mKeyUser.add(guestMember)
        }
    }

    override fun onNfcId(id: String) {
        requireActivity().runOnUiThread {
            mDeviceModel.ssmLockLiveData.value?.apply {
                this.setNFC(id)

                view?.apply {
                    findViewById<TextView>(R.id.nfc_id_txt)?.apply {
                        this.text = id
                    }
                }

            }
        }
    }

    override fun onChange() {
        L.d(
            "harry",
            "[onChange][status: " + mDeviceModel.ssmLockLiveData.value?.deviceStatus.toString() + "] [CHBleManager:  " + CHBleManager.getConnectRSize()
                .toString() + "]"
        )
        if (!isAdded) return
        when {
            CHBleManager.mScanning == CHScanStatus.BleClose -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.noble)
            }

            mDeviceModel.ssmLockLiveData.value!!.deviceStatus == CHDeviceStatus.NoBleSignal -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                if (CHBleManager.getConnectRSize() >= 7) {
                    view?.findViewById<TextView>(R.id.err_title)?.text =
                        getString(R.string.BleTooManyConnections)
                } else {
                    view?.findViewById<TextView>(R.id.err_title)?.text =
                        getString(R.string.NoBleSignal)
                }
            }

            mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.UnLogin -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text =
                    mDeviceModel.ssmLockLiveData.value!!.deviceStatus.toString()
            }

            else -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.GONE
            }
        }
    }

    private fun isNotifyEnable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = getSystemService(
                requireContext(),
                NotificationManager::class.java
            ) as NotificationManager
            return notificationManager.areNotificationsEnabled()
        } else {
            return true
        }
    }

    private fun openSettingNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.apply {
                val intent = Intent()
                isToNotification = true
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
                startActivity(intent)
            }
        }
    }

    private fun checkTvSysNotifyWidget(isCheck: Boolean = false, isOnResume: Boolean = false) {
        view?.findViewById<TextView>(R.id.tvSysNotifyWidget)?.apply {
            text =
                if (isNotifyEnable()) getString(R.string.android_notifica_permis_on) else getString(
                    R.string.android_notifica_permis_off
                )
            if (!isOnResume) {
                isEnabled = isCheck
            } else {
                if (isToNotification) {
                    isToNotification = false
                    updataTargetDevice(true, mDeviceModel.ssmLockLiveData.value)
                }
            }
            isSelected = isEnabled && !isNotifyEnable()
        }
    }

    fun checkTvSysNotifyMsg(isCheck: Boolean = false, isOnResume: Boolean = false) {
        view?.findViewById<View>(R.id.noti_zone)?.apply {
            setOnClickListener(null)
            setOnClickListener {
                openSettingNotify()
            }
        }
        view?.findViewById<TextView>(R.id.tvSysNotifyMsg)?.apply {
            text =
                if (isNotifyEnable()) getString(R.string.android_notifica_permis_on) else getString(
                    R.string.android_notifica_permis_off
                )
            if (!isOnResume) {
                isEnabled = isCheck
            }
            isSelected = isEnabled && !isNotifyEnable()

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

interface NfcSetting {
    fun onNfcId(id: String)
}

interface BleStatusUpdate {
    fun onChange()
}

interface DeviceStatusChange {
    fun onUIDeviceStatus(status: CHDeviceStatus) {}
}