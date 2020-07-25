package co.candyhouse.app.tabs.devices.ssm2.room

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.sesame.ble.Sesame2.CHSesame2History
import org.zakariya.stickyheaders.SectioningAdapter
import java.text.SimpleDateFormat
import java.util.*


class SSMHistoryAdapter(var mGroupHistData: ArrayList<Pair<String, List<CHSesame2History>>>) : SectioningAdapter() {

    inner class ItemViewHolder(itemView: View) : SectioningAdapter.ItemViewHolder(itemView) {
        var name: TextView = itemView.findViewById<TextView>(R.id.name)
        var time: TextView = itemView.findViewById<TextView>(R.id.time)
        var locktype: TextView = itemView.findViewById<TextView>(R.id.locktype)
        var setting_params: TextView = itemView.findViewById<TextView>(R.id.params)
        var head: ImageView = itemView.findViewById<ImageView>(R.id.head)

    }

    inner class HeaderViewHolder(itemView: View) : SectioningAdapter.HeaderViewHolder(itemView) {
        var textView: TextView = itemView.findViewById<TextView>(R.id.title)
    }

    override fun getNumberOfSections(): Int {
        return mGroupHistData.size
    }

    override fun getNumberOfItemsInSection(sectionIndex: Int): Int {
        return mGroupHistData[sectionIndex].second.size
    }

    override fun doesSectionHaveHeader(sectionIndex: Int): Boolean {
        return getNumberOfSections() > 0
    }


    override fun onCreateHeaderViewHolder(parent: ViewGroup, headerType: Int): HeaderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.list_item_simple_header, parent, false)
        return HeaderViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindHeaderViewHolder(viewHolder: SectioningAdapter.HeaderViewHolder, sectionIndex: Int, headerType: Int) {
        val s = mGroupHistData[sectionIndex]
        val hvh = viewHolder as HeaderViewHolder
        hvh.textView.text = s.first
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, itemType: Int): SectioningAdapter.ItemViewHolder? {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.list_item_simple_item, parent, false)
        return ItemViewHolder(v)
    }

    override fun onBindItemViewHolder(viewHolder: SectioningAdapter.ItemViewHolder, sectionIndex: Int, itemIndex: Int, itemType: Int) {
        val dataPair = mGroupHistData[sectionIndex]
        val ivh = viewHolder as ItemViewHolder
        val history = dataPair.second[itemIndex]
//        Log.d("tag","recordID:"+history.recordID.toString())
//        Log.d("tag","date"+history.date.toString())
//        Log.d("tag","histag"+history.histag.toString())
        when(history){
            is CHSesame2History.TimeChanged -> {
                Log.d("tag","timeBefore"+history.timeBefore.toString())
                Log.d("tag","timeAfter"+history.timeAfter.toString())
            }
            is CHSesame2History.MechSettingUpdated -> {
                Log.d("tag","lockTargetBefore"+history.lockTargetBefore.toString())
                Log.d("tag","lockTargetAfter"+history.lockTargetAfter.toString())
                Log.d("tag","unlockTargetBefore"+history.unlockTargetBefore.toString())
                Log.d("tag","unlockTargetAfter"+history.unlockTargetAfter.toString())
            }
            is CHSesame2History.AutoLockUpdated  -> {
                Log.d("tag","enabledBefore:"+history.enabledBefore.toString())
                Log.d("tag","enabledAfter"+history.enabledAfter.toString())
            }
        }

        ivh.locktype.text = history.javaClass.simpleName
        ivh.time.text = getTZ().format(history.date)
        ivh.name.text = history.recordID.toString() + ":" + history.historyTag?.let { String(it) }

        ivh.setting_params.text = when (history) {
            is CHSesame2History.TimeChanged -> history.timeBefore.toString() + " -> " + history.timeAfter
            is CHSesame2History.AutoLockUpdated -> "" + history.enabledBefore + "-->" + history.enabledAfter
            is CHSesame2History.MechSettingUpdated -> "lock:" + history.lockTargetBefore + "->" + history.lockTargetAfter + " unlock:" + history.unlockTargetBefore + "->" + history.unlockTargetAfter
            else -> ""
        }

        ivh.head.setImageResource(
                when (history) {
                    is CHSesame2History.ManualElse -> R.drawable.ic_handmove
                    is CHSesame2History.ManualLocked -> R.drawable.ic_icon_locked
                    is CHSesame2History.ManualUnlocked -> R.drawable.ic_icon_unlocked
                    is CHSesame2History.AutoLockUpdated -> R.drawable.ic_icons_outlined_setting
                    is CHSesame2History.AutoLock -> R.drawable.ic_autolock
                    is CHSesame2History.BLELock -> R.drawable.ic_icon_locked
                    is CHSesame2History.BLEUnlock -> R.drawable.ic_icon_unlocked
                    is CHSesame2History.TimeChanged -> R.drawable.ic_icon_time_change
                    is CHSesame2History.MechSettingUpdated -> R.drawable.ic_icons_outlined_setting
                    else -> R.drawable.ic_autolock
                }
        )
    }
}


fun getTZ(): SimpleDateFormat {
    val sd = SimpleDateFormat("HH:mm:ss a", Locale.getDefault())
    return sd
}

fun showTZ(): SimpleDateFormat {

    val sd = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())

    return sd
}

fun groupTZ(): SimpleDateFormat {

    val sd = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    return sd
}



