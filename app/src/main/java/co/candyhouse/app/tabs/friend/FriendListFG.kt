package co.candyhouse.app.tabs.friend

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgFriendListBinding
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.app.tabs.devices.model.CHUserViewModel
import co.candyhouse.server.CHUser
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter
import co.utils.recycle.loadmoreadapter.LoadMoreAdapter
import co.utils.recycle.loadmoreadapter.LoadMoreAdapter.Companion.STATE_NORMAL
import co.utils.recycle.loadmoreadapter.LoadMoreAdapter.Companion.STATE_NO_MORE_DATA
import co.utils.safeNavigate
import kotlinx.coroutines.launch

class FriendListFG : HomeFragment<FgFriendListBinding>() {
    override fun getViewBinder() = FgFriendListBinding.inflate(layoutInflater)

    private val userViewModel: CHUserViewModel by activityViewModels()
    private lateinit var adapter: GenericAdapter<Any>
    private var loadMoreAdapter: LoadMoreAdapter<*>? = null

    override fun setupUI() {
        initializeAdapter()

        bind.recyEmpty.setEmptyView(bind.emptyView)

        loadMoreAdapter = LoadMoreAdapter.wrap(adapter)
        bind.recyEmpty.adapter = loadMoreAdapter
    }

    override fun setupListeners() {
        bind.swiperefresh.setOnRefreshListener {
            userViewModel.myFriends.value.clear()
            userViewModel.syncFriendsFromServer()
        }
        loadMoreAdapter?.setOnLoadMoreListener {
            L.d("hcia", "setOnLoadMoreListener:")
            userViewModel.syncFriendsFromServer()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun <T : View> observeViewModelData(view: T) {
        if (!userViewModel.isLoaded || SharedPreferencesUtils.isNeedFreshFriend) {
            userViewModel.syncFriendsFromServer()
        }
        userViewModel.needRefresh.observe(viewLifecycleOwner) { isR ->
            bind.swiperefresh.isRefreshing = isR
            bind.recyEmpty.adapter?.notifyDataSetChanged()
        }
        userViewModel.lastDataSize.observe(viewLifecycleOwner) { lastCount ->
            if (lastCount < 10) {
                loadMoreAdapter?.setState(STATE_NO_MORE_DATA)
            } else {
                loadMoreAdapter?.setState(STATE_NORMAL)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.myFriends.collect { friendsList ->
                L.d("hcia", "朋友數目 size:" + friendsList.size + " -->  :")
                adapter.updateList(friendsList.toMutableList())
            }
        }
    }

    private fun initializeAdapter() {
        adapter = object : GenericAdapter<Any>(userViewModel.myFriends.value.toMutableList()) {
            override fun getLayoutId(position: Int, obj: Any): Int = R.layout.friend_cell

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(view), Binder<CHUser> {
                    var title: TextView = view.findViewById(R.id.title)

                    @SuppressLint("SetTextI18n")
                    override fun bind(data: CHUser, pos: Int) {
                        title.text = data.nickname ?: data.email
                        itemView.setOnClickListener {
                            userViewModel.userViewModel.value = data
                            safeNavigate(R.id.action_deviceListPG_to_FriendDetailFG)
                        }
                    }
                }
            }
        }
    }

}