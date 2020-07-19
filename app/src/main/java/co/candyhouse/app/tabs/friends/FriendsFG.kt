/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.candyhouse.app.tabs.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.candyhouse.app.tabs.MainActivity
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import co.candyhouse.app.tabs.devices.ssm2.room.avatatImagGenaroter
import co.candyhouse.sesame.server.CHAccountManager
//import co.candyhouse.sesame.server.a.model.UserProfile
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseFG
import co.utils.L
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.AlertView
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import java.util.*

class FriendsFG : BaseFG() {

    companion object {
        var instance: FriendsFG? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
    }


    override fun onResume() {
        super.onResume()
        if (MainActivity.nowTab == 1) {
            (activity as MainActivity).showMenu()
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var swiperefreshView: SwipeRefreshLayout
//    val mFriends = ArrayList<UserProfile>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        L.d("hcia", "view:" )
        val view = inflater.inflate(R.layout.fg_friends, container, false)
        refleshPage()

        swiperefreshView = view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh).apply {
            setOnRefreshListener { refleshPage() }
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.recy).apply {
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
//                        override fun bind(data: UserProfile, pos: Int) {
//                            customName.text = data.nickname
////                            itemView.setOnClickListener {
////                                val alert = AlertView(data.nickname, "", AlertStyle.IOS)
////                                alert.addAction(AlertAction(getString(R.string.delete_friend), AlertActionStyle.NEGATIVE) { action ->
////                                    CHAccountManager.unfriend(data.id) {
////                                        refleshPage()
////                                    }
////                                })
////                                alert.show(MainActivity.activity as AppCompatActivity)
////                            }
//                            head.setImageDrawable(avatatImagGenaroter(data.first_name))
//                        }
//                    }
//                }
//            }
        }

        return view
    }


    fun refleshPage() {
//        CHAccountManager.myFriends {
//            it.onSuccess {
//                recyclerView?.post {
//                    mFriends.clear()
//                    mFriends.addAll(it.data.sortedBy { it.nickname })
//                    (recyclerView.adapter as GenericAdapter<*>).notifyDataSetChanged()
//                }
//            }
//            it.onFailure {
//            }
//            ThreadUtils.runOnUiThread {
//                swiperefreshView.isRefreshing = false
//            }
//        }
    }
}
