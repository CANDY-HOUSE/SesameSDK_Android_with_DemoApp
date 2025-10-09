package co.candyhouse.app.tabs.devices.hub3.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.adapter.provider.Ssm2DevicesAdapterProvider
import co.candyhouse.app.tabs.devices.model.LockDeviceStatus
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter

/**
 * 自定义添加芝麻设备适配器
 *
 * @author frey on 2025/4/10
 */
class Ssm2DevicesAdapter(
    private val context: Context,
    items: MutableList<LockDeviceStatus>,
    private val deviceNameProvider: Ssm2DevicesAdapterProvider
) : GenericAdapter<LockDeviceStatus>(items) {
    override fun getLayoutId(position: Int, obj: LockDeviceStatus): Int =
        R.layout.wm2_key_cell

    override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(view), Binder<LockDeviceStatus> {
            @SuppressLint("SetTextI18n")
            override fun bind(data: LockDeviceStatus, pos: Int) {
                val title = itemView.findViewById<TextView>(R.id.title)
                title.text = data.id
                deviceNameProvider.getDeviceNameByIdNew(data.id)?.apply {
                    L.d("deviceNameById", this)
                    title.text = this
                }

                itemView.setOnClickListener {
                    val activity = context as? AppCompatActivity
                    if (activity != null) {
                        AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                            addAction(
                                AlertAction(
                                    context.getString(R.string.ssm_delete),
                                    AlertActionStyle.NEGATIVE
                                ) {
                                    deviceNameProvider.removeSesame(data.id)
                                })
                            show(activity)
                        }
                    } else {
                        L.e("Ssm2DevicesAdapter", "Activity is null or not AppCompatActivity")
                    }
                }
            }
        }
}