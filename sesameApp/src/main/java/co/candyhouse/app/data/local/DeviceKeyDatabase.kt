package co.candyhouse.app.data.local

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import co.candyhouse.sesame.ble.CHDevice
import co.candyhouse.sesame.ble.CHKeyPersistence
import co.candyhouse.sesame.utils.HttpResponseCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [DeviceKeyEntity::class], version = 29, exportSchema = false)
internal abstract class DeviceKeyDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceKeyDao

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: DeviceKeyDatabase? = null

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        private fun database(): DeviceKeyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, DeviceKeyDatabase::class.java, "word_database")
                .build().also { instance = it }
        }
    }

    object Keys : CHKeyPersistence {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val dao get() = database().deviceDao()
        fun getAllDB(onResponse: HttpResponseCallback<List<CHDevice>>) {
            scope.launch { onResponse(runCatching { dao.getAll().map { it.toKey() } }) }
        }

        override fun insert(device: CHDevice, onResponse: HttpResponseCallback<String>) {
            scope.launch {
                onResponse(runCatching {
                    UUID.fromString(device.deviceUUID)
                    dao.insert(DeviceKeyEntity.from(device.copy(deviceUUID = device.deviceUUID.lowercase())))
                    ""
                })
            }
        }

        override fun deleteByDeviceId(deviceId: String, onResponse: HttpResponseCallback<Int>) {
            scope.launch { onResponse(runCatching { dao.deleteByUUID(deviceId.lowercase()) }) }
        }
    }
}

@Dao
internal interface DeviceKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceKeyEntity)

    @Query("SELECT * FROM CHDevice")
    suspend fun getAll(): List<DeviceKeyEntity>

    @Query("DELETE FROM CHDevice WHERE deviceUUID = :uuid")
    suspend fun deleteByUUID(uuid: String): Int
}
