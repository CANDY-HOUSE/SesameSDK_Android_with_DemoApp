package co.candyhouse.sesame.db.model.base

import co.candyhouse.sesame.open.HttpResponseCallback
import co.candyhouse.sesame.utils.L
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.Executors


open class BaseModel<T>(dao: BaseDao<T>) {
    internal val mExecutor: Executor = Executors.newSingleThreadExecutor()
    internal val mDao = dao

    init {
        getAllDB {

        }
    }

    fun insert(data: T, onResponse: HttpResponseCallback<List<T>>): BaseModel<T> {
        mExecutor.execute {
            mDao.insert(data)
            try {
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
                L.d("hcia", "getAllDB:error:" + error)
            }
        }

        return this
    }
}