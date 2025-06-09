package candyhouse.sesameos.ir.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.ext.IRDeviceType

data class BeanApp(
    val name: String = "",
    val icon: Int = 0,
    val key: Int = 0,
)

class IrAppAdapter(private val mData: List<BeanApp>, private val chooseType: (BeanApp) -> Unit) :
    RecyclerView.Adapter<IrAppAdapter.ViewHolder>() {

    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view =
            LayoutInflater.from(context).inflate(R.layout.fg_ir_app_item, parent, false)
        return ViewHolder(view)
    }

    private fun deviceKeyToName(key: Int): String {
        val deviceNames = context?.resources?.getStringArray(R.array.strs_device)

        val deviceMap = mapOf(
            IRDeviceType.DEVICE_REMOTE_AIR to 0,
            IRDeviceType.DEVICE_REMOTE_HW to 1,
            IRDeviceType.DEVICE_REMOTE_AP to 2,
            IRDeviceType.DEVICE_REMOTE_TV to 3,
            IRDeviceType.DEVICE_REMOTE_IPTV to 4,
            IRDeviceType.DEVICE_REMOTE_BOX to 5,
            IRDeviceType.DEVICE_REMOTE_DVD to 6,
            IRDeviceType.DEVICE_REMOTE_FANS to 7,
            IRDeviceType.DEVICE_REMOTE_PJT to 8,
            IRDeviceType.DEVICE_REMOTE_LIGHT to 9,
            IRDeviceType.DEVICE_REMOTE_DC to 10,
            IRDeviceType.DEVICE_REMOTE_AUDIO to 11,
            IRDeviceType.DEVICE_REMOTE_ROBOT to 12,
            IRDeviceType.DEVICE_REMOTE_DIY to 13
        )
        val index = deviceMap[key]
        return index?.let { deviceNames?.get(it) } ?: ""
    }

    private fun deviceKeyToIcon(key: Int): Int {
        var value = R.drawable.png_diy
        if (key == IRDeviceType.DEVICE_REMOTE_AIR) {
            value = R.drawable.png_air
        } else if (key == IRDeviceType.DEVICE_REMOTE_HW) {
            value = R.drawable.png_hw
        } else if (key == IRDeviceType.DEVICE_REMOTE_AP) {
            value = R.drawable.png_ap
        } else if (key == IRDeviceType.DEVICE_REMOTE_TV) {
            value = R.drawable.png_tv
        } else if (key == IRDeviceType.DEVICE_REMOTE_IPTV) {
            value = R.drawable.png_iptv
        } else if (key == IRDeviceType.DEVICE_REMOTE_BOX) {
            value = R.drawable.png_box
        } else if (key == IRDeviceType.DEVICE_REMOTE_DVD) {
            value = R.drawable.png_dvd
        } else if (key == IRDeviceType.DEVICE_REMOTE_FANS) {
            value = R.drawable.png_fan
        } else if (key == IRDeviceType.DEVICE_REMOTE_PJT) {
            value = R.drawable.png_pjt
        } else if (key == IRDeviceType.DEVICE_REMOTE_LIGHT) {
            value = R.drawable.png_light
        } else if (key == IRDeviceType.DEVICE_REMOTE_DC) {
            value = R.drawable.png_dc
        } else if (key == IRDeviceType.DEVICE_REMOTE_AUDIO) {
            value = R.drawable.png_audio
        } else if (key == IRDeviceType.DEVICE_REMOTE_ROBOT) {
            value = R.drawable.png_robot
        }
        return value
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = deviceKeyToName(mData[position].key)
        holder.imgIcon.setImageResource(deviceKeyToIcon(mData[position].key))

        holder.itemView.setOnClickListener {
            chooseType(mData[position])
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
    }

}
