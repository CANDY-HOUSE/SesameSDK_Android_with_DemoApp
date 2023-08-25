package co.candyhouse.app.tabs.devices.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import co.utils.CHUser

import co.utils.L
import co.utils.SharedPreferencesUtils
import kotlinx.coroutines.flow.MutableStateFlow

class CHUserViewModel : ViewModel() {
    var isLoaded = false

    val userViewModel = MutableLiveData<CHUser>()

    val myFriends = MutableStateFlow(ArrayList<CHUser>())

    var needRefresh = MutableLiveData<Boolean>()
    var lastDataSize = MutableLiveData<Int>()

    fun clearFriend() {
        isLoaded = true
        SharedPreferencesUtils.isNeedFreshFriend = false
        myFriends.value.apply {
            clear()
        }.run {
            needRefresh.postValue(false)
        }
    }

    fun syncFriendsFromServer() {
        L.d("hcia", "刷新朋友 syncFriendsFromServer")
        needRefresh.postValue(true)


    }


}