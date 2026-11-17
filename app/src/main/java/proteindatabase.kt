package com.example.proteinexplorer.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [ProteinEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ProteinDatabase : RoomDatabase() {

    abstract fun proteinDao(): ProteinDao

    companion object {

        @Volatile
        private var INSTANCE: ProteinDatabase? = null

        fun get(context: Context): ProteinDatabase {

            return INSTANCE ?: synchronized(this) {

                Room.databaseBuilder(
                    context.applicationContext,
                    ProteinDatabase::class.java,
                    "protein_database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}