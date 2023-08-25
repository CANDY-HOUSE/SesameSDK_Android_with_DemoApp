
package co.candyhouse.app.tabs.devices.ssm2.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.utils.L
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.guestkey_list.*

class GuestKeyListFG : BaseDeviceFG(R.layout.guestkey_list) {

    var mDeviceList = ArrayList<CHGuestKeyCut>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh()
        swiperefresh.setOnRefreshListener {
            refresh()
        }
        recy_empty.setEmptyView(empty_view)
        recy_empty.adapter = object : GenericAdapter<CHGuestKeyCut>(mDeviceList) {

            override fun getLayoutId(position: Int, obj: CHGuestKeyCut): Int {
                return R.layout.guestkey_cell
            }


            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(view), Binder<CHGuestKeyCut> {
                var title: TextView = view.findViewById(R.id.title)
                override fun bind(data: CHGuestKeyCut, pos: Int) {
//                        title.text = data.guestKeyId
                    itemView.setOnClickListener {
                        swiperefresh.isRefreshing = true

                        mDeviceModel.ssmLockLiveData.value!!.removeGuestKey(data.guestKeyId) {
                            it.onSuccess {
                                recy_empty?.post {
                                    mDeviceList.remove(data)
                                    recy_empty?.adapter?.notifyDataSetChanged()
                                    swiperefresh.isRefreshing = false
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun refresh() {
        swiperefresh.isRefreshing = true

        mDeviceModel.ssmLockLiveData.value!!.getGuestKeys {
            it.onSuccess {
                recy_empty?.post {
                    L.d("hcia", "getGuestKeys -->it.data:" + it.data)
//                    it.data.forEach {
//                        L.d("hcia", "it:" + it)
//                    }
                    mDeviceList.clear()
                    mDeviceList.addAll(it.data)
                    recy_empty?.adapter?.notifyDataSetChanged()
                    swiperefresh.isRefreshing = false

                }
            }
        }
    }
}
