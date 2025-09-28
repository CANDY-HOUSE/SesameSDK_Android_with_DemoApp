package co.candyhouse.sesame.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.HttpResponseCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [CHDevice::class], version = 29, exportSchema = false)
internal abstract class CHDB : RoomDatabase() {

    abstract fun deviceDao(): ChDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: CHDB? = null

        fun getDatabase(): CHDB {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    CHBleManager.appContext,
                    CHDB::class.java,
                    "word_database"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }

    object CHSS2Model {
        private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val dao get() = getDatabase().deviceDao()

        fun getAllDB(onResponse: HttpResponseCallback<List<CHDevice>>) {
            dbScope.launch {
                try {
                    val devices = dao.getAll()
                    onResponse(Result.success(devices))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun getDevice(deviceID: String, onResponse: HttpResponseCallback<CHDevice>) {
            dbScope.launch {
                try {
                    val device = dao.getByUUID(deviceID)
                    if (device != null) {
                        onResponse(Result.success(device))
                    } else {
                        onResponse(Result.failure(CHError.NotfoundError.value))
                    }
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun insert(device: CHDevice, onResponse: HttpResponseCallback<String>) {
            dbScope.launch {
                try {
                    if (isValidUUID(device.deviceUUID)) {
                        dao.insert(device)
                        onResponse(Result.success(""))
                    } else {
                        onResponse(Result.failure(IllegalArgumentException("Invalid UUID")))
                    }
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun delete(device: CHDevice, onResponse: HttpResponseCallback<String>) {
            dbScope.launch {
                try {
                    dao.delete(device)
                    onResponse(Result.success(""))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun deleteByDeviceId(deviceId: String, onResponse: HttpResponseCallback<Int>) {
            dbScope.launch {
                try {
                    val deletedCount = dao.deleteByUUID(deviceId)
                    onResponse(Result.success(deletedCount))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun deleteByDeviceIds(deviceIds: List<String>, onResponse: HttpResponseCallback<Int>) {
            dbScope.launch {
                try {
                    val deletedCount = dao.deleteByUUIDs(deviceIds)
                    onResponse(Result.success(deletedCount))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun replaceAll(devices: List<CHDevice>, onResponse: HttpResponseCallback<Unit>) {
            dbScope.launch {
                try {
                    dao.replaceAll(devices)
                    onResponse(Result.success(Unit))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        private fun isValidUUID(uuid: String): Boolean {
            return try {
                UUID.fromString(uuid)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }
    }
}

@Dao
interface ChDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: CHDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<CHDevice>)

    @Delete
    suspend fun delete(device: CHDevice)

    @Update
    suspend fun update(device: CHDevice)

    @Query("SELECT * FROM CHDevice")
    suspend fun getAll(): List<CHDevice>

    @Query("SELECT * FROM CHDevice WHERE deviceUUID = :uuid LIMIT 1")
    suspend fun getByUUID(uuid: String): CHDevice?

    @Query("DELETE FROM CHDevice WHERE deviceUUID = :uuid")
    suspend fun deleteByUUID(uuid: String): Int

    @Query("DELETE FROM CHDevice WHERE deviceUUID IN (:deviceIds)")
    suspend fun deleteByUUIDs(deviceIds: List<String>): Int

    @Query("DELETE FROM CHDevice")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(devices: List<CHDevice>) {
        deleteAll()
        insertAll(devices)
    }
}