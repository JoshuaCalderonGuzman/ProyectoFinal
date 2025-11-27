package com.proyectofinal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ⬇NUEVA MIGRACIÓN para añadir la columna dueDateTimestamp ⬇
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ejecuta el SQL para añadir la columna.
        // INTEGER se usa para almacenar Long (timestamp).
        // DEFAULT NULL permite que las notas existentes tengan valor nulo sin problema.
        db.execSQL("ALTER TABLE items ADD COLUMN dueDateTimestamp INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE items ADD COLUMN photoPaths TEXT NOT NULL DEFAULT '[]'")
        // 3. Añadir videoPaths (maneja List<String> con TypeConverter)
        db.execSQL("ALTER TABLE items ADD COLUMN videoPaths TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE items ADD COLUMN audioPaths TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE items ADD COLUMN filePaths TEXT NOT NULL DEFAULT '[]'")

    }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Crear la nueva tabla con la estructura final
        db.execSQL("CREATE TABLE items_new (id INTEGER PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT, isTask INTEGER NOT NULL, isCompleted INTEGER NOT NULL, timestamp INTEGER NOT NULL, reminderTimestamps TEXT NOT NULL DEFAULT '[]', photoPaths TEXT NOT NULL DEFAULT '[]', videoPaths TEXT NOT NULL DEFAULT '[]', audioPaths TEXT NOT NULL DEFAULT '[]', filePaths TEXT NOT NULL DEFAULT '[]')")

        // 2. Copiar datos de la tabla vieja a la nueva.
        // Se transforma el viejo dueDateTimestamp (si existe) en un arreglo JSON
        db.execSQL(
            "INSERT INTO items_new (id, title, description, isTask, isCompleted, timestamp, reminderTimestamps, photoPaths, videoPaths, audioPaths, filePaths) " +
                    "SELECT id, title, description, isTask, isCompleted, timestamp, " +
                    "CASE WHEN dueDateTimestamp IS NOT NULL THEN '[' || dueDateTimestamp || ']' ELSE '[]' END, " + // Migra el Long a List<Long> JSON
                    "photoPaths, videoPaths, audioPaths, filePaths FROM items"
        )

        // 3. Eliminar la tabla vieja y renombrar la nueva
        db.execSQL("DROP TABLE items")
        db.execSQL("ALTER TABLE items_new RENAME TO items")
    }
}

// ⬇ La versión de la base de datos se incrementa a 2 ⬇
@Database(entities = [Item::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
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
                    // ⬇ AÑADIR LA MIGRACIÓN AQUÍ ⬇
                    .addMigrations(MIGRATION_1_2,MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}