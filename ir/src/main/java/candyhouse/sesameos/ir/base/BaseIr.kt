package candyhouse.sesameos.ir.base

import androidx.lifecycle.MutableLiveData
import co.candyhouse.sesame.open.device.CHHub3

class BaseIr {

    companion object {
        val hub3Device = MutableLiveData<CHHub3>()
    }
}