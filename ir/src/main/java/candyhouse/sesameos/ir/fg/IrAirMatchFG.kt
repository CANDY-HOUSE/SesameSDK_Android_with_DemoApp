package candyhouse.sesameos.ir.fg

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.AirMatchViewAdapter
import candyhouse.sesameos.ir.base.BaseIr
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.databinding.FragmentAirMatchBinding
import candyhouse.sesameos.ir.domain.repository.RemoteRepository
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.viewModel.IrMatchViewModel
import candyhouse.sesameos.ir.viewModel.IrMatchViewModelFactory
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L

/**
 * 空调遥控器适配界面
 * add by wuying@cn.candyhouse.co
 */
class IrAirMatchFG : IrBaseFG<FragmentAirMatchBinding>() {
    private val tag = "IrAirMatchFG"
    lateinit var device: CHHub3
    private val viewModel: IrMatchViewModel by viewModels {
        IrMatchViewModelFactory(
            requireContext(),
            RemoteRepository(requireContext())
        )
    }

    override fun getViewBinder() = FragmentAirMatchBinding.inflate(layoutInflater)

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
            setupMatchView()
            initData()
        }
    }

    private fun setupParams() {
        val irCompanyCode =
            arguments?.getParcelableCompat<IrCompanyCode>(Config.irCompany)
        if (null == irCompanyCode) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT)
                .show()
            L.d(tag, "ir company code is null")
            safeNavigateBack()
            return
        }
        if (BaseIr.hub3Device.value == null) {
            Toast.makeText(requireContext(), R.string.ir_device_info_empty, Toast.LENGTH_SHORT)
                .show()
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
        device = BaseIr.hub3Device.value!!
        viewModel.setDevice(device)
        L.d(tag, "irCompanyCode=${irCompanyCode.toString()}")
        viewModel.setIrCompanyCode(irCompanyCode)
    }

    private fun setupMatchView() {
        bind.btnNoResponse.visibility = View.VISIBLE
        bind.tvProgress.visibility = View.VISIBLE
        setProgress( viewModel.getCurrentCodeIndex(),airMatchViewAdapter.currentPosition,viewModel.getTotalCodeCounts())
        bind.btnNoResponse.setOnClickListener({
            hideMatchingContainer()
            if (viewModel.getCurrentCodeIndex() == viewModel.getTotalCodeCounts() - 1) {
                Toast.makeText(requireContext(), R.string.air_match_end, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.matchNextCode()
            airMatchViewAdapter.currentPosition = 0
            airMatchViewAdapter.notifyDataSetChanged()
            matchNextKey()
            setProgress(airMatchViewAdapter.currentPosition, viewModel.getCurrentCodeIndex(), viewModel.getTotalCodeCounts())
        })
        bind.btnResponded.setOnClickListener({
            hideMatchingContainer()
            matchNextKey()
            if (airMatchViewAdapter.currentPosition < airMatchViewAdapter.itemCount - 1) {
                airMatchViewAdapter.currentPosition++
                airMatchViewAdapter.notifyDataSetChanged()
            }
            setProgress(airMatchViewAdapter.currentPosition, viewModel.getCurrentCodeIndex(), viewModel.getTotalCodeCounts())
            if (airMatchViewAdapter.currentPosition == airMatchViewAdapter.itemCount - 1) {
                bind.btnResponded.text = getString(R.string.air_match_finished)
            } else {
                bind.btnResponded.text = getString(R.string.air_match_responded)
            }
        })
    }

    private fun setProgress(currentKeyIndex: Int, currentGroupIndex: Int, totalGroup: Int) {
//        val formattedText = getString(R.string.ir_match_key, currentGroupIndex + 1, currentKeyIndex + 1, totalGroup)
//        bind.tvProgress.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            Html.fromHtml(formattedText, Html.FROM_HTML_MODE_COMPACT)
//        } else {
//            Html.fromHtml(formattedText)
//        }
        val text = getString(R.string.ir_match_key, currentGroupIndex + 1, currentKeyIndex + 1, totalGroup)
        val spannableString = SpannableString(text)

        // 为第一个数字设置颜色
        val firstNumber = (currentGroupIndex + 1).toString()
        val firstIndex = text.indexOf(firstNumber)
        if (firstIndex != -1) {
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#28aeb1")),
                firstIndex,
                firstIndex + firstNumber.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 为第二个数字设置颜色
        val secondNumber = (currentKeyIndex + 1).toString()
        val secondIndex = text.indexOf(secondNumber, firstIndex + firstNumber.length)
        if (secondIndex != -1) {
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#28aeb1")),
                secondIndex,
                secondIndex + secondNumber.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        bind.tvProgress.text = spannableString
    }


    private fun matchNextKey() {
        if (airMatchViewAdapter.currentPosition == airMatchViewAdapter.itemCount - 1) {
            showSaveDialog()
            return
        }
        val noResponseResource = if (viewModel.getCurrentCodeIndex() == viewModel.getTotalCodeCounts() - 1) {
            R.string.air_match_no_respond
        } else {
            R.string.air_match_no_respond_turn_next
        }
        bind.btnNoResponse.text = getString(noResponseResource)
        if (View.GONE == bind.btnNoResponse.visibility) {
            bind.btnNoResponse.visibility = View.VISIBLE
        }
        if (View.GONE == bind.tvProgress.visibility) {
            bind.tvProgress.visibility = View.VISIBLE
        }
    }

    private fun showSaveDialog() {
        showCustomDialog(
            getString(R.string.ir_set_controller_name),
            viewModel.irCompanyCodeLiveData.value!!.name,
            getString(R.string.ir_edit_limit)
        ) { value ->
            viewModel.addIRDeviceInfo(value)
        }
    }

    fun simulateClickAtPosition(recyclerView: RecyclerView, position: Int) {
        recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
    }


    private fun checkInfo(): Boolean {
        return if (::device.isInitialized && null != viewModel.irCompanyCodeLiveData.value) {
            true
        } else false
    }

    private fun initData() {

    }

    private fun setupTitleView() {
        setTitle(viewModel.irCompanyCodeLiveData.value!!.name)
    }

    private lateinit var airMatchViewAdapter: AirMatchViewAdapter

    private fun setupRecyclerView() {
        airMatchViewAdapter = AirMatchViewAdapter(
            requireContext(),
            onItemClick = { item, position ->
                viewModel.handleItemClick(item, position)
                showMatchingContainer()
            }
        )
        bind.recyclerView.apply {
            adapter = airMatchViewAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeUiState() {
        viewModel.irRemoteDeviceLiveData.observe(viewLifecycleOwner) { value ->
            if (value == null) {
                Toast.makeText(requireContext(), R.string.air_match_add_failed, Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(requireContext(), R.string.air_match_success, Toast.LENGTH_SHORT).show()
            setFragmentResult(
                Config.irCompanyMatchResult, bundleOf(
                    Config.irMatchSuccess to true,
                    Config.irDevice to value
                )
            )
            safeNavigateBack()
        }
        viewModel.irMatchItemListLiveData.observe(viewLifecycleOwner) {
            showContent(it)
        }

    }

    private fun showContent(list: List<IrControlItem>) {
        bind.progressBar.isVisible = false
        bind.recyclerView.isVisible = true
        bind.errorView.isVisible = false
        setupLayoutManager()
        airMatchViewAdapter.submitList(list)
    }


    private fun showError(error: Throwable) {
        bind.progressBar.isVisible = false
        bind.recyclerView.isVisible = false
        bind.errorView.isVisible = true
        bind.errorView.text = error.localizedMessage
    }

    private fun setupLayoutManager() {
        bind.recyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            3
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

    fun showMatchingContainer() {
        bind.matchingContainer.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(
            bind.matchingContainer, "translationY",
            bind.matchingContainer.height.toFloat(), 0f
        ).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun hideMatchingContainer() {
        ObjectAnimator.ofFloat(
            bind.matchingContainer, "translationY",
            0f, bind.matchingContainer.height.toFloat()
        ).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bind.matchingContainer.visibility = View.GONE
                }
            })
            start()
        }
    }

}