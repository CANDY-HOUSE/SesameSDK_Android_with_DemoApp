package co.candyhouse.app.tabs.devices.ssm2.menber

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.app.R
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.AlertView
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_delete_member.*
import java.util.*

class DeleteMemberFG : BaseSSMFG() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_delete_member, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        recy.apply {
//            setHasFixedSize(true)
//            adapter = object : GenericAdapter<CHMemberAndOperater>(memberList) {
//                override fun getLayoutId(position: Int, obj: CHMemberAndOperater): Int {
//                    return R.layout.cell_friend
//                }
//
//                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
//                    return object : RecyclerView.ViewHolder(view),
//                            Binder<CHMemberAndOperater> {
//                        var avatar: ImageView = itemView.findViewById(R.id.avatar)
//                        var customName = itemView.findViewById<TextView>(R.id.title)
//
//                        @SuppressLint("SetTextI18n")
//                        override fun bind(data: CHMemberAndOperater, pos: Int) {
//                            customName.text = data.opetator?.name
//                            avatar.setImageDrawable(avatatImagGenaroter(data.opetator?.firstname))
////                            itemView.setOnClickListener {
////                                val alert = AlertView(data.opetator!!.name, "", AlertStyle.IOS)
////                                alert.addAction(AlertAction(getString(R.string.delete_member), AlertActionStyle.NEGATIVE) { action ->
////                                    MainActivity.activity?.showProgress()
////
////                                    mSesame?.revokeKey(UUID.fromString(data.member.user_id)) { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, second: Any? ->
////                                        MainActivity.activity?.hideProgress()
////                                        refleshPage()
////                                    }
////                                })
////                                alert.show(MainActivity.activity as AppCompatActivity)
//
////                            }
//                        }
//                    }
//                }
//            }
//        }
        backicon.setOnClickListener {
            findNavController().navigateUp()
        }

        refleshPage()
    }

    fun refleshPage() {
//        mSesame?.getDeviceMembers {
//            it.onSuccess {
//                val ss: List<CHMemberAndOperater> = it.data
//                memberList.clear()
//                memberList.addAll(ss.sortedByDescending { it.member.role }).apply {
//                    recy?.post {
//                        recy?.adapter?.notifyDataSetChanged()
//                    }
//                }
//            }
//        }
    }

}