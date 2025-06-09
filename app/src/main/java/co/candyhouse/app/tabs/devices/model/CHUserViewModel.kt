package co.candyhouse.app.tabs.devices.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.server.CHUser
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import kotlinx.coroutines.flow.MutableStateFlow

class CHUserViewModel : ViewModel() {
    var isLoaded = false

    val userViewModel = MutableLiveData<CHUser>()

    val myFriends = MutableStateFlow(ArrayList<CHUser>())

    var needRefresh = MutableLiveData<Boolean>()
    var lastDataSize = MutableLiveData<Int>()

    fun clearFriend() {
        isLoaded = false
        SharedPreferencesUtils.isNeedFreshFriend = false
        myFriends.value.apply {
            clear()
        }.run {
            needRefresh.postValue(false)
        }
    }

    fun syncFriendsFromServer() {
        L.d("hcia", "刷新朋友 syncFriendsFromServer" + AWSStatus.getAWSLoginStatus())

        if (AWSStatus.getAWSLoginStatus()) {
            needRefresh.postValue(true)

            CHLoginAPIManager.getFriends(myFriends.value.lastOrNull()?.sub) { result ->
                result.onSuccess {
                    isLoaded = true
                    SharedPreferencesUtils.isNeedFreshFriend = false

                    L.d("hcia", "朋友數目 size:" + myFriends.value.size + " -->  :" + it.data.size)
                    lastDataSize.postValue(it.data.size)
                    synchronized(myFriends.value) {
                        val distinctData = it.data.filterNot { newData ->
                            myFriends.value.any { existingData -> existingData.email == newData.email }
                        }
                        myFriends.value = ArrayList(myFriends.value).apply {
                            addAll(distinctData)
                        }
                    }

                    L.d("hcia", "朋友數目 size:" + myFriends.value.size + " -->  :" + it.data.size)
                }
                result.onFailure {
                    L.d("hcia", "刷新朋友失敗 it:$it")
                }
                needRefresh.postValue(false)
            }
        }
    }

    fun addFriend(user: CHUser) {
        myFriends.value.add(user)
    }

    fun removeFriend(user: CHUser) {
        myFriends.value.remove(user)
    }
}