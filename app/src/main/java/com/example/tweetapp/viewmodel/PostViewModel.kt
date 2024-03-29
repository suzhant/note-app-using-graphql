package com.example.tweetapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.enums.PostType
import com.example.tweetapp.model.UserWithNotes
import com.example.tweetapp.repository.RemoteRepository
import com.example.tweetapp.repository.RoomRepository
import com.hasura.FetchNoteQuery
import com.hasura.type.Note_pk_columns_input
import com.hasura.type.Note_set_input
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val remoteRepository: RemoteRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val _post = MutableStateFlow<ApiState<List<FetchNoteQuery.Note>>>(ApiState.Loading)
    val post : StateFlow<ApiState<List<FetchNoteQuery.Note>>> = _post.asStateFlow()

    private val _selectedPost = MutableLiveData<PostType>()
    val selectedPost : LiveData<PostType>  = _selectedPost

    fun setSelectedPost(post: PostType){
        _selectedPost.value = post
    }

    fun getPost(userId: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _post.value = (ApiState.Loading)
                val response = remoteRepository.fetchPost(userId)
                if (!response.hasErrors()){
                    _post.value = (ApiState.Success(response.data!!.note))
                }else{
                    _post.value = (ApiState.Error(response.errors.toString()))
                }
            }catch (e: ApolloException){
                _post.value = (ApiState.Error(e.message.toString()))
            }
        }
    }

     suspend fun updatePost(id: Note_pk_columns_input, input: Note_set_input){
        try {
            remoteRepository.updatePost(id,input)
        }catch (e : ApolloException){
            Log.d("error",e.message.toString())
        }
    }

      suspend fun createPost(title: String, body: String,id : String, timestamp : Long,uuid: String) {
         try {
            remoteRepository.createPost(id = id, title = title, body = body,timestamp = timestamp,
                uuid = uuid
            )
         }catch (e : ApolloException){
             Log.d("error",e.message.toString())
         }
    }

     fun deletePost(note: Post){
         viewModelScope.launch(Dispatchers.IO) {
             try {
                 remoteRepository.removePost(note.id)
             } catch (e: ApolloException) {
                 Log.d("error", e.message.toString())
             }
         }
    }

    fun deleteNote(note: Post){
        viewModelScope.launch(Dispatchers.IO) {
            try {
              roomRepository.deleteNote(note)
              Log.d("room", "success deletion")
            } catch (e: Exception) {
                Log.d("error", e.message.toString())
            }
        }
    }


    fun upsertNote(note: Post){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                 roomRepository.upsertNote(note)
                 Log.d("room", "success update")
            } catch (e: Exception) {
                Log.d("room", e.message.toString())
            }
        }
    }

    fun insertAllNote(note: List<Post>){
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.insertAllNotes(note)
        }
    }

    fun getAllNotesFromLocal() : Flow<List<Post>> {
        return roomRepository.getAllNotes()
    }

    suspend fun getAllNotes(uuid: String) : List<Post>{
      return  roomRepository.getAllNotes(uuid)
    }

    fun getUserWithNotes() : Flow<List<UserWithNotes>>{
        return roomRepository.getUserWithNotes()
    }

    fun getNotesByUserId(uuid :String) : Flow<List<Post>>{
        return roomRepository.getNotesByUserId(uuid)
    }

     suspend fun getNoteById(postId: String) : Post{
       return withContext(Dispatchers.IO) {
           roomRepository.getNoteById(postId)
       }
    }

}