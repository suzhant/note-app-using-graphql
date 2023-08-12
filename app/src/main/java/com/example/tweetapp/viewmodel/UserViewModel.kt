package com.example.tweetapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.repository.ProtoRepository
import com.example.tweetapp.repository.UserRepository
import com.hasura.GetAllUsersQuery
import com.hasura.GetUserByIdQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val protoRepository: ProtoRepository,
) : ViewModel() {

    private val _users = MutableLiveData<ApiState<List<GetAllUsersQuery.User>>>()
    val users : LiveData<ApiState<List<GetAllUsersQuery.User>>> = _users

    private val _userData = MutableLiveData<ApiState<GetUserByIdQuery.User_by_pk>>()
    val userData : LiveData<ApiState<GetUserByIdQuery.User_by_pk>> = _userData

    private val _login = MutableLiveData(false)
    val login : LiveData<Boolean> = _login

    private val _workState = MutableLiveData<Boolean>(null)
    val workState : LiveData<Boolean> = _workState

    fun setWorkState(state : Boolean){
        _workState.value = state
    }

    val protoData = protoRepository.userPreferenceFLow.asLiveData()

    fun updateUserPrefDataStore(isFirstTime : Boolean, userId : String) =
        viewModelScope.launch(Dispatchers.IO) {
            protoRepository.updateValue(isFirstTime,userId)
        }

     fun getUserPrefById(userId: String) = protoRepository.getKeyByUserId(userId)

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