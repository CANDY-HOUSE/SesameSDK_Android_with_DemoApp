package co.candyhouse.sesame.db.model.base

import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.HttpResponseCallback
import co.candyhouse.sesame.utils.L
import java.lang.Exception
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors


open class BaseModel<T>(dao: BaseDao<T>) {
    internal val mExecutor: Executor = Executors.newSingleThreadExecutor()
    internal val mDao = dao

    init {
        getAllDB {

        }
    }
    fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    fun insert(data: T, onResponse: HttpResponseCallback<List<T>>): BaseModel<T> {

        mExecutor.execute {
            try {
                if (data is CHDevice){
                    if (isValidUUID(data.deviceUUID)){
                        mDao.insert(data)
                    }
                }else{
                    mDao.insert(data)
                }
                onResponse.invoke(Result.success(mDao.getAll()))
            } catch (error: Exception) {
                onResponse.invoke(Result.failure(error))
                L.d("hcia", "getAllDB:error:" + error)
            }
        }
        return this
    }

    fun getAllDB(onResponse: HttpResponseCallback<List<T>>): BaseModel<T> {

        mExecutor.execute {
            try {
                onResponse.invoke(Result.success(mDao.getAll()))
            } catch (error: Exception) {
                onResponse.invoke(Result.failure(error))

                // 打印堆栈，追踪代码调用流程
                error.printStackTrace()
            }
        }

        return this
    }
    fun clearAll(){

        mExecutor.execute {
            try {
                mDao.deleteAll()

            } catch (error: Exception) {
                  error.printStackTrace()

            }
        }

    }
}