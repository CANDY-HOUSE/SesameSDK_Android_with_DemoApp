package co.candyhouse.app.tabs.devices.model

import androidx.lifecycle.ViewModel
import com.amazonaws.mobile.client.UserState
import kotlinx.coroutines.flow.MutableStateFlow

class CHLoginViewModel : ViewModel() {
    val gUserState = MutableStateFlow(UserState.UNKNOWN)
    var isJustLogin = false
}
