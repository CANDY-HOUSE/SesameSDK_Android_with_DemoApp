package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RemoteCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RemoteCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RemoteCacheEntity>)

    @Delete
    suspend fun delete(entity: RemoteCacheEntity)

    @Delete
    suspend fun deleteList(entities: List<RemoteCacheEntity>)

    @Delete
    suspend fun deleteSome(vararg entities: RemoteCacheEntity)

    @Update
    suspend fun update(entity: RemoteCacheEntity)

    @Query("SELECT * FROM remote_cache")
    suspend fun getAll(): List<RemoteCacheEntity>

    @Query("DELETE FROM remote_cache")
    suspend fun deleteAll()

    @Query("SELECT * FROM remote_cache WHERE irType = :irType")
    suspend fun getCacheByIRType(irType: Int): RemoteCacheEntity?

    @Query("SELECT * FROM remote_cache WHERE irType = :irType")
    fun getCacheByIRTypeSync(irType: Int): RemoteCacheEntity?

    @Query("DELETE FROM remote_cache WHERE irType = :irType")
    suspend fun deleteCacheByIRType(irType: Int)

    @Query("DELETE FROM remote_cache WHERE timestamp < :expiryTime")
    suspend fun deleteExpiredCache(expiryTime: Long)

    @Query("SELECT * FROM remote_cache WHERE irType = :irType AND timestamp > :expiryTime")
    suspend fun getValidCache(irType: Int, expiryTime: Long): RemoteCacheEntity?

    @Query("SELECT * FROM remote_cache WHERE irType = :irType ORDER BY pageIndex")
    suspend fun getCacheByIRTypePages(irType: Int): List<RemoteCacheEntity>
}