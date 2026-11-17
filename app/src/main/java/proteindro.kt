package com.example.proteinexplorer.data

import androidx.room.*

@Dao
interface ProteinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(protein: ProteinEntity)

    @Query("SELECT * FROM proteins")
    suspend fun getAll(): List<ProteinEntity>

    @Query("DELETE FROM proteins")
    suspend fun deleteAll()
}