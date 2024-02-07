package com.example.tweetapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.UserWithNotes
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Upsert
    suspend fun upsertNote(note : Post)

    @Delete
    suspend fun deleteNote(note: Post)

    @Query("SELECT * FROM notes")
    fun getAllNotes() : Flow<List<Post>>

    @Query("SELECT * FROM users")
    fun getUserWithNotes(): Flow<List<UserWithNotes>>

    @Query("SELECT * FROM notes where user_id = :uuid")
    fun getNoteByUserId(uuid : String): Flow<List<Post>>

    @Query("SELECT * FROM notes where user_id = :uuid")
    suspend fun getAllNotes(uuid: String) : List<Post>
    @Query("SELECT * FROM notes where id = :postId")
    suspend fun getPostById(postId : String) : Post

    @Transaction
    suspend fun insertAllNotes(posts: List<Post>) {
        for (post in posts) {
            upsertNote(post)
        }
    }
}