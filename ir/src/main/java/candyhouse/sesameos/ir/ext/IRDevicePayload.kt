package candyhouse.sesameos.ir.ext

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

typealias IRDeviceCode = String

data class IRDevicePayload(
        val uuid: String,
        var name: String,

        val deviceUUID: String,
        val type: IRDeviceCode,
       var keys: List<CHHub3IRCode>
)
@Parcelize
data class CHHub3IRCode(
        var irCodeID: UByte,
        val nameLength: UByte,
        val irCodeName: ByteArray? = null
): Parcelable