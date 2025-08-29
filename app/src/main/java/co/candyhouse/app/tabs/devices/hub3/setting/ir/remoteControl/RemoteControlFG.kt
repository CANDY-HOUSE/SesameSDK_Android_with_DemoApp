package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl

import android.Manifest
import android.os.Build
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
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrMatchRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgRemoteControlBinding
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.LayoutSettings
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.repository.RemoteRepository
import co.candyhouse.sesame.open.device.CHHub3

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
class RemoteControlFG : BaseNFG<FgRemoteControlBinding>() {
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
        initParams()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkInfo()) {
            observeUiState()
            setupTitleView()
            setupRecyclerView()
            setupHelpView()
            setupBackViewListener()
            setupMatterActionButton()
        }
        viewModel.markAsLoaded()
    }

    private fun initParams() {
        if (mDeviceViewModel.ssmLockLiveData.value == null || mDeviceViewModel.ssmLockLiveData.value !is CHHub3) {
            safeNavigateBack()
            return
        }
        val device = mDeviceViewModel.ssmLockLiveData.value!! as CHHub3
        viewModel.setDevice(device)
        arguments?.let {
            if (it.containsKey(RemoteBundleKeyConfig.isNewDevice)) {
                isNewDevice = it.getBoolean(RemoteBundleKeyConfig.isNewDevice, false)
            }
            if (it.containsKey(RemoteBundleKeyConfig.editable)) {
                editable = it.getBoolean(RemoteBundleKeyConfig.editable, true)
            }
        }

        val argDevice = arguments?.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
        if (null == argDevice) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
            L.d(tag, "remote device is null")
            safeNavigateBack()
            return
        }
        L.Companion.d(tag, "argDevice=${argDevice.toString()}")
        if (isNewDevice) {
            argDevice.haveSave = false
        } else {
            argDevice.haveSave = true
        }
        viewModel.initConfig(argDevice.type)
        viewModel.setRemoteDevice(argDevice.clone())

        if (mDeviceViewModel.ssmLockLiveData.value == null) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
            L.Companion.d(tag, "hub3 device is null")
            safeNavigateBack()
            return
        }
    }

    private fun checkInfo(): Boolean {
        if (mDeviceViewModel.ssmLockLiveData.value == null || mDeviceViewModel.ssmLockLiveData.value !is CHHub3) {
            safeNavigateBack()
        }
        return viewModel.isDeviceInitialized() && viewModel.irRemoteDeviceLiveData.value != null
    }

    private fun setupTitleView() {
        if (!viewModel.isFirstLoad.value) {
            return
        }
        if (isNewDevice) {
            viewModel.irRemoteDeviceLiveData.value!!.alias = viewModel.irRemoteDeviceLiveData.value!!.alias + "\uD83D\uDD8B\uFE0F"
        }
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
            viewModel.addIRDeviceInfo(viewModel.getDevice(), viewModel.irRemoteDeviceLiveData.value!!) {
                if (it) {
                    setTitle(value)
                    setRightTextView("")
                    bind.topTitle.imgRight.visibility = View.GONE
                    bind.linearLayoutHelp.visibility = View.GONE
                }
            }
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isFirstLoad.collect { isFirst ->

            }
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
                    RemoteBundleKeyConfig.chDeviceId to viewModel.getDevice().deviceId.toString().uppercase()
                )
            )
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 及以上
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            requestPermissions(permissions, 1001)
        } else {
            // Android 13 以下
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requestPermissions(permissions, 1001)
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

    private fun setupMatterActionButton() {
        val fab = bind.fab
        // TODO: 目前只有debug版本支持把红外设备添加到 Matter
        if (BuildConfig.DEBUG) {
            fab.visibility = View.VISIBLE
            fab.setOnClickListener { view ->
                L.Companion.d(tag, "FAB clicked, adding to Matter + ${viewModel.getIrRemoteDevice()}")
                Toast.makeText(requireContext(), "添加到Matter", Toast.LENGTH_SHORT).show()
                viewModel.addIrDeviceToMatter(viewModel.getIrRemoteDevice(), viewModel.getDevice())
            }
        } else {
            fab.visibility = View.GONE
        }
    }

    fun setRightTextView(name: String, block: () -> Unit = {}) {
        view?.findViewById<TextView>(R.id.tvRight)?.apply {
            if (name.isEmpty()) {
                visibility = View.GONE
                return
            }
            visibility = View.VISIBLE
            text = name
            setOnClickListener { block() }
        }
    }
    fun setTitle(name: String) {
        view?.findViewById<TextView>(R.id.tvTitle)?.text = name
    }

    fun tvTitleOnclick(name: String, block: () -> Unit) {
        view?.findViewById<TextView>(R.id.tvTitle)?.apply {
            text = name
            setOnClickListener { block() }
        }
    }

    fun getTitleName(): String {
        return view?.findViewById<TextView>(R.id.tvTitle)?.text?.toString() ?: ""
    }

    fun showCustomDialog(dialogTitle: String, editText: String = "", tips:String = "",callOK: (String) -> Unit = {}) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.fg_remote_control_save_dialog, null)

        builder.setView(dialogView)

        val dialog: AlertDialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val tipsTextView = dialogView.findViewById<TextView>(R.id.ir_edit_tips)
        dialogView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.tvOk).setOnClickListener {
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
}