package candyhouse.sesameos.ir.base.irHttpClientBean

import candyhouse.sesameos.ir.base.IrRemote
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class IrRemoteInfoRespond (@SerializedName("type") val type: Int,
                           @SerializedName("count") val count: Int,
                           @SerializedName("data") val data: List<IrRemote>): Serializable {

}