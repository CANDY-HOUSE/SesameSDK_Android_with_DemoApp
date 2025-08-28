package co.candyhouse.server.dto

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class IrRemoteInfoRespond (@SerializedName("type") val type: Int,
                           @SerializedName("count") val count: Int,
                           @SerializedName("data") val data: List<IrRemote>): Serializable {

}