package com.planner.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Entry::class, Goal::class, CategoryEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun goalDao(): GoalDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN startTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE entries ADD COLUMN endTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goals ADD COLUMN deadline INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT OR IGNORE INTO categories (name, displayName, colorHex, isDefault) VALUES ('HEALTH', '운동', '4CAF50', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, displayName, colorHex, isDefault) VALUES ('MIND', '독서', '2196F3', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, displayName, colorHex, isDefault) VALUES ('FAMILY', '가족', 'FF9800', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, displayName, colorHex, isDefault) VALUES ('LANGUAGE', '외국어', '9C27B0', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, displayName, colorHex, isDefault) VALUES ('FINANCE', '재테크', 'F44336', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, displayName, colorHex, isDefault) VALUES ('TECHNOLOGY', '기술', '00BCD4', 1)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN entryType TEXT NOT NULL DEFAULT 'DURATION'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN count INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN entryType TEXT NOT NULL DEFAULT 'DURATION'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "planner_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { INSTANCE = it }
            }
        }
    }
}
