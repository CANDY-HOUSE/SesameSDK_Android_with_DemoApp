package candyhouse.sesameos.ir.base

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IrMatchRemote(
    val irRemote: IrRemote,
    val matchPercent: String
) : Parcelable
