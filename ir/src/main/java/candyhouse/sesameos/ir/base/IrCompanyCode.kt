package candyhouse.sesameos.ir.base

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IrCompanyCode(
    val code: List<Int>,
    val name: String,
    val direction:String = ""
): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IrCompanyCode

        if (!code.equals(other.code)) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}