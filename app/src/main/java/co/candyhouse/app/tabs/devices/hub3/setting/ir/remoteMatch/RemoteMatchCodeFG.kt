package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteMatch

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrMatchRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.databinding.FgRemoteMatchCodeBinding
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import co.utils.getParcelableArrayListCompat
import co.utils.getParcelableCompat
import co.utils.safeNavigateBack

/**
 * 遥控器自动适配界面
 * add by wuying@cn.candyhouse.co
 */
class RemoteMatchCodeFG : BaseNFG<FgRemoteMatchCodeBinding>() {
    private val tag = RemoteMatchCodeFG::class.java.simpleName
    private var selectedIrRemote: IrRemote? = null
    private val viewModel: IrAirMatchCodeViewModel by viewModels {
        IrMatchCodeViewModelFactory(requireContext())
    }

    override fun getViewBinder(): FgRemoteMatchCodeBinding {
        return FgRemoteMatchCodeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupParams()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkInfo()) {
            observeUiState()
            setupTitleView()
            setupRecyclerView()
        }
        startAutoMatch()
    }

    private fun setupParams() {
        if (mDeviceViewModel.ssmLockLiveData.value == null) {
            safeNavigateBack()
            return
        }
        viewModel.setHub3Device(mDeviceViewModel.ssmLockLiveData.value!! as CHHub3)
        val irRemote = arguments?.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
        if (null == irRemote) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
            L.Companion.d(tag, "hub3 device is null")
            safeNavigateBack()
            return
        }
        viewModel.setOriginRemote(irRemote)
        arguments?.let {
            if (it.containsKey(RemoteBundleKeyConfig.irSearchResult)) {
                val searchRemoteList = it.getParcelableArrayListCompat<IrMatchRemote>(RemoteBundleKeyConfig.irSearchResult)
                if(!searchRemoteList.isNullOrEmpty()) {
                    viewModel.setSearchRemoteList(searchRemoteList)
                }
            }
        }
    }

    private fun gotoIrControlView(irRemote: IrRemote) {
        selectedIrRemote = irRemote
        setFragmentResult(
            RemoteBundleKeyConfig.irAutoSearchResult, bundleOf(
                RemoteBundleKeyConfig.irMatchSuccess to true,
                RemoteBundleKeyConfig.irDevice to irRemote,
                RemoteBundleKeyConfig.irSearchResult to ArrayList(viewModel.irMatchRemoteListLiveData.value ?: emptyList())
            )
        )
        safeNavigateBack()
    }

    private fun checkInfo(): Boolean {
        if (mDeviceViewModel.ssmLockLiveData.value == null || mDeviceViewModel.ssmLockLiveData.value !is CHHub3) {
            safeNavigateBack()
            return false
        }
        return viewModel.isHub3DeviceInitialized()
    }

    private fun setupTitleView() {
        setTitle(viewModel.originRemoteLiveData.value!!.model!!)
    }

    private fun startAutoMatch() {
        showAutoMatchView(emptyList(), true)
        viewModel.startAutoMatch()
    }

    private fun stopAutoMatch() {
        viewModel.exitMatchMode()
    }

    private fun setupRecyclerView() {
        val irAdapter = RemoteMatchRemoteAdapter(requireActivity()) { irRemote, position ->
            gotoIrControlView(irRemote)
        }
        bind.rvMatchRemote.adapter = irAdapter
    }

    private fun observeUiState() {
        viewModel.irMatchRemoteListLiveData.observe(viewLifecycleOwner) {
            showAutoMatchView(it)
        }
        viewModel.connectStatusLiveData.observe(viewLifecycleOwner) {
            bind.rlError.visibility = if (it) View.GONE else View.VISIBLE
        }
        viewModel.matchingLiveData.observe(viewLifecycleOwner) {
            if (it) {
                bind.rvMatchRemote.adapter?.let { adapter ->
                    bind.tvSearching.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                }
            } else {
                bind.tvSearching.visibility = View.GONE
            }
        }
    }

    private fun showAutoMatchView(irRemoteList: List<IrMatchRemote>, isSearching: Boolean = false) {
        if (irRemoteList.isEmpty() && !isSearching) {
            Toast.makeText(requireContext(), R.string.air_auto_match_code_fail, Toast.LENGTH_SHORT).show()
        }
        bind.rvMatchRemote.adapter.let { adapter ->
            if (adapter is RemoteMatchRemoteAdapter) {
                adapter.updateData(irRemoteList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind.rvMatchRemote.adapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoMatch()
        if (selectedIrRemote == null && isRemoving) {
            setFragmentResult(
                RemoteBundleKeyConfig.irAutoSearchResult, bundleOf(
                    RemoteBundleKeyConfig.irMatchSuccess to true,
                    RemoteBundleKeyConfig.irSearchResult to ArrayList(viewModel.irMatchRemoteListLiveData.value ?: emptyList())
                )
            )
        }
    }

    fun setTitle(name: String) {
        view?.findViewById<TextView>(R.id.tvTitle)?.text = name
    }

}