package com.example.tweetapp.repository

import com.apollographql.apollo3.api.ApolloResponse
import com.hasura.CreateNoteByIdMutation
import com.hasura.FetchNoteQuery
import com.hasura.RemoveNoteMutation
import com.hasura.UpdateNoteMutation
import com.hasura.type.Note_pk_columns_input
import com.hasura.type.Note_set_input

interface RemoteRepository {
    suspend fun createPost(id : String,title : String, body : String,timestamp: Long,uuid: String) : ApolloResponse<CreateNoteByIdMutation.Data>
    suspend fun fetchPost(uuid: String) : ApolloResponse<FetchNoteQuery.Data>
    suspend fun removePost(id: String) : ApolloResponse<RemoveNoteMutation.Data>
    suspend fun updatePost(id: Note_pk_columns_input, input: Note_set_input) : ApolloResponse<UpdateNoteMutation.Data>
}