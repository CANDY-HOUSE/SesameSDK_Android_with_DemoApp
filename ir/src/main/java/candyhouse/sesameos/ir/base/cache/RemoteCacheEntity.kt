package candyhouse.sesameos.ir.base.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import candyhouse.sesameos.ir.base.IrRemote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "remote_cache")
@TypeConverters(IrRemoteListConverter::class)
data class RemoteCacheEntity(
    @PrimaryKey
    val brandType: Int,
    val remoteList: List<IrRemote>,
    val timestamp: Long = System.currentTimeMillis()
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