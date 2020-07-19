package co.candyhouse.app.tabs.devices

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.tabs.MainActivity
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleManagerDelegate
import co.candyhouse.sesame.ble.CHSesame2Status
import co.candyhouse.sesame.ble.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.app.R
import co.utils.L
import co.utils.recycle.EmptyRecyclerView
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_rg_device.*
import java.util.*

class RegisterDevicesFG : Fragment() {

    private lateinit var recyclerView: EmptyRecyclerView
    var mDeviceList = ArrayList<CHSesame2>()

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).hideMenu()
        CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredSesames(sesames: List<CHSesame2>) {
                ThreadUtils.runOnUiThread {
//                    L.d("hcia", "sesames:" + sesames.first().rssi)

                    mDeviceList.clear()
                    mDeviceList.addAll(sesames.sortedByDescending { it.rssi })
                    (recyclerView.adapter as GenericAdapter<*>).notifyDataSetChanged()
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_rg_device, container, false)

        recyclerView = view.findViewById<EmptyRecyclerView>(R.id.leaderboard_list).apply {
            setHasFixedSize(true)
            adapter = object : GenericAdapter<Any>(mDeviceList) {
                override fun getLayoutId(position: Int, obj: Any): Int {
                    return R.layout.cell_device_unregist
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view),
                            Binder<CHSesame2> {
                        var customName: TextView = itemView.findViewById(R.id.title)
                        var uuidTxt: TextView = itemView.findViewById(R.id.title_txt)
                        var statusTxt: TextView = itemView.findViewById(R.id.subtitle_txt)

                        @SuppressLint("SetTextI18n")
                        override fun bind(data: CHSesame2, pos: Int) {
                            val sesame = data
                            customName.text = "" + (sesame.rssi + 130) + "%"
                            uuidTxt.text = sesame.deviceId.toString().toUpperCase()
                            statusTxt.text = getString(R.string.Sesame2) + data.deviceStatus
                            sesame.connnect() {}
                            itemView.setOnClickListener {
                                MainActivity.activity?.showProgress()
                                if (sesame.deviceStatus == CHSesame2Status.readytoRegister) {
                                    registerSSM(sesame)
                                } else {
                                    sesame.delegate = object : CHSesame2Delegate {
                                        override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
                                            if (status == CHSesame2Status.readytoRegister) {
                                                registerSSM(sesame)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setEmptyView(empty_view)
        backicon.setOnClickListener { findNavController().navigateUp() }
    }

    private fun RecyclerView.ViewHolder.registerSSM(sesame: CHSesame2) {
        sesame.registerSesame() { res ->

            res.onSuccess {

                L.d("hcia", "UI收到成功註冊")
                sesame.setHistoryTag("ドラえもん".toByteArray()) {}
                sesame.configureLockPosition(0, 256) {}
                DeviceListFG.instance?.refleshPage()
                itemView?.post {
                    findNavController().navigateUp()
                }
            }
            res.onFailure {
                itemView?.post {
                    Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
                }
            }
            MainActivity.activity?.hideProgress()
        }
    }

}
