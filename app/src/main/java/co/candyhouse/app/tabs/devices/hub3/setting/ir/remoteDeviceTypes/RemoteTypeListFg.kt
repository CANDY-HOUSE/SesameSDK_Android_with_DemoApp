package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteDeviceTypes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgRemoteTypeListBinding
import co.candyhouse.app.tabs.devices.hub3.setting.ir.BaseIRFG
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.sesame.utils.L
import co.utils.getParcelableCompat
import co.utils.safeNavigate
import co.utils.safeNavigateBack
import com.skydoves.balloon.dp2Px

class RemoteTypeListFg : BaseIRFG<FgRemoteTypeListBinding>() {

    private val tag = RemoteTypeListFg::class.java.simpleName

    override fun getViewBinder() = FgRemoteTypeListBinding.inflate(layoutInflater)

    private var learnRemote: IrRemote? = null
    private var hub3DeviceId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initParams()
        setupRecyclerView()
        setupBackViewListener()
    }

    private fun initParams() {
        arguments?.let {args->
            hub3DeviceId = if (args.containsKey(RemoteBundleKeyConfig.hub3DeviceId)) {
                args.getString(RemoteBundleKeyConfig.hub3DeviceId, "")
            } else {
                ""
            }
            if (hub3DeviceId.isEmpty()) {
                L.d(tag, "hub3 device id not match")
                safeNavigateBack()
            }
        }
    }

    private fun setupRecyclerView() {
        val irTypeList = mutableListOf(
            IRTypeBean(key = IRType.DEVICE_REMOTE_AIR),
            IRTypeBean(key = IRType.DEVICE_REMOTE_TV),
            IRTypeBean(key = IRType.DEVICE_REMOTE_LIGHT),
            IRTypeBean(key = IRType.DEVICE_REMOTE_FANS),
            IRTypeBean(key = IRType.DEVICE_REMOTE_DIY)
        )
        bind.ryView.adapter = RemoteTypeListAdapter(irTypeList) {
            L.d(tag, "clickEvent it:$it")
            if (it.key == IRType.DEVICE_REMOTE_DIY) {
                safeNavigate(R.id.action_to_learn, Bundle().apply {
                    this.putBoolean(RemoteBundleKeyConfig.isNewDevice, true)
                    this.putString(RemoteBundleKeyConfig.hub3DeviceId, arguments?.getString(RemoteBundleKeyConfig.hub3DeviceId, ""))
                })
            } else {
                safeNavigate(R.id.action_to_remoteList, Bundle().apply {
                    this.putInt(RemoteBundleKeyConfig.productKey, it.key)
                    this.putString(RemoteBundleKeyConfig.hub3DeviceId, arguments?.getString(RemoteBundleKeyConfig.hub3DeviceId, ""))
                })
            }
        }

        val margin = this.requireContext().dp2Px(16)
        bind.ryView.addItemDecoration(RemoteTypeDivider(this.requireContext(), margin, margin))
        bind.ryView.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false) // 2列的网格布局

    }


    private fun setupBackViewListener() {
        setFragmentResultListener(RemoteBundleKeyConfig.learningIrDeviceResult) { _, bundle ->
            learnRemote = bundle.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
            L.d(tag, "haveLearningDevice:${learnRemote == null}")
        }
    }

}