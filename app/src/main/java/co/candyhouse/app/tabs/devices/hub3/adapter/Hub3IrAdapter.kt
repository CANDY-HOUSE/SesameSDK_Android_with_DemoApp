package co.candyhouse.app.tabs.devices.hub3.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity.Companion.activity
import co.candyhouse.app.tabs.devices.hub3.adapter.provider.Hub3IrAdapterProvider
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter

/**
 * 自定义添加红外线列表适配器
 *
 * @author frey on 2025/4/10
 */
class Hub3IrAdapter(
    private val context: Context,
    items: MutableList<IrRemote>,
    private val hub3IrAdapterProvider: Hub3IrAdapterProvider
) : GenericAdapter<IrRemote>(items) {
    override fun getLayoutId(position: Int, obj: IrRemote): Int =
        R.layout.hub3_key_cell

    override fun getViewHolder(
        view: View,
        viewType: Int
    ): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(view), Binder<IrRemote> {
            @SuppressLint("SetTextI18n")
            override fun bind(data: IrRemote, pos: Int) {
                val title = itemView.findViewById<TextView>(R.id.title)
                title.text = data.alias

                // 学习类型保存
                if (data.type == IRType.DEVICE_REMOTE_CUSTOM) {
                    hub3IrAdapterProvider.setIrRemote(data)
                }

                itemView.setOnClickListener {
                    AlertView(
                        title.text.toString(),
                        "",
                        AlertStyle.IOS
                    ).apply {
                        addAction(
                            AlertAction(
                                context.getString(R.string.hub3_details),
                                AlertActionStyle.POSITIVE
                            ) {
                                L.d(
                                    "sf",
                                    "详情……" + data.alias + " " + data.type + " " + data.uuid
                                )
                                when (data.type) {
                                    IRType.DEVICE_REMOTE_CUSTOM -> {
                                        // 学习
                                        hub3IrAdapterProvider.performStudy(data)
                                    }

                                    IRType.DEVICE_REMOTE_AIR, IRType.DEVICE_REMOTE_LIGHT, IRType.DEVICE_REMOTE_TV -> {
                                        // 空调
                                        hub3IrAdapterProvider.performAirControl(data)
                                    }

                                    else -> {
                                        L.d("sf", "暂不支持未知类型跳转...")
                                    }
                                }
                            })
                        addAction(
                            AlertAction(
                                context.getString(R.string.ssm_delete),
                                AlertActionStyle.NEGATIVE
                            ) {
                                L.d(
                                    "sf",
                                    "删除……" + data.alias + " " + data.type + " " + data.uuid
                                )
                                hub3IrAdapterProvider.deleteIRDevice(data)
                            })
                        show(activity as AppCompatActivity)
                    }
                }
            }
        }
}