package candyhouse.sesameos.ir.adapter



import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.EmIconType
import candyhouse.sesameos.ir.ext.Ext
import kotlinx.android.parcel.Parcelize


@Parcelize
data class BeanIcon(
        val msg:String?=null,
        val type: EmIconType =EmIconType.SVG,
        @DrawableRes val svg:Int?=0,
): Parcelable {

}
class IrIconAdapter(private val mData: List<BeanIcon>, private val click: (BeanIcon) -> Unit) : RecyclerView.Adapter<IrIconAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fg_ir_icon_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mData[position]
       Ext.drawImg(item,holder.tvName,holder.imgView)
        holder.itemView.setOnClickListener {
            click(item)
        }
     }

    override fun getItemCount(): Int {

        return mData.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val imgView: ImageView = itemView.findViewById(R.id.imgView)

    }


}
