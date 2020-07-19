package co.candyhouse.app.tabs.devices.ssm2.menber

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.app.base.scan.ScanCallBack
import co.candyhouse.app.R
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_add_member.*


class AddMemberFG : BaseSSMFG() {
//    var mFriends = ArrayList<UserProfile>()

    companion object {
//        var memberList = ArrayList<CHMemberAndOperater>()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_add_member, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swiperefresh.apply {
            setOnRefreshListener { refleshPage() }
        }
        recy.apply {
            setHasFixedSize(true)
//            adapter = object : GenericAdapter<UserProfile>(mFriends) {
//                override fun getLayoutId(position: Int, obj: UserProfile): Int {
//                    return R.layout.cell_friend
//                }
//
//                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
//                    return object : RecyclerView.ViewHolder(view), Binder<UserProfile> {
//                        var customName = itemView.findViewById<TextView>(R.id.title)
//                        var head = itemView.findViewById<ImageView>(R.id.avatar)
//
//                        @SuppressLint("SetTextI18n")
//                        override fun bind(data: UserProfile, pos: Int) {
//                            customName.text = data.nickname
//                            itemView.setOnClickListener {
////                                val alert = AlertView(data.nickname, "", AlertStyle.IOS)
////                                alert.addAction(AlertAction(getString(R.string.add_member), AlertActionStyle.NEGATIVE) { action ->
////                                    MainActivity.activity?.showProgress()
////                                    mSesame?.shareKey(data.id) { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, second: Any? ->
////                                        MainActivity.activity?.hideProgress()
////                                        itemView?.post {
////                                            findNavController().navigateUp()
////                                        }
////                                    }
////                                })
////                                alert.show(MainActivity.activity as AppCompatActivity)
//                            }
//                            head.setImageDrawable(avatatImagGenaroter(data.firstName))
//                        }
//                    }
//                }
//            }
        }

        backicon.setOnClickListener {
            findNavController().navigateUp()
        }
        right_icon.setOnClickListener {
            co.candyhouse.app.base.scan.ScanFG.callBack = object : ScanCallBack {
                override fun onScanFriendSuccess(friendID: String) {
//                    MainActivity.activity?.showProgress()
//                    mSesame?.shareKey(UUID.fromString(friendID)) { cmd: SSM2ItemCode?, res: SSM2CmdResultCode?, second: Any? ->
//                        MainActivity.activity?.hideProgress()
//                        right_icon?.post {
//                            findNavController().navigateUp()
//                        }
//                    }
                }
            }
            findNavController().navigate(R.id.to_scan)
        }
        refleshPage()
    }

    fun refleshPage() {

//        CHAccountManager.myFriends {
//            it.onSuccess {
//                recy?.post {
//                    mFriends.clear()
//                    mFriends.addAll(it.data.sortedBy { it.nickname }.filterNot {
//                        (memberList.map { it.member.operator_id }).contains(it.id.toString())
//                    })
//                    recy?.adapter?.notifyDataSetChanged()
////                    (recy?.adapter as? GenericAdapter<*>)?.notifyDataSetChanged()
//                }
//            }
//            it.onFailure {
//            }
//            swiperefresh?.post {
//                swiperefresh?.isRefreshing = false
//            }
//        }
    }

}