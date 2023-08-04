package com.example.tweetapp.repository

import com.apollographql.apollo3.api.ApolloResponse
import com.hasura.ChangePostMutation
import com.hasura.FetchPostQuery
import com.hasura.InsertPostMutation
import com.hasura.RemovePostMutation
import com.hasura.type.Post_pk_columns_input
import com.hasura.type.Post_set_input

interface PostRepository {
    suspend fun createPost(title : String, body : String) : ApolloResponse<InsertPostMutation.Data>
    suspend fun fetchPost() : ApolloResponse<FetchPostQuery.Data>
    suspend fun removePost(id: Int) : ApolloResponse<RemovePostMutation.Data>
    suspend fun updatePost(id: Post_pk_columns_input, input: Post_set_input) : ApolloResponse<ChangePostMutation.Data>

}