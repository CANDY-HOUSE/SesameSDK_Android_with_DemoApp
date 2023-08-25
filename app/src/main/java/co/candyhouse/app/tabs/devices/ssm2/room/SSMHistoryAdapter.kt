package co.candyhouse.app.tabs.devices.ssm2.room

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.sesame.open.device.CHSesame2History
import org.zakariya.stickyheaders.SectioningAdapter
import java.text.SimpleDateFormat
import java.util.*


class SSMHistoryAdapter(var mGroupHistData: ArrayList<Pair<String, List<CHSesame2History>>>) : SectioningAdapter() {

    inner class ItemViewHolder(itemView: View) : SectioningAdapter.ItemViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.name)
        var time: TextView = itemView.findViewById(R.id.time)
//        var locktype: TextView = itemView.findViewById(R.id.locktype)
        var head: ImageView = itemView.findViewById(R.id.head)
        var right_img: ImageView = itemView.findViewById(R.id.right_img)
    }

    inner class HeaderViewHolder(itemView: View) : SectioningAdapter.HeaderViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.title)
    }

    override fun getNumberOfSections(): Int {
        return mGroupHistData.size
    }

    override fun getNumberOfItemsInSection(sectionIndex: Int): Int {
        return mGroupHistData[sectionIndex].second.size
    }

    override fun doesSectionHaveHeader(sectionIndex: Int): Boolean {
        return numberOfSections > 0
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup, headerType: Int): HeaderViewHolder {
        return HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple_header, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindHeaderViewHolder(viewHolder: SectioningAdapter.HeaderViewHolder, sectionIndex: Int, headerType: Int) {
        (viewHolder as HeaderViewHolder).textView.text = mGroupHistData[sectionIndex].first
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, itemType: Int): SectioningAdapter.ItemViewHolder? {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple_item, parent, false))
    }

    override fun onBindItemViewHolder(viewHolder: SectioningAdapter.ItemViewHolder, sectionIndex: Int, itemIndex: Int, itemType: Int) {

        val history = mGroupHistData[sectionIndex].second[itemIndex]

        (viewHolder as ItemViewHolder).apply {
//            locktype.text = history.javaClass.simpleName
            time.text = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(history.date)
            name.text = history.historyTag?.let { String(it) } ?: when (history) {
                is CHSesame2History.AutoLock -> name.context.getString(R.string.autolock)
                is CHSesame2History.ManualLocked -> name.context.getString(R.string.manua_lock)
                is CHSesame2History.ManualUnlocked -> name.context.getString(R.string.manua_unlock)
                is CHSesame2History.ManualElse -> name.context.getString(R.string.manua_unlock)
                else -> "???"
            }
            head.setImageResource(
                    when (history) {
                        is CHSesame2History.AutoLock -> R.drawable.ic_history_lock
                        is CHSesame2History.ManualLocked -> R.drawable.ic_history_lock
                        is CHSesame2History.BLELock -> R.drawable.ic_history_lock
                        is CHSesame2History.WM2Lock -> R.drawable.ic_history_lock
                        is CHSesame2History.WEBLock -> R.drawable.ic_history_lock
                        is CHSesame2History.ManualUnlocked -> R.drawable.ic_history_unlock
                        is CHSesame2History.ManualElse -> R.drawable.ic_history_unlock
                        is CHSesame2History.BLEUnlock -> R.drawable.ic_history_unlock
                        is CHSesame2History.WM2Unlock -> R.drawable.ic_history_unlock
                        is CHSesame2History.WEBUnlock -> R.drawable.ic_history_unlock
                        else -> R.drawable.ic_ap_alert
                    }
            )
            right_img.setImageResource(
                    when (history) {
                        is CHSesame2History.ManualLocked -> R.drawable.ic_hand
                        is CHSesame2History.ManualUnlocked -> R.drawable.ic_hand
                        is CHSesame2History.ManualElse -> R.drawable.ic_hand
                        is CHSesame2History.AutoLock -> R.drawable.ic_auto
                        is CHSesame2History.BLELock -> R.drawable.ic_bluetooth_grey
                        is CHSesame2History.WM2Lock -> R.drawable.ic_wifi_grey
                        is CHSesame2History.WM2Unlock -> R.drawable.ic_wifi_grey
                        is CHSesame2History.WEBLock -> R.drawable.ic_wifi_grey
                        is CHSesame2History.BLEUnlock -> R.drawable.ic_bluetooth_grey
                        is CHSesame2History.WEBUnlock -> R.drawable.ic_wifi_grey
                        else -> R.drawable.ic_ap_alert
                    }
            )
        }

    }
}



