package co.utils.recycle





import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R

data class BotItem(val name:String,val id:Int)
class Bot2ItemView(val list:MutableList<BotItem>,val callback:(bot2:BotItem)->Unit): RecyclerView.Adapter<Bot2ItemView.ViewHolder>() {

  /*  val list= arrayListOf(
        "剧本0",
        "剧本1",
        "剧本2",

    )*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adp_bot_item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvName.text = item.name
        holder.itemView.setOnClickListener {
             callback(item)
        }

    }

    override fun getItemCount(): Int {

        return list.size
    }




    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)


    }


}
