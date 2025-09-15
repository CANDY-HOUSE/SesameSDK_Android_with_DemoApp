
package co.candyhouse.app.tabs.devices.ssm2.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.GuestkeyListBinding
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.candyhouse.sesame.utils.L
import co.utils.recycle.GenericAdapter

class GuestKeyListFG : BaseDeviceFG<GuestkeyListBinding>() {

    var mDeviceList = ArrayList<CHGuestKeyCut>()
    override fun getViewBinder()= GuestkeyListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh()
      bind.  swiperefresh.setOnRefreshListener {
            refresh()
        }
        bind.   recyEmpty.setEmptyView(   bind.emptyView)
        bind.   recyEmpty.adapter = object : GenericAdapter<CHGuestKeyCut>(mDeviceList) {

            override fun getLayoutId(position: Int, obj: CHGuestKeyCut): Int {
                return R.layout.guestkey_cell
            }


            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(view), Binder<CHGuestKeyCut> {
                var title: TextView = view.findViewById(R.id.title)
                override fun bind(data: CHGuestKeyCut, pos: Int) {
//                        title.text = data.guestKeyId
                    itemView.setOnClickListener {
                        bind.     swiperefresh.isRefreshing = true

                        mDeviceModel.ssmLockLiveData.value!!.removeGuestKey(data.guestKeyId) {
                            it.onSuccess {
                                bind.        recyEmpty.post {
                                    mDeviceList.remove(data)
                                    bind.        recyEmpty.adapter?.notifyDataSetChanged()
                                    bind.            swiperefresh.isRefreshing = false
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun refresh() {
        bind.  swiperefresh.isRefreshing = true

        mDeviceModel.ssmLockLiveData.value!!.getGuestKeys {
            it.onSuccess {
                bind.       recyEmpty.post {
                    L.d("hcia", "getGuestKeys -->it.data:" + it.data)
            //                    it.data.forEach {
            //                        L.d("hcia", "it:" + it)
            //                    }
                    mDeviceList.clear()
                    mDeviceList.addAll(it.data)
                    bind.      recyEmpty.adapter?.notifyDataSetChanged()
                    bind.     swiperefresh.isRefreshing = false

                }
            }
        }
    }
}
