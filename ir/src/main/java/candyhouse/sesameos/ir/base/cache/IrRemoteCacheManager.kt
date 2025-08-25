package candyhouse.sesameos.ir.base.cache

import android.content.Context
import candyhouse.sesameos.ir.base.IrRemote

object IrRemoteCacheManager {
    private const val CACHE_EXPIRY_TIME = 24 * 60 * 60 * 1000L // 24小时

    private var cacheDao: RemoteCacheDao? = null
    private var isInitialized = false

    // 改进初始化方法，支持懒加载
    fun init(context: Context) {
        if (!isInitialized) {
            val database = RemoteDatabase.getInstance(context)
            cacheDao = database.remoteCacheDao()
            isInitialized = true
        }
    }

    // 确保已初始化
    private fun ensureInitialized() {
        if (!isInitialized || cacheDao == null) {
            throw IllegalStateException("IrRemoteCacheManager must be initialized before use")
        }
    }

    // 保存缓存
    suspend fun saveCache(brandType: Int, remoteList: List<IrRemote>) {
        ensureInitialized()
        val entity = RemoteCacheEntity(
            brandType = brandType,
            remoteList = remoteList,
            timestamp = System.currentTimeMillis()
        )
        cacheDao?.insert(entity)
    }

    // 获取有效缓存
    suspend fun getValidCache(brandType: Int): List<IrRemote>? {
        ensureInitialized()
        val expiryTime = System.currentTimeMillis() - CACHE_EXPIRY_TIME
        val cache = cacheDao?.getValidCache(brandType, expiryTime)
        return cache?.remoteList
    }

}