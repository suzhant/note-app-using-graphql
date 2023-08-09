package com.example.tweetapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.model.User
import com.example.tweetapp.repository.UserRepository
import com.google.android.gms.common.api.Api
import com.hasura.GetAllUsersQuery
import com.hasura.GetUserByIdQuery
import com.hasura.UpdateUserByIdMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {

    private val _users = MutableLiveData<ApiState<List<GetAllUsersQuery.User>>>()
    val users : LiveData<ApiState<List<GetAllUsersQuery.User>>> = _users

    private val _userData = MutableLiveData<ApiState<GetUserByIdQuery.User_by_pk>>()
    val userData : LiveData<ApiState<GetUserByIdQuery.User_by_pk>> = _userData

    private val _login = MutableLiveData(false)
    val login : LiveData<Boolean> = _login

    fun setLogin(enable : Boolean){
        _login.value = enable
    }

     fun getAllUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            _users.postValue(ApiState.Loading(true))
            try {
                val response = userRepository.getAllUsers()
                if (!response.hasErrors()){
                    _users.postValue(ApiState.Success(response.data!!.user))
                }else{
                    _users.postValue(ApiState.Error(response.errors.toString()))
                }
            }catch (e: ApolloException){
                _users.postValue(ApiState.Error(e.message.toString()))
            }
        }
    }

    fun createUser(uuid : String,email : String, username : String , profilePic: String) = flow{
        emit(ApiState.Loading(true))
        try {
            val response = userRepository.createUser(uuid, email, username, profilePic).data
            if (response!=null){
                emit(ApiState.Success(response))
            }else{
                emit(ApiState.Error("response is null"))
            }
        }catch (e : Exception){
            emit(ApiState.Error(e.message.toString()))
        }
    }.catch {
        emit(ApiState.Error(it.message.toString()))
    }.flowOn(Dispatchers.IO)


    suspend fun deleteUser(uuid: String){
        userRepository.deleteUser(uuid)
    }

     fun updateUser(uuid : String,email : String, username : String , profilePic: String) = flow {
        emit(ApiState.Loading(true))
        try {
            val response = userRepository.updateUser(
                uuid = uuid,
                email = email,
                username = username,
                profilePic = profilePic
            ).data
            if (response!=null){
                emit(ApiState.Success(response))
            }else{
                emit(ApiState.Error("response is null"))
            }
        }catch (e : Exception){
            emit(ApiState.Error(e.message.toString()))
        }

    }.catch {
         emit(ApiState.Error(it.message.toString()))
     }.flowOn(Dispatchers.IO)

    suspend fun getUserById(uuid: String){
         try{
            val response = userRepository.getUserById(uuid)
            _userData.value = ApiState.Success(response.data?.user_by_pk!!)
        }catch (e : ApolloException){
             _userData.value = ApiState.Error(e.message.toString())
        }
    }

}