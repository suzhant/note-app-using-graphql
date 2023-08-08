package com.example.tweetapp.repository

import com.apollographql.apollo3.api.ApolloResponse
import com.hasura.CreateUserMutation
import com.hasura.DeleteUserByIdMutation
import com.hasura.GetAllUsersQuery
import com.hasura.GetUserByIdQuery
import com.hasura.UpdateUserByIdMutation

interface UserRepository {
    suspend fun getAllUsers() : ApolloResponse<GetAllUsersQuery.Data>
    suspend fun createUser(uuid : String,email : String, username : String , profilePic: String) : ApolloResponse<CreateUserMutation.Data>
    suspend fun updateUser(uuid : String,email : String, username : String , profilePic: String) : ApolloResponse<UpdateUserByIdMutation.Data>
    suspend fun deleteUser(uuid: String) : ApolloResponse<DeleteUserByIdMutation.Data>
    suspend fun getUserById(uuid: String) : ApolloResponse<GetUserByIdQuery.Data>
}