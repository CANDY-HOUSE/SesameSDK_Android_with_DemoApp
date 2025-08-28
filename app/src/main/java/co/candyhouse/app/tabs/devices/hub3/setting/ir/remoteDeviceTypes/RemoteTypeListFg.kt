package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteDeviceTypes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.sesame.BaseFG
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate
import com.skydoves.balloon.dp2Px
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgRemoteTypeListBinding
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.utils.getParcelableCompat

class RemoteTypeListFg : BaseFG<FgRemoteTypeListBinding>() {

    private val tag = RemoteTypeListFg::class.java.simpleName

    override fun getViewBinder() = FgRemoteTypeListBinding.inflate(layoutInflater)

    private var learnRemote: IrRemote? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBackViewListener()
    }

    private fun setupRecyclerView() {
        val irTypeList = mutableListOf(
            IRTypeBean(key = IRType.DEVICE_REMOTE_AIR),
            IRTypeBean(key = IRType.DEVICE_REMOTE_TV),
            IRTypeBean(key = IRType.DEVICE_REMOTE_LIGHT),
            IRTypeBean(key = IRType.DEVICE_REMOTE_DIY)
        )
        bind.ryView.adapter = RemoteTypeListAdapter(irTypeList) {
            L.d(tag, "clickEvent it:$it")
            if (it.key == IRType.DEVICE_REMOTE_DIY) {
                safeNavigate(R.id.action_to_learn, Bundle().apply {
                    this.putBoolean(RemoteBundleKeyConfig.isNewDevice, true)
                })
            } else {
                safeNavigate(R.id.action_to_remoteList, Bundle().apply {
                    this.putInt(RemoteBundleKeyConfig.productKey, it.key)
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