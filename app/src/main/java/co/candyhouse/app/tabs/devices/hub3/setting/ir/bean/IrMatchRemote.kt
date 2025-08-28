package co.candyhouse.app.tabs.devices.hub3.setting.ir.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IrMatchRemote(
    val irRemote: IrRemote,
    val matchPercent: String
) : Parcelable
