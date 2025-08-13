package candyhouse.sesameos.ir.fg

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.AirControlViewsAdapter
import candyhouse.sesameos.ir.base.BaseIr
import candyhouse.sesameos.ir.base.IrMatchRemote
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.databinding.FragmentAirControlBinding
import candyhouse.sesameos.ir.domain.repository.RemoteRepository
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableArrayListCompat
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import candyhouse.sesameos.ir.ext.IRDeviceType
import candyhouse.sesameos.ir.models.LayoutSettings
import candyhouse.sesameos.ir.viewModel.AirControlUiState
import candyhouse.sesameos.ir.viewModel.AirControlViewModel
import candyhouse.sesameos.ir.viewModel.AirControlViewModelFactory
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.launch
import kotlin.collections.List

/**
 * 红外遥控器控制界面
 * add by wuying@cn.candyhouse.co
 */
class IrControlFG : IrBaseFG<FragmentAirControlBinding>() {
    private val tag = "IrControlFG"
    var isNewDevice = false
    var editable = true
    private val viewModel: AirControlViewModel by viewModels {
        AirControlViewModelFactory(
            requireContext(),
            RemoteRepository(requireContext())
        )
    }

    override fun getViewBinder() = FragmentAirControlBinding.inflate(layoutInflater)

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
        }
        viewModel.markAsLoaded()
    }

    private fun initParams() {
        arguments?.let {
            if (it.containsKey(Config.isNewDevice)) {
                isNewDevice = it.getBoolean(Config.isNewDevice, false)
            }
            if (it.containsKey(Config.editable)) {
                editable = it.getBoolean(Config.editable, true)
            }
        }

        val argDevice =
            arguments?.getParcelableCompat<IrRemote>(Config.irDevice)
        if (null == argDevice) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT)
                .show()
            L.d(tag,"remote device is null")
            safeNavigateBack()
            return
        }
        L.d(tag,"argDevice=${argDevice.toString()}")
        if (isNewDevice) {
            argDevice.haveSave = false
        } else {
            argDevice.haveSave = true
        }
        viewModel.initConfig(argDevice.type)
        viewModel.setRemoteDevice(argDevice.clone())

        if (BaseIr.hub3Device.value == null) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT)
                .show()
            L.d(tag,"hub3 device is null")
            safeNavigateBack()
            return
        }
        val device = BaseIr.hub3Device.value!!
        viewModel.setDevice(device)
    }

    private fun checkInfo(): Boolean {
        return viewModel.isDeviceInitialized() && viewModel.irRemoteDeviceLiveData.value != null
    }

    private fun setupTitleView() {
        if (!viewModel.isFirstLoad.value) {
            return
        }
        if (isNewDevice) {
            viewModel.irRemoteDeviceLiveData.value!!.alias = viewModel.irRemoteDeviceLiveData.value!!.alias +"\uD83D\uDD8B\uFE0F"
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
            bind.topTitle.imgRight.setImageResource(R.drawable.svg_save)
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

    private lateinit var airControlViewsAdapter: AirControlViewsAdapter

    private fun setupRecyclerView() {
        airControlViewsAdapter = AirControlViewsAdapter(
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
            val companyCode = viewModel.getCompanyCode()
            L.d(tag, "companyCode=${companyCode.toString()}")
            companyCode?.let {
                L.d(tag, "go to match company code")
                    safeNavigate(R.id.action_to_irAirMatchCodeFg, Bundle().apply {
                    this.putInt(Config.productKey, viewModel.getIrRemoteDevice()!!.type)
                    this.putParcelable(Config.irCompany, it)
                    this.putParcelableArrayList(Config.irSearchResult, ArrayList(viewModel.getSearchRemoteList()))
                })
            }
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
        viewModel.irRemoteDeviceLiveData.observe(viewLifecycleOwner) { value->
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
                Config.controlIrDeviceResult, bundleOf(
                    Config.irDevice to remote,
                    Config.chDeviceId to viewModel.getDevice().deviceId.toString().uppercase()
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
        setFragmentResultListener(Config.irCompanyMatchResult) { _, bundle ->
            if (bundle.containsKey(Config.irMatchSuccess)) {
                val isMatchSuccess = bundle.getBoolean(Config.irMatchSuccess, false)
                val irRemote = bundle.getParcelableCompat<IrRemote>(Config.irDevice)
                if (isMatchSuccess && null != irRemote) {
                    if (viewModel.getIrRemoteDevice()!!.haveSave) {
                        viewModel.deleteIrDeviceInfo(viewModel.getDevice(), viewModel.irRemoteDeviceLiveData.value!!)
                    }
                    setRightTextView("")
                    bind.topTitle.imgRight.visibility = View.GONE
                    irRemote.haveSave = true
                    viewModel.setRemoteDevice(irRemote)
                }
                bind.linearLayoutHelp.visibility = View.GONE
            }
        }
        setFragmentResultListener(Config.irAutoSearchResult) {_, bundle ->
            if (bundle.containsKey(Config.irMatchSuccess)) {
                viewModel.setSearchRemoteList(emptyList())
                val irMatchSuccess = bundle.getBoolean(Config.irMatchSuccess, false)
                if (!irMatchSuccess){
                    return@setFragmentResultListener
                }
                val searchRemoteList = bundle.getParcelableArrayListCompat<IrMatchRemote>(Config.irSearchResult)
                if (!searchRemoteList.isNullOrEmpty()) {
                    viewModel.setSearchRemoteList(searchRemoteList)
                }
                val irRemote = bundle.getParcelableCompat<IrRemote>(Config.irDevice)
                if (null == irRemote) {
                    return@setFragmentResultListener
                }
                viewModel.setRemoteDevice(irRemote)
            }

        }
    }

}