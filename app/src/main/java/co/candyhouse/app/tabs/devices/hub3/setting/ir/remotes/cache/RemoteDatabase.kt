package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database( entities = [ RemoteCacheEntity::class], version = 2, exportSchema = false)
@TypeConverters(IrRemoteListConverter::class)
abstract class RemoteDatabase : RoomDatabase() {
    abstract fun remoteCacheDao(): RemoteCacheDao
    companion object {
        @Volatile
        private var INSTANCE: RemoteDatabase? = null

        fun getInstance(context: Context): RemoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext,RemoteDatabase::class.java,"remote_database")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
