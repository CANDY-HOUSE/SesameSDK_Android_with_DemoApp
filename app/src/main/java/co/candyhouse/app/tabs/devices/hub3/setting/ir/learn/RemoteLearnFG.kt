package co.candyhouse.app.tabs.devices.hub3.setting.ir.learn

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgRemoteLearnBinding
import co.candyhouse.app.tabs.devices.hub3.setting.ir.BaseIRFG
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IRCode
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.getParcelableCompat
import co.utils.safeNavigateBack
import java.util.UUID

class RemoteLearnFG : BaseIRFG<FgRemoteLearnBinding>() {
    private val tag = RemoteLearnFG::class.java.simpleName
    val viewModel: IRDeviceViewModel by viewModels { IRLearnViewModelFactory(requireContext().applicationContext) }
    override fun getViewBinder() = FgRemoteLearnBinding.inflate(layoutInflater)
    val listCodes = mutableListOf<IRCode>()
    var editable = true
    lateinit var adapter: RemoteLearnAdapter
    var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initParams(savedInstanceState)
    }

    private fun initParams(saveBundle: Bundle?) {
        arguments?.let {args ->
            var hub3DeviceId = if (args.containsKey(RemoteBundleKeyConfig.hub3DeviceId)) {
                args.getString(RemoteBundleKeyConfig.hub3DeviceId, "")
            } else {
                ""
            }
            if (hub3DeviceId.isNullOrEmpty()) {
                L.d(tag, "hub3 device id not match")
                safeNavigateBack()
                return
            }
            viewModel.setHub3DeviceId(hub3DeviceId)

            editable = if (args.containsKey(RemoteBundleKeyConfig.editable)) {
                args.getBoolean(RemoteBundleKeyConfig.editable, true)
            } else {
                true
            }

            var argDevice = if (args.containsKey(RemoteBundleKeyConfig.irDevice)) {
                args.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
            } else {
                null
            }
            if (null == argDevice) {
                val defaultName = getString(R.string.ir_study)
                argDevice = IrRemote( model = "", alias = defaultName, uuid = UUID.randomUUID().toString().uppercase(), state = "", timestamp = 0L,
                    type = IRType.DEVICE_REMOTE_CUSTOM, code = 0,direction = "", haveSave = false)
            }
            viewModel.setRemoteDevice(argDevice)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTitleView()
        setupIrCodeViews()
        showLoadingView()
        getIrCodes()
        getIrMode()
        observeUiState()
        initDevice()
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
                        viewModel.getHub3DeviceId(),
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

    private fun emitIrLearnCode(item: IRCode) {
        viewModel.emitIrLearnCode(
            viewModel.getHub3DeviceId(),
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
        L.d(tag, "showErrorView")
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
                L.d(tag, "listCodes: ${listCodes.toString()}")
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


    private fun initDevice() {
        if (viewModel.getIrRemoteDevice()?.haveSave == false) {
            showLoadingView()
            viewModel.addIRDeviceInfo(mutableListOf(),
                onSuccess = {
                    viewModel.getIrRemoteDevice()?.haveSave = true
                    showContentView()
                },
                onError = {
                    showErrorView()
                })
        }
    }

    private fun addIrCode(baseIrCode: IRCode?, isRefresh: Boolean = true) {
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

    private fun showLongClick(position: Int, item: IRCode) {
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

    private fun changeIrCode(position: Int, item: IRCode, name: String) {
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
        }.onFailure { e ->
            L.e(tag, "${e.message}")
        }
        val saveRemote:IrRemote? = if(viewModel.addDeviceSuccess) viewModel.getIrRemoteDevice() else null
        setFragmentResult(
            RemoteBundleKeyConfig.learningIrDeviceResult, bundleOf(
                RemoteBundleKeyConfig.irDevice to saveRemote
            )
        )
    }

}