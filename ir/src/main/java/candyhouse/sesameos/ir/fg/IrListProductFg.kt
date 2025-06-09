package candyhouse.sesameos.ir.fg

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager

import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.BeanApp
import candyhouse.sesameos.ir.adapter.IrAppAdapter
import candyhouse.sesameos.ir.adapter.IrAppDivider
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.databinding.FgIrListproductBinding
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import candyhouse.sesameos.ir.ext.IRDeviceType
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame2.BuildConfig

import com.google.android.material.internal.ViewUtils

class IrListProductFg : IrBaseFG<FgIrListproductBinding>() {

    val TAG = "IrListProductFg"

    override fun getViewBinder() = FgIrListproductBinding.inflate(layoutInflater)

    private var learningIrRemote: IrRemote? = null

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.ryView.layoutManager =
            LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false) // 2列的网格布局

        val mData = mutableListOf(
            BeanApp(key = IRDeviceType.DEVICE_REMOTE_AIR),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_HW),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_AP),
            BeanApp(key = IRDeviceType.DEVICE_REMOTE_TV),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_IPTV),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_BOX),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_DVD),
//             BeanApp(key = IRDeviceType.DEVICE_REMOTE_FANS),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_PJT),
            BeanApp(key = IRDeviceType.DEVICE_REMOTE_LIGHT),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_DC),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_AUDIO),
//            BeanApp(key = IRDeviceType.DEVICE_REMOTE_ROBOT),
            BeanApp(key = IRDeviceType.DEVICE_REMOTE_DIY)
        )

        bind.ryView.adapter = IrAppAdapter(
            mData
        ) {
            L.d(TAG, "clickEvent it:" + it)
            if (it.key == IRDeviceType.DEVICE_REMOTE_DIY) {
                val irRemote = arguments?.getParcelableCompat<IrRemote>(Config.irDevice)
                safeNavigate(R.id.action_to_irdiy, Bundle().apply {
                    this.putBoolean(Config.isNewDevice, true)
                })
            } else {
                safeNavigate(R.id.action_to_ircomany, Bundle().apply {
                    this.putInt(Config.productKey, it.key)
                })
            }
        }

        val leftMargin = ViewUtils.dpToPx(this.requireContext(), 16)
        val rightMargin = ViewUtils.dpToPx(this.requireContext(), 16)

        val customDivider =
            IrAppDivider(this.requireContext(), leftMargin.toInt(), rightMargin.toInt())

        bind.ryView.addItemDecoration(customDivider)

        // setTitle("选择设备类型")
        //tvTitleOnclick("") {}

        setupBackViewListener()
    }

    private fun setupBackViewListener() {
        setFragmentResultListener(Config.learningIrDeviceResult) { _, bundle ->
            learningIrRemote = bundle.getParcelableCompat<IrRemote>(Config.irDevice)
            L.d(TAG, "haveLearningDevice:${learningIrRemote == null}")
        }
        setFragmentResultListener(Config.irCompanyAddResult) { _, bundle ->
            if (bundle.containsKey(Config.irMatchSuccess)) {
                val isMatchSuccess = bundle.getBoolean(Config.irMatchSuccess, false)
                if (isMatchSuccess) {
                    safeNavigateBack()
                }
            }
        }
    }

}
