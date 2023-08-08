package com.example.tweetapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tweetapp.dao.PostDao
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.User

@Database(entities = [Post::class,User::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao() : PostDao

    class MigrationFrom1To2 : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }

    class MigrationFrom2To3 : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Step 1: Create a temporary table
            database.execSQL("""
            CREATE TABLE temp_table (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL
            )
        """.trimIndent())

            // Step 2: Convert the data from the old column to the new column
            database.execSQL("""
            UPDATE notes SET id = CAST(id AS TEXT)
        """.trimIndent())

            // Step 3: Transfer data from the old table to the temporary table
            database.execSQL("""
            INSERT INTO temp_table (id,title,body)
            SELECT id,title,body FROM notes
        """.trimIndent())

            // Step 4: Drop the old table
            database.execSQL("DROP TABLE notes")

            // Step 5: Rename the temporary table to the original table name
            database.execSQL("ALTER TABLE temp_table RENAME TO notes")

            // Step 6: Add the new column to the existing table
            database.execSQL("ALTER TABLE notes ADD COLUMN timestamp INTEGER NOT NULL")

        }
    }

    class MigrationFrom3To4 : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {

            // Create a new translation table
            database.execSQL("""
            CREATE TABLE temp_table (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                user_id TEXT NOT NULL
            )
        """.trimIndent())

            // Step 3: Transfer data from the old table to the temporary table
            database.execSQL("""
            INSERT INTO temp_table (id,title,body,timestamp)
            SELECT id,title,body,timestamp FROM notes
        """.trimIndent())

            // Remove old table
            database.execSQL("DROP TABLE notes")

            // Change name of table to correct one
            database.execSQL("ALTER TABLE temp_table RENAME TO notes")

            database.execSQL("""
            CREATE TABLE users (
                uuid TEXT PRIMARY KEY NOT NULL,
                email TEXT NOT NULL,
                username TEXT NOT NULL,
                profilePic TEXT NOT NULL
            )
        """.trimIndent())
        }
    }

}