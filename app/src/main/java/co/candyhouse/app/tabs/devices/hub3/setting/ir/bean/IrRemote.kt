package co.candyhouse.app.tabs.devices.hub3.setting.ir.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IrRemote(
    var model: String?, // 品牌名称
    var alias: String,  //别名
    val uuid: String,   // 唯一识别号
    var state: String?, // 状态编码
    val timestamp: Long,
    val type: Int,     // 类型设备
    var code: Int = -1, // 设备类型code，默认为-1，0:学习，其他：具体码组Code
    val keys: Array<String>? = emptyArray(), // 键值列表
    val direction: String?,  // 索引
    var haveSave:Boolean = true // 是否已保存,默认为true
) : Parcelable {
    override fun toString(): String {
        return "IrRemote(model=$model, alias='$alias', uuid='$uuid', state=$state, timestamp=$timestamp, type=$type, code=$code, keys=${keys?.contentToString()?:""}, direction='$direction')"
    }

    fun clone(): IrRemote {
        val newIrRemote = IrRemote(
            model = this.model,
            alias = this.alias,
            uuid = this.uuid,
            state = this.state,
            timestamp = this.timestamp,
            type = this.type,
            code = this.code,
            keys = keys?.toList()?.toTypedArray() ?: emptyArray(),
            direction = this.direction
        )
        return newIrRemote
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IrRemote

        if (model != other.model) return false
        if (alias != other.alias) return false
        if (uuid != other.uuid) return false
        if (state != other.state) return false
        if (timestamp != other.timestamp) return false
        if (type != other.type) return false
        if (code != other.code) return false
        if (keys == null) {
            if (other.keys != null) return false
        } else if (!keys.contentEquals(other.keys ?: return false)) return false
        if (direction != other.direction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = model?.hashCode() ?: 0
        result = 31 * result + alias.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + type
        result = 31 * result + code
        result = 31 * result + (keys?.contentHashCode()?:0)
        result = 31 * result + (direction?.hashCode() ?: 0)
        return result
    }


}