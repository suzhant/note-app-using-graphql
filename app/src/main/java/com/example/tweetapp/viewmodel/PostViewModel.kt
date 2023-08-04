package com.example.tweetapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.tweetapp.model.PostType
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.repository.PostRepository
import com.hasura.FetchPostQuery
import com.hasura.type.Post_pk_columns_input
import com.hasura.type.Post_set_input
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(private val repository: PostRepository) : ViewModel() {

    private val _post = MutableLiveData<ApiState<List<FetchPostQuery.Post>>>()
    val post : LiveData<ApiState<List<FetchPostQuery.Post>>> = _post

    private val _selectedPost = MutableLiveData<PostType>()
    val selectedPost : LiveData<PostType>  = _selectedPost

    fun setSelectedPost(post: PostType){
        _selectedPost.value = post
    }

    fun getPost(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _post.postValue(ApiState.Loading(true))
                val response = repository.fetchPost()
                if (!response.hasErrors()){
                    _post.postValue(ApiState.Success(response.data!!.post))
                }else{
                    _post.postValue(ApiState.Error(response.errors.toString()))
                }
            }catch (e: ApolloException){
                _post.postValue(ApiState.Error(e.message.toString()))
            }
        }
    }

     suspend fun updatePost(id: Post_pk_columns_input, input: Post_set_input){
        try {
            repository.updatePost(id,input)
        }catch (e : ApolloException){
            Log.d("error",e.message.toString())
        }
    }

      suspend fun createPost(title: String, body: String) {
         try {
            repository.createPost(title, body)
         }catch (e : ApolloException){
             Log.d("error",e.message.toString())
         }
    }

    suspend fun deletePost(id: Int){
        try {
            repository.removePost(id)
        } catch (e: ApolloException) {
            Log.d("error", e.message.toString())
        }
    }
}