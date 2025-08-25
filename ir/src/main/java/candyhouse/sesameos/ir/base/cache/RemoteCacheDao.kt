package candyhouse.sesameos.ir.base.cache

import androidx.room.*
import co.candyhouse.sesame.db.model.base.BaseDao

@Dao
interface RemoteCacheDao : BaseDao<RemoteCacheEntity> {

    @Query("SELECT * FROM remote_cache WHERE brandType = :brandType")
    suspend fun getCacheByBrandType(brandType: Int): RemoteCacheEntity?

    @Query("SELECT * FROM remote_cache WHERE brandType = :brandType")
    fun getCacheByBrandTypeSync(brandType: Int): RemoteCacheEntity?

    @Query("DELETE FROM remote_cache WHERE brandType = :brandType")
    suspend fun deleteCacheByBrandType(brandType: Int)

    @Query("SELECT * FROM remote_cache")
    override fun getAll(): List<RemoteCacheEntity>

    @Query("DELETE FROM remote_cache")
    override fun deleteAll()

    @Query("DELETE FROM remote_cache WHERE timestamp < :expiryTime")
    suspend fun deleteExpiredCache(expiryTime: Long)


    // 获取未过期的缓存
    @Query("SELECT * FROM remote_cache WHERE brandType = :brandType AND timestamp > :expiryTime")
    suspend fun getValidCache(brandType: Int, expiryTime: Long): RemoteCacheEntity?
}