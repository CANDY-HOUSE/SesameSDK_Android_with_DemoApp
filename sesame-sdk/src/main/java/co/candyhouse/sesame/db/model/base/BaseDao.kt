package co.candyhouse.sesame.db.model.base

import androidx.room.*

@Dao
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: MutableList<T>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(word: T)

    @Delete
    fun delete(element: T)

    @Delete
    fun deleteList(elements: MutableList<T>)

    @Delete
    fun deleteSome(vararg elements: T)

    @Update
    fun update(element: T)

    fun deleteAll()

    fun getAll(): List<T>

}