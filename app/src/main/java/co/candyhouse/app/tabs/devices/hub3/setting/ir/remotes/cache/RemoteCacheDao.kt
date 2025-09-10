package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache

import androidx.room.*
import co.candyhouse.sesame.db.model.base.BaseDao

@Dao
interface RemoteCacheDao : BaseDao<RemoteCacheEntity> {

    @Query("SELECT * FROM remote_cache WHERE irType = :irType")
    suspend fun getCacheByIRType(irType: Int): RemoteCacheEntity?

    @Query("SELECT * FROM remote_cache WHERE irType = :irType")
    fun getCacheByIRTypeSync(irType: Int): RemoteCacheEntity?

    @Query("DELETE FROM remote_cache WHERE irType = :irType")
    suspend fun deleteCacheByIRType(irType: Int)

    @Query("SELECT * FROM remote_cache")
    override fun getAll(): List<RemoteCacheEntity>

    @Query("DELETE FROM remote_cache")
    override fun deleteAll()

    @Query("DELETE FROM remote_cache WHERE timestamp < :expiryTime")
    suspend fun deleteExpiredCache(expiryTime: Long)


    // 获取未过期的缓存
    @Query("SELECT * FROM remote_cache WHERE irType = :irType AND timestamp > :expiryTime")
    suspend fun getValidCache(irType: Int, expiryTime: Long): RemoteCacheEntity?
}