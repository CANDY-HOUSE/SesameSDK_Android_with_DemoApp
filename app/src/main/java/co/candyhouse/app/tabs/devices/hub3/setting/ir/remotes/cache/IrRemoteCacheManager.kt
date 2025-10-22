package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote

object IrRemoteCacheManager {
    private const val PAGE_SIZE = 1000 // 每页最大数量
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
        // 重新组装分页数据，先删除旧数据。
        cacheDao?.deleteCacheByIRType(irType)
        val pages = remoteList.chunked(PAGE_SIZE)
        val entities = pages.mapIndexed { index, pageData ->
            RemoteCacheEntity(
                irType = irType,
                pageIndex = index,
                totalPages = pages.size,
                remoteList = pageData,
                timestamp = System.currentTimeMillis()
            )
        }
        cacheDao?.insertAll(entities)
    }

    // 获取有效缓存
    suspend fun getCache(irType: Int): List<IrRemote>? {
        ensureInitialized()
        val list = mutableListOf<IrRemote>()
        try {
            val pages = cacheDao?.getCacheByIRTypePages(irType)
            pages?.let { pageList ->
                for (page in pageList) {
                    list.addAll(page.remoteList)
                }
            }
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return list
    }

}