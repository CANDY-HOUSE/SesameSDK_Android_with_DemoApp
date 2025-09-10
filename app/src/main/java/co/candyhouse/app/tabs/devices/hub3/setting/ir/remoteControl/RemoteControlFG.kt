package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgRemoteControlBinding
import co.candyhouse.app.tabs.devices.hub3.setting.ir.BaseIRFG
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrMatchRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.LayoutSettings
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.repository.RemoteRepository
import co.candyhouse.sesame.utils.L
import co.utils.getParcelableArrayListCompat
import co.utils.getParcelableCompat
import co.utils.safeNavigate
import co.utils.safeNavigateBack
import kotlinx.coroutines.launch

/**
 * 红外遥控器控制界面
 * add by wuying@cn.candyhouse.co
 */
class RemoteControlFG : BaseIRFG<FgRemoteControlBinding>() {
    private val tag = RemoteControlFG::class.java.simpleName
    var isNewDevice = false
    var editable = true

    private val viewModel: RemoteControlViewModel by viewModels {
        RemoteControlViewModelFactory(
            requireContext(),
            RemoteRepository(requireContext())
        )
    }

    override fun getViewBinder() = FgRemoteControlBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initParams(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkInfo()) {
            observeUiState()
            setupTitleView()
            setupRecyclerView()
            setupHelpView()
            setupBackViewListener()
        }
    }

    private fun initParams(saveBundle: Bundle?) {
        arguments?.let { args ->
            val hub3DeviceId = if (args.containsKey(RemoteBundleKeyConfig.hub3DeviceId)) {
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

            isNewDevice = if (args.containsKey(RemoteBundleKeyConfig.isNewDevice)) {
                args.getBoolean(RemoteBundleKeyConfig.isNewDevice, false)
            } else {
                false
            }

            editable = if (args.containsKey(RemoteBundleKeyConfig.editable)) {
                args.getBoolean(RemoteBundleKeyConfig.editable, true)
            } else {
                true
            }

            val argDevice = if (args.containsKey(RemoteBundleKeyConfig.irDevice)) {
                args.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
            } else {
                null
            }
            if (null == argDevice) {
                Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
                L.d(tag, "remote device is null")
                safeNavigateBack()
                return
            }
            val irRemote = argDevice.clone()
            irRemote.haveSave = !isNewDevice
            viewModel.initConfig(irRemote.type)
            if (isNewDevice) {
                irRemote.alias = irRemote.alias + "\uD83D\uDD8B\uFE0F"
            }
            viewModel.setRemoteDevice(irRemote)
        }
    }

    private fun checkInfo(): Boolean {
        if (viewModel.getHub3DeviceId().isEmpty()) {
            safeNavigateBack()
        }
        return viewModel.irRemoteDeviceLiveData.value != null
    }

    private fun setupTitleView() {

        setTitle(viewModel.irRemoteDeviceLiveData.value!!.alias)
        tvTitleOnclick(viewModel.irRemoteDeviceLiveData.value!!.alias) {
            if (!editable) {
                return@tvTitleOnclick
            }
            showCustomDialog(
                getString(R.string.ir_set_controller_name),
                getTitleName(),
                getString(R.string.ir_edit_limit)
            ) { value ->
                if (viewModel.getIrRemoteDevice()!!.haveSave) {
                    viewModel.modifyRemoteIrDeviceInfo(value)
                } else {
                    viewModel.irRemoteDeviceLiveData.value!!.alias = value
                    setTitle(value)
                }
            }
        }
        if (isNewDevice) {
            bind.topTitle.imgRight.setImageResource(R.drawable.ic_save)
            bind.topTitle.imgRight.visibility = View.VISIBLE
            bind.topTitle.imgRight.setOnClickListener { showSaveDialog() }
        }
    }

    private fun showSaveDialog() {
        showCustomDialog(
            getString(R.string.ir_confirm_controller_name),
            viewModel.irRemoteDeviceLiveData.value!!.alias,
            getString(R.string.ir_edit_limit)
        ) { value ->
            viewModel.irRemoteDeviceLiveData.value!!.alias = value
            viewModel.addIRDeviceInfo(viewModel.irRemoteDeviceLiveData.value!!) {
                if (it) {
                    setTitle(value)
                    setRightTextView("")
                    bind.topTitle.imgRight.visibility = View.GONE
                    bind.linearLayoutHelp.visibility = View.GONE
                }
            }

            // TODO: 把这个功能移植到云端
            viewModel.addIrDeviceToMatter(viewModel.getIrRemoteDevice())
        }
    }

    private lateinit var airControlViewsAdapter: RemoteControlViewsAdapter

    private fun setupRecyclerView() {
        airControlViewsAdapter = RemoteControlViewsAdapter(
            onItemClick = { item ->
                viewModel.handleItemClick(item)
            }
        )

        bind.recyclerView.apply {
            adapter = airControlViewsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupHelpView() {
        if (!isNewDevice) {
            bind.linearLayoutHelp.visibility = View.GONE
            return
        }
        bind.textviewHelp.visibility = View.VISIBLE
        bind.linearLayoutHelp.setOnClickListener({
            safeNavigate(R.id.action_to_remoteMatchCodeFg, Bundle().apply {
                this.putString(RemoteBundleKeyConfig.hub3DeviceId, viewModel.getHub3DeviceId())
                this.putParcelable(RemoteBundleKeyConfig.irDevice, viewModel.getIrRemoteDevice())
                this.putParcelableArrayList(RemoteBundleKeyConfig.irSearchResult, ArrayList(viewModel.getSearchRemoteList()))
            })
        })
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AirControlUiState.Loading -> showLoading()
                        is AirControlUiState.Success -> showContent(state)
                        is AirControlUiState.Error -> showError(state.exception)
                    }
                }
            }
        }
        viewModel.irRemoteDeviceLiveData.observe(viewLifecycleOwner) { value ->
            setTitle(value.alias)
        }
    }

    private fun showLoading() {
        bind.progressBar.isVisible = true
        bind.recyclerView.isVisible = false
        bind.errorView.isVisible = false
    }

    private fun showContent(state: AirControlUiState.Success) {
        bind.progressBar.isVisible = false
        bind.recyclerView.isVisible = true
        bind.errorView.isVisible = false
        setupLayoutManager(state.layoutSettings)
        airControlViewsAdapter.submitList(state.items)
    }


    private fun showError(error: Throwable) {
        bind.progressBar.isVisible = false
        bind.recyclerView.isVisible = false
        bind.errorView.isVisible = true
        bind.errorView.text = error.localizedMessage
    }

    private fun setupLayoutManager(layoutSettings: LayoutSettings) {
        bind.recyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            layoutSettings.columns
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind.recyclerView.adapter = null
        val irRemote = viewModel.getIrRemoteDevice()
        irRemote?.let { remote ->
            setFragmentResult(
                RemoteBundleKeyConfig.controlIrDeviceResult, bundleOf(
                    RemoteBundleKeyConfig.irDevice to remote,
                    RemoteBundleKeyConfig.hub3DeviceId to viewModel.getHub3DeviceId()
                )
            )
        }
    }

    private fun setupBackViewListener() {
        setFragmentResultListener(RemoteBundleKeyConfig.irAutoSearchResult) { _, bundle ->
            if (bundle.containsKey(RemoteBundleKeyConfig.irMatchSuccess)) {
                viewModel.setSearchRemoteList(emptyList())
                val irMatchSuccess = bundle.getBoolean(RemoteBundleKeyConfig.irMatchSuccess, false)
                if (!irMatchSuccess) {
                    return@setFragmentResultListener
                }
                val searchRemoteList = bundle.getParcelableArrayListCompat<IrMatchRemote>(RemoteBundleKeyConfig.irSearchResult)
                if (!searchRemoteList.isNullOrEmpty()) {
                    viewModel.setSearchRemoteList(searchRemoteList)
                }
                val irRemote = bundle.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
                if (null == irRemote) {
                    return@setFragmentResultListener
                }
                viewModel.setRemoteDevice(irRemote)
            }

        }
    }

}