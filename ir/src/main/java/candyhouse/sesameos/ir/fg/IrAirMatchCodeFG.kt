package candyhouse.sesameos.ir.fg

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.ui.unit.Velocity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.IrMatchRemoteAdapter
import candyhouse.sesameos.ir.base.BaseIr
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrMatchRemote
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.databinding.FragmentAirMatchCodeBinding
import candyhouse.sesameos.ir.domain.repository.RemoteRepository
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableArrayListCompat
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import candyhouse.sesameos.ir.viewModel.IrAirMatchCodeViewModel
import candyhouse.sesameos.ir.viewModel.IrMatchCodeViewModelFactory
import co.candyhouse.sesame.utils.L


/**
 * 空调类型遥控器适配界面
 * add by wuying@cn.candyhouse.co
 */
class IrAirMatchCodeFG : IrBaseFG<FragmentAirMatchCodeBinding>() {
    private val tag = IrAirMatchCodeFG::class.java.simpleName
    private var haveMatchResult = false
    private var selectedIrRemote: IrRemote? = null
    private val viewModel: IrAirMatchCodeViewModel by viewModels {
        IrMatchCodeViewModelFactory(
            requireContext(), RemoteRepository(requireContext())
        )
    }

    override fun getViewBinder(): FragmentAirMatchCodeBinding {
        return FragmentAirMatchCodeBinding.inflate(layoutInflater)
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
            if (!haveMatchResult) {
                startAutoMatch()
            }
            L.d(tag, "onViewCreated haveMatchResult=$haveMatchResult")
        }
    }

    private fun setupParams() {
        if (null == BaseIr.hub3Device.value) {
            L.d(tag, "hub3 device is null, set hub3 device")
            safeNavigateBack()
            return
        }
        viewModel.setHub3Device(BaseIr.hub3Device.value!!)
        val irCompanyCode = arguments?.getParcelableCompat<IrCompanyCode>(Config.irCompany)
        if (null == irCompanyCode) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
            L.d(tag, "ir company code is null")
            safeNavigateBack()
            return
        }
        if (BaseIr.hub3Device.value == null) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
            L.d(tag, "hub3 device is null")
            safeNavigateBack()
            return
        }
        arguments?.let {
            if (it.containsKey(Config.productKey)) {
                val productKey = it.getInt(Config.productKey)
                viewModel.setupMatchData(productKey)
            } else {
                L.d(tag, "productKey is null")
                safeNavigateBack()
            }
        }
        L.d(tag, "irCompanyCode=${irCompanyCode.toString()}")
        viewModel.setIrCompanyCode(irCompanyCode)
        haveMatchResult = false
        arguments?.let {
            if (it.containsKey(Config.irSearchResult)) {
                val searchRemoteList = it.getParcelableArrayListCompat<IrMatchRemote>(Config.irSearchResult)
                if(!searchRemoteList.isNullOrEmpty()) {
                    viewModel.setSearchRemoteList(searchRemoteList)
                    haveMatchResult = true
                }
            }
        }
    }

    private fun gotoIrControlView(irRemote: IrRemote) {
        val initCompanyCode = viewModel.irCompanyCodeLiveData.value
        if (null == initCompanyCode) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT).show()
            L.d(tag, "ir company code is null")
            safeNavigateBack()
            return
        }
        selectedIrRemote = irRemote
        setFragmentResult(
            Config.irAutoSearchResult, bundleOf(
                Config.irMatchSuccess to true,
                Config.irDevice to irRemote,
                Config.irSearchResult to ArrayList(viewModel.irMatchRemoteListLiveData.value ?: emptyList())
            )
        )
        safeNavigateBack()
    }

    private fun checkInfo(): Boolean {
        return viewModel.isHub3DeviceInitialized() && null != viewModel.irCompanyCodeLiveData.value
    }

    private fun setupTitleView() {
        setTitle(viewModel.irCompanyCodeLiveData.value!!.name)
        bind.topTitle.imgRight.setImageResource(R.drawable.selector_img_add_del)
        bind.topTitle.imgRight.visibility = View.VISIBLE
        bind.topTitle.imgRight.setOnClickListener {
            if (bind.topTitle.imgRight.isSelected) {
                stopAutoMatch()
            } else {
                startAutoMatch()
            }
        }
    }

    private fun startAutoMatch() {
        showAutoMatchView(emptyList(), true)
        viewModel.startSubscribeIR()
    }

    private fun stopAutoMatch() {
        bind.tvSearching.visibility = View.GONE
        viewModel.stopSubscribeIR()
    }

    private fun setupRecyclerView() {
        val irAdapter = IrMatchRemoteAdapter(requireActivity()) { irRemote, position ->
            gotoIrControlView(irRemote)
        }
        bind.rvMatchRemote.adapter = irAdapter
    }

    private fun observeUiState() {
        viewModel.irMatchRemoteListLiveData.observe(viewLifecycleOwner) {
            showAutoMatchView(it)
        }
    }

    private fun showAutoMatchView(irRemoteList: List<IrMatchRemote>, isSearching: Boolean = false) {
        if (irRemoteList.isEmpty() && !isSearching) {
            Toast.makeText(requireContext(), R.string.air_auto_match_code_fail, Toast.LENGTH_SHORT).show()
        }
        bind.tvSearching.visibility = if (isSearching) View.VISIBLE else View.GONE
        bind.topTitle.imgRight.isSelected = isSearching
        bind.rvMatchRemote.adapter.let { adapter ->
            if (adapter is IrMatchRemoteAdapter) {
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
        if (selectedIrRemote == null && isRemoving) {
            setFragmentResult(
                Config.irAutoSearchResult, bundleOf(
                    Config.irMatchSuccess to true,
                    Config.irSearchResult to ArrayList(viewModel.irMatchRemoteListLiveData.value ?: emptyList())
                )
            )
        }
    }


}