package com.proyectofinal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ⬇️ NUEVA MIGRACIÓN para añadir la columna dueDateTimestamp ⬇️
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ejecuta el SQL para añadir la columna.
        // INTEGER se usa para almacenar Long (timestamp).
        // DEFAULT NULL permite que las notas existentes tengan valor nulo sin problema.
        db.execSQL("ALTER TABLE items ADD COLUMN dueDateTimestamp INTEGER DEFAULT NULL")
    }
}

// ⬇️ La versión de la base de datos se incrementa a 2 ⬇️
@Database(entities = [Item::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // ⬇️ AÑADIR LA MIGRACIÓN AQUÍ ⬇️
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}