package com.example.tweetapp.repository

import com.example.tweetapp.dao.PostDao
import com.example.tweetapp.model.Post

class RoomRepository(private val postDao: PostDao) {

     suspend fun upsertNote(post: Post) = postDao.upsertNote(post)

     suspend fun insertAllNotes(note : List<Post>) = postDao.insertAllNotes(note)

     suspend fun deleteNote(note: Post) = postDao.deleteNote(note)

     suspend fun getAllNotes(uuid: String) = postDao.getAllNotes(uuid)

     fun getAllNotes() = postDao.getAllNotes()

     fun getUserWithNotes() = postDao.getUserWithNotes()

     fun getNotesByUserId(uuid : String) = postDao.getNoteByUserId(uuid)
 }