package co.candyhouse.app.tabs.devices.hub3.setting.ir.learn

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgRemoteLearnBinding
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.CHHub3IRCode
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.getParcelableCompat
import co.utils.safeNavigateBack
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class RemoteLearnFG : BaseNFG<FgRemoteLearnBinding>() {
    private val gTag = RemoteLearnFG::class.java.simpleName
    val viewModel: IRDeviceViewModel by viewModels { IRLearnViewModelFactory(requireContext().applicationContext) }
    override fun getViewBinder() = FgRemoteLearnBinding.inflate(layoutInflater)
    val listCodes = mutableListOf<CHHub3IRCode>()

    var isNewDevice = false
    var editable = true
    lateinit var adapter: RemoteLearnAdapter
    lateinit var chHub3Delegate: CHHub3Delegate
    var isRegisterMode = false

    private var shouldContinue = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mDeviceViewModel.ssmLockLiveData.value == null || mDeviceViewModel.ssmLockLiveData.value !is CHHub3) {
            safeNavigateBack()
        }
        arguments?.let {
            if (it.containsKey(RemoteBundleKeyConfig.isNewDevice)) {
                isNewDevice = it.getBoolean(RemoteBundleKeyConfig.isNewDevice, false)
            }
            if (it.containsKey(RemoteBundleKeyConfig.editable)) {
                editable = it.getBoolean(RemoteBundleKeyConfig.editable, true)
            }
        }
        val defaultName = getString(R.string.ir_study)
        val irRemote: IrRemote =
            arguments?.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
                ?: IrRemote(
                    model = "",
                    alias = defaultName,
                    uuid = UUID.randomUUID().toString().uppercase(),
                    state = "",
                    timestamp = 0L,
                    type = IRType.DEVICE_REMOTE_CUSTOM,
                    code = 0,
                    direction = ""
                )
        viewModel.setRemoteDevice(irRemote)
        // 来自firebase crash：BaseIr.hub3Device.value有空的情况，做安全处理.
        try {
            viewModel.setCHHub3(mDeviceViewModel.ssmLockLiveData.value as CHHub3)
        } catch (e: NullPointerException) {
            // 处理异常信息
            handleHub3DeviceValueException(e)
            // 回退
            parentFragmentManager.popBackStack()
            return
        }
    }

    private fun handleHub3DeviceValueException(e: NullPointerException) {
        L.Companion.d("sf", "BaseIr.hub3Device.value is null. Must return!")

        shouldContinue = false

        val previousLabel = findNavController().previousBackStackEntry?.destination?.label
        L.Companion.d("sf", "导航来自：${previousLabel ?: "IrListProductFg"}")

        FirebaseCrashlytics.getInstance().apply {
            log("自学习界面初始化：BaseIr.hub3Device.value is null.")
            log("来自：$previousLabel")
            recordException(e)
        }

        Toast.makeText(
            context,
            "Unexpected error. Back to home, pull to refresh",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!shouldContinue) return
        setupTitleView()
        setupIrCodeViews()
        L.Companion.d(gTag,"device status=" + viewModel.getCHHub3().deviceStatus.name + " / " + viewModel.getCHHub3().deviceStatus.value + "  isNewDevice" + (isNewDevice))
        showLoadingView()
        getIrCodes()
        getIrMode()
        observeUiState()
        innitDevice()
    }

    private fun getIrMode() {
        viewModel.getMode { mode ->
            handleActionOnMainThread {
                if (mode.toInt() == IROperation.MODE_CONTROL) {
                    bind.topTitle.imgRight.isSelected = false
                    isRegisterMode = false
                } else {
                    bind.topTitle.imgRight.isSelected = true
                    isRegisterMode = true
                }
            }
        }
    }

    private fun setupTitleView() {
        if (editable) {
            tvTitleOnclick(viewModel.irRemoteDeviceLiveData.value!!.alias) {
                showCustomDialog(
                    getString(R.string.ir_set_controller_name),
                    viewModel.irRemoteDeviceLiveData.value!!.alias,
                    getString(R.string.ir_edit_limit)
                ) { value ->
                    showOverLayLoadingView()
                    viewModel.modifyIrRemoteInfo(
                        viewModel.getCHHub3().deviceId.toString().uppercase(),
                        alias = value,
                        onSuccess = {
                            showContentView()
                        },
                        onError = {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.ir_change_fail),
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                }
            }
        }
        bind.topTitle.imgRight.visibility = if (editable) View.VISIBLE else View.GONE
        bind.topTitle.imgRight.setImageResource(R.drawable.selector_img_add_del)
        bind.topTitle.imgRight.setOnClickListener { mView ->
            switchModel(mView.isSelected)
        }
    }

    private fun setupIrCodeViews() {
        adapter = RemoteLearnAdapter(
            requireContext(),
            listCodes, editable, onClickItem = { item ->
                emitIrLearnCode(item)
                viewModel.updateLearnDataToServer()
            },
            onLongClickItem = { position, item ->
                if (editable) {
                    showLongClick(position, item)
                }
            })
        bind.ryView.adapter = adapter
        if (!editable) {
            bind.textViewEmpty.text = getString(R.string.ir_no_code)
        }
    }

    private fun emitIrLearnCode(item: CHHub3IRCode) {
        viewModel.emitIrLearnCode(
            viewModel.getCHHub3().deviceId.toString().uppercase(),
            item.irID,
            viewModel.irRemoteDeviceLiveData.value!!.uuid,
            onSuccess = {
                handleActionOnMainThread {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.ir_send_success), Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onError = { message ->
                handleActionOnMainThread {
                    Toast.makeText(
                        requireContext(),
                        message, Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    fun switchModel(isClose: Boolean) {
        if (isClose) {
            isRegisterMode = false
            viewModel.exitLearnMode()
        } else {
            isRegisterMode = true
            viewModel.enterLearnMode()
        }
    }

    private fun showContentView() {
        handleActionOnMainThread {
            bind.progressG.visibility = View.GONE
            if (bind.ryView.visibility != View.VISIBLE) {
                bind.ryView.visibility = View.VISIBLE
            }
            bind.linearLayoutEmpty.visibility = if (listCodes.isEmpty()) View.VISIBLE else View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.GONE
        }
    }

    private fun showLoadingView() {
        handleActionOnMainThread {
            bind.linearLayoutEmpty.visibility = View.GONE
            bind.ryView.visibility = View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.GONE
            bind.progressG.visibility = View.VISIBLE
        }
    }

    private fun showOverLayLoadingView() {
        handleActionOnMainThread {
            bind.linearLayoutEmpty.visibility = View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.GONE
            bind.progressG.visibility = View.VISIBLE
        }
    }

    private fun showErrorView(onclick: () -> Unit = {}) {
        L.Companion.d(gTag, "showErrorView")
        handleActionOnMainThread {
            bind.progressG.visibility = View.GONE
            bind.ryView.visibility = View.GONE
            bind.linearLayoutEmpty.visibility = View.GONE
            bind.linearLayoutNetworkRetry.visibility = View.VISIBLE
            bind.linearLayoutNetworkRetry.setOnClickListener {
                onclick()
            }
        }
    }

    private fun postIrCode() {
        viewModel.addIRDeviceInfo(
            viewModel.getCHHub3(),
            listCodes,
            onSuccess = {},
            onError = {})
    }

    private fun getIrCodes() {
        viewModel.getIrCodeListInfo(
            onSuccess = {
                showContentView()
            }, onError = {
                showErrorView({
                    getIrCodes()
                })
            })
    }


    private fun observeUiState() {

        viewModel.irRemoteDeviceLiveData.observe(viewLifecycleOwner) { irRomteDevice ->
            handleActionOnMainThread {
                setTitle(irRomteDevice.alias)
            }

        }
        viewModel.irCodeLiveData.observe(viewLifecycleOwner) {
            handleActionOnMainThread {
                if (viewModel.getCHHub3().deviceStatus.value != CHDeviceLoginStatus.Login) {


                }
                L.Companion.d(gTag, "listCodes: ${listCodes.toString()}")

                listCodes.clear()
                listCodes.addAll(it)
                adapter.notifyDataSetChanged()
            }
        }
        viewModel.irCodeChangedLiveData.observe(viewLifecycleOwner) { irCode ->
            handleActionOnMainThread {
                addIrCode(irCode)
                switchModel(true)
                showContentView()
            }
        }
    }


    private fun innitDevice() {
        if (isNewDevice) {
            showLoadingView()
            viewModel.addIRDeviceInfo(
                viewModel.getCHHub3(), mutableListOf(),
                onSuccess = {
                    showContentView()
                },
                onError = {
                    showErrorView()
                })
        }
    }

    private fun addIrCode(baseIrCode: CHHub3IRCode?, isRefresh: Boolean = true) {
        handleActionOnMainThread {
            if (baseIrCode != null) {
                val isFind = listCodes.any { it.irID == baseIrCode.irID }
                if (isFind) return@handleActionOnMainThread
                listCodes.add(baseIrCode)
                if (isRefresh) {
                    adapter.notifyDataSetChanged()
                }
                postIrCode()
            }
        }
    }

    private fun showLongClick(position: Int, item: CHHub3IRCode) {
        AlertView("", "", AlertStyle.IOS).apply {
            val actionTitle: String = getString(R.string.ir_modify_key_name)
            val dialogTips: String = getString(R.string.ir_edit_limit)
            addAction(AlertAction(actionTitle, AlertActionStyle.DEFAULT) { action ->
                showCustomDialog(actionTitle, item.name, dialogTips) { name ->
                    changeIrCode(position, item, name)
                }
            })
            addAction(
                AlertAction(
                    getString(R.string.ir_delete),
                    AlertActionStyle.NEGATIVE
                ) { action ->
                    showOverLayLoadingView()
                    viewModel.deleteIRCode(
                        item,
                        onSuccess = {
                            handleActionOnMainThread {
                                listCodes.remove(item)
                                adapter.notifyDataSetChanged()
                                showContentView()
                            }
                        }, onDeleteError = {
                            handleActionOnMainThread {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.ir_delete_fail),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                })
            show(activity as AppCompatActivity)
        }
    }

    private fun changeIrCode(position: Int, item: CHHub3IRCode, name: String) {
        // viewModel.getCHHub3().irCodeChange(item.irID, name) {}
        showOverLayLoadingView()
        viewModel.changeIRCode(item, name, onSuccess = {
            handleActionOnMainThread {
                listCodes[position].name = name
                adapter.notifyItemChanged(position)
                showContentView()
            }
        }, onChangeError = {
            handleActionOnMainThread {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.ir_change_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.runCatching {
            if (isRegisterMode) {
                viewModel.exitLearnMode()
            }
            viewModel.unsubscribeGetModeTopic()
            if (::chHub3Delegate.isInitialized) {
                getCHHub3().multicastDelegate.removeDelegate(chHub3Delegate)
            }
        }.onFailure { e ->
            L.Companion.e(gTag, "${e.message}")
        }
        setFragmentResult(
            RemoteBundleKeyConfig.learningIrDeviceResult, bundleOf(
                RemoteBundleKeyConfig.irDevice to viewModel.getIrRemoteDevice()
            )
        )
    }

    private fun handleActionOnMainThread(action: () -> Unit) {
        if (view == null) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            action()
        }
    }

    fun tvTitleOnclick(name: String, block: () -> Unit) {
        view?.findViewById<TextView>(R.id.tvTitle)?.apply {
            text = name
            setOnClickListener { block() }
        }
    }

    fun showCustomDialog(dialogTitle: String, editText: String = "", tips:String = "",callOK: (String) -> Unit = {}) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(co.candyhouse.app.R.layout.fg_remote_control_save_dialog, null)

        builder.setView(dialogView)

        val dialog: AlertDialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        val title = dialogView.findViewById<TextView>(co.candyhouse.app.R.id.dialog_title)
        val edtName = dialogView.findViewById<EditText>(co.candyhouse.app.R.id.edtName)
        val tipsTextView = dialogView.findViewById<TextView>(co.candyhouse.app.R.id.ir_edit_tips)
        dialogView.findViewById<TextView>(co.candyhouse.app.R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<TextView>(co.candyhouse.app.R.id.tvOk).setOnClickListener {
            dialog.dismiss()
            val name = edtName.text.toString()
            callOK(name)

        }
        edtName.setText(editText)
        title.text = dialogTitle
        if (tips.isNotEmpty()) {
            tipsTextView.text = tips
        }
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.75).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun setTitle(name: String) {
        view?.findViewById<TextView>(co.candyhouse.app.R.id.tvTitle)?.text = name
    }
}