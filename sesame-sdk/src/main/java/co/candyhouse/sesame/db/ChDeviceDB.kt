package co.candyhouse.sesame.db


import androidx.room.*
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.db.model.*
import co.candyhouse.sesame.db.model.base.BaseDao
import co.candyhouse.sesame.db.model.base.BaseModel
import co.candyhouse.sesame.open.HttpResponseCallback
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

internal var httpScope = CoroutineScope(IO)

@Database(entities = [CHDevice::class], version = 29, exportSchema = false)
internal abstract class CHDB : RoomDatabase() {
    object CHSS2Model : BaseModel<CHDevice>(getDatabase().ss2dao()) {
        fun getDevice(deviceID: String, onResponse: HttpResponseCallback<CHDevice>) = httpScope.launch {
//                    L.d("hcia", "有沒有匹配上啊？ deviceID:" + deviceID)
//            val tmpdev = CHSS2Model.mDao.getAll()
            val tmp = CHSS2Model.mDao.getAll().filter { it.deviceUUID == deviceID }
            if (tmp.count() != 0) {
//            L.d("hcia", "匹配上 ! tmpdev:" + tmp.first().profile?.name + " nickname:" + tmp.first().nickname)
                onResponse.invoke(Result.success(tmp.first()))
            } else {
              L.d("hcia", "!!!沒有匹配上啊 deviceID:" + deviceID)
                onResponse.invoke(Result.failure(CHError.NotfoundError.value))
            }
        }

        fun delete(device: CHDevice, onResponse: HttpResponseCallback<Any>) {
            mExecutor.execute {
                CHSS2Model.mDao.delete(device)
                onResponse.invoke(Result.success(""))
            }
        }
    }

    abstract fun ss2dao(): CHSS2Dao



    companion object {
        @Volatile
        private var INSTANCE: CHDB? = null
        fun getDatabase(): CHDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        CHBleManager.appContext!!,
                        CHDB::class.java,
                        "word_database"
                )
//                        .addMigrations(MIGRATION_29_30)
                        .fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


@Dao
interface CHSS2Dao : BaseDao<CHDevice> {
    @Query("delete from CHDevice")
    override fun deleteAll()

    @Query("SELECT * from CHDevice")
    override fun getAll(): List<CHDevice>
}

