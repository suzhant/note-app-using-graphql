package com.example.tweetapp.repository

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.hasura.CreateUserMutation
import com.hasura.DeleteUserByIdMutation
import com.hasura.GetAllUsersQuery
import com.hasura.GetUserByIdQuery
import com.hasura.UpdateUserByIdMutation
import com.hasura.type.User_insert_input
import com.hasura.type.User_set_input

class UserRepositoryImpl(private val apolloClient: ApolloClient) : UserRepository {
    override suspend fun getAllUsers(): ApolloResponse<GetAllUsersQuery.Data> {
        val mutation = GetAllUsersQuery()
        return apolloClient.query(mutation).execute()
    }

    override suspend fun createUser(uuid : String,email : String, username : String , profilePic: String): ApolloResponse<CreateUserMutation.Data> {
        val input = User_insert_input(uuid = Optional.present(uuid), email = Optional.present(email), profile_pic = Optional.present(profilePic), user_name = Optional.present(username))
        val mutation = CreateUserMutation(input)
        return apolloClient.mutation(mutation).execute()
    }

    override suspend fun updateUser(uuid : String,email : String, username : String , profilePic: String): ApolloResponse<UpdateUserByIdMutation.Data> {
        val input = User_set_input(email = Optional.present(email), profile_pic = Optional.present(profilePic), user_name = Optional.present(username))
        val mutation = UpdateUserByIdMutation(uuid,input)
        return apolloClient.mutation(mutation).execute()
    }

    override suspend fun deleteUser(uuid: String): ApolloResponse<DeleteUserByIdMutation.Data> {
        val mutation = DeleteUserByIdMutation(uuid = uuid)
        return apolloClient.mutation(mutation).execute()
    }

    override suspend fun getUserById(uuid: String): ApolloResponse<GetUserByIdQuery.Data> {
        val mutation = GetUserByIdQuery(uuid)
        return apolloClient.query(mutation).execute()
    }
}