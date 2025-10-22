package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "remote_cache", primaryKeys = ["irType", "pageIndex"] ) // 复合主键 分页保存
@TypeConverters(IrRemoteListConverter::class)
data class RemoteCacheEntity(
    val irType: Int,
    val remoteList: List<IrRemote>,
    val timestamp: Long = System.currentTimeMillis(),
    val pageIndex: Int,
    val totalPages: Int
)

// TypeConverter 用于转换 List<IrRemote>
class IrRemoteListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromRemoteList(list: List<IrRemote>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toRemoteList(json: String): List<IrRemote> {
        val type = object : TypeToken<List<IrRemote>>() {}.type
        return gson.fromJson(json, type)
    }
}