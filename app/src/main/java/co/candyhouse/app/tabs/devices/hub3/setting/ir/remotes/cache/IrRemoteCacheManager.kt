package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote

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
    suspend fun saveCache(irType: Int, remoteList: List<IrRemote>) {
        ensureInitialized()
        val entity = RemoteCacheEntity(
            irType = irType,
            remoteList = remoteList,
            timestamp = System.currentTimeMillis()
        )
        cacheDao?.insert(entity)
    }

    // 获取有效缓存
    suspend fun getCache(irType: Int): List<IrRemote>? {
        ensureInitialized()
        val cache = cacheDao?.getCacheByIRType(irType)
        return cache?.remoteList
    }

}