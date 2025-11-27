package com.proyectofinal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM items WHERE isTask = 0 ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isTask = 1 ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): Item?
}