package com.example.tweetapp.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tweetapp.model.Post
import com.example.tweetapp.repository.RemoteRepository
import com.example.tweetapp.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile


@HiltWorker
class RoomSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val roomRepository: RoomRepository,
    private val remoteRepository: RemoteRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser!=null){
                updateRoom(auth)
                Result.success()
            }else{
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun updateRoom(auth : FirebaseAuth) {

        val apiData = remoteRepository.fetchPost(auth.uid.toString()).data?.note
        val mapToPost = apiData?.map {post ->
            Post(
                id = post.id,
                title = post.title,
                body = post.body,
                timestamp = post.timestamp.toString().toLong(),
                uuid = post.uuid
            )
        }

        val notes = roomRepository.getAllNotes(auth.uid.toString())
        Log.d("remoteWorker","room called")
        mapToPost?.filterNot { post -> notes.any { it.id == post.id } }
            ?.forEach { newEntity ->
                roomRepository.upsertNote(newEntity)
            }

        // Step 2: Find updated entities and update them in the local database
        mapToPost?.filter { post -> notes.any { it.id == post.id && it !=  post } }
            ?.forEach { updatedEntity ->
                roomRepository.upsertNote(updatedEntity)
            }
    }


}
