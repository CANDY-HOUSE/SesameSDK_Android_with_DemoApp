package co.candyhouse.app.tabs.devices.model

import androidx.lifecycle.ViewModel
import co.candyhouse.app.ext.aws.AWSLoginState
import kotlinx.coroutines.flow.MutableStateFlow

class CHLoginViewModel : ViewModel() {
    val gUserState = MutableStateFlow(AWSLoginState.UNKNOWN)
    var isJustLogin = false
}
