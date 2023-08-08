package com.example.tweetapp.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apollographql.apollo3.api.Optional
import com.example.tweetapp.model.Action
import com.example.tweetapp.repository.RemoteRepository
import com.example.tweetapp.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import com.hasura.FetchNoteQuery
import com.hasura.type.Note_pk_columns_input
import com.hasura.type.Note_set_input
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import java.util.Date

@HiltWorker
class RemoteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val roomRepository: RoomRepository,
    private val remoteRepository: RemoteRepository,
) : CoroutineWorker(context, params){
    override suspend fun doWork(): Result {
        return try {
            val actionInput =
                inputData.getString(ACTION) ?: return Result.failure()
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser!=null){
                updateServer(actionInput,auth)
                Result.success()
            }else{
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun updateServer(action : String,auth: FirebaseAuth) {
        val notes = roomRepository.getAllNotes(auth.uid.toString())
        Log.d("remoteWorker","remote called")
        val localEntity = notes.map { note ->
            FetchNoteQuery.Note(
                id = note.id,
                title = note.title,
                body = note.body,
                timestamp = note.timestamp,
                uuid = note.uuid
            )
        }
        val apiData = remoteRepository.fetchPost(auth.uid.toString()).data?.note
        // Step 1: Find newly added entities in the room data and insert them into the api
        if (action == "SYNC" || action == Action.CREATE.name){
            localEntity.filter { localData -> localData !in apiData.orEmpty()}
                .forEach { newEntity ->
                    remoteRepository.createPost(id = newEntity.id, title = newEntity.title, body = newEntity.body, timestamp = newEntity.timestamp.toString().toLong(), uuid = auth.uid.toString())
                }

        }

        // Step 2: Find updated entities and update them in the local database
        if (action == "SYNC" || action == Action.EDIT.name){
            localEntity.filter { localData -> apiData?.any { it.id == localData.id && it !=  localData } == true }
                .forEach { updatedEntity ->
                    val id = Note_pk_columns_input(updatedEntity.id)
                    val input = Note_set_input(title = Optional.present(updatedEntity.title), body = Optional.present(updatedEntity.body), timestamp = Optional.present(Date().time))
                    remoteRepository.updatePost(id,input)
                }
        }

        // Step 3: Find deleted entities and remove them from the remote database
        if (action == "SYNC" || action == Action.DELETE.name){
            apiData?.filterNot { apiEntity -> localEntity.any { it.id == apiEntity.id } }
                ?.forEach { deletedEntity ->
                    remoteRepository.removePost(deletedEntity.id)
                }
        }
    }

    companion object{
        const val ACTION = "action"
    }
}