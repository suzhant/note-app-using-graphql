package com.example.tweetapp.repository

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.hasura.CreateNoteByIdMutation
import com.hasura.FetchNoteQuery
import com.hasura.RemoveNoteMutation
import com.hasura.UpdateNoteMutation
import com.hasura.type.Note_insert_input
import com.hasura.type.Note_on_conflict
import com.hasura.type.Note_pk_columns_input
import com.hasura.type.Note_set_input
import com.hasura.type.String_comparison_exp
import com.hasura.type.note_constraint

class RemoteRepositoryImpl(private val apolloClient: ApolloClient) : RemoteRepository {

    override suspend fun createPost(
        id: String,
        title: String,
        body: String,
        timestamp : Long,
        uuid: String
    ): ApolloResponse<CreateNoteByIdMutation.Data> {
        val input = Note_insert_input(id = Optional.present(id), title = Optional.present(title), body = Optional.present(body), timestamp = Optional.present(timestamp), uuid = Optional.present(uuid))
        val onconflict = Note_on_conflict(constraint = note_constraint.note_pkey)
        val mutation = CreateNoteByIdMutation(input,Optional.present(onconflict))
        return apolloClient.mutation(mutation).execute()
    }

    override suspend fun fetchPost(uuid : String): ApolloResponse<FetchNoteQuery.Data> {
        val comparison = String_comparison_exp(_eq = Optional.present(uuid))
        val mutation = FetchNoteQuery(comparison)
        return apolloClient.query(mutation).execute()
    }

    override suspend fun removePost(id: String): ApolloResponse<RemoveNoteMutation.Data> {
        val mutation = RemoveNoteMutation(id)
         return  apolloClient.mutation(mutation).execute()
    }

    override suspend fun updatePost(id: Note_pk_columns_input,input:Note_set_input): ApolloResponse<UpdateNoteMutation.Data> {
        val mutation = UpdateNoteMutation(_set = Optional.present(input), pk_columns = id)
        return  apolloClient.mutation(mutation).execute()
    }

}