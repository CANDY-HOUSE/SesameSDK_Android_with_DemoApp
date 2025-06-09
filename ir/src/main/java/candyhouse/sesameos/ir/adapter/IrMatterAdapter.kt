package candyhouse.sesameos.ir.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R

import com.google.android.material.bottomsheet.BottomSheetDialog

class IrMatterAdapter(private val mData: List<String>, private val chooseType: (Boolean) -> Unit) : RecyclerView.Adapter<IrMatterAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fg_ir_matter_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mData[position]
        holder.tvName.text = item
        holder.itemView.setOnClickListener {
          //  showBottom(holder.itemView)
        }

    }

    override fun getItemCount(): Int {

        return mData.size
    }


    fun showBottom(itemView: View) {
        val bottomSheetDialog = BottomSheetDialog(itemView.context)
        val view = LayoutInflater.from(itemView.context).inflate(R.layout.fg_ir_bm_add, null)
        view.findViewById<TextView>(R.id.tvAddApp).setOnClickListener {
            bottomSheetDialog.dismiss()
            chooseType(true)
        }
        view.findViewById<TextView>(R.id.tvAddScene).setOnClickListener {
            bottomSheetDialog.dismiss()
            chooseType(false)
        }
        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvName: TextView = itemView.findViewById(R.id.tvName)


    }


}
