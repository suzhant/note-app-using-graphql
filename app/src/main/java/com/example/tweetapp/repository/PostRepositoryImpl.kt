package com.example.tweetapp.repository

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.hasura.ChangePostMutation
import com.hasura.FetchPostQuery
import com.hasura.InsertPostMutation
import com.hasura.RemovePostMutation
import com.hasura.type.Post_pk_columns_input
import com.hasura.type.Post_set_input

class PostRepositoryImpl(private val apolloClient: ApolloClient) : PostRepository {

    override suspend fun createPost(
        title: String,
        body: String,
    ): ApolloResponse<InsertPostMutation.Data> {
        val mutation = InsertPostMutation(Optional.present(title),Optional.present(body))
        return apolloClient.mutation(mutation).execute()
    }

    override suspend fun fetchPost(): ApolloResponse<FetchPostQuery.Data> {
        val mutation = FetchPostQuery()
        return apolloClient.query(mutation).execute()
    }

    override suspend fun removePost(id: Int): ApolloResponse<RemovePostMutation.Data> {
        val mutation = RemovePostMutation(id)
         return  apolloClient.mutation(mutation).execute()
    }

    override suspend fun updatePost(id: Post_pk_columns_input,input:Post_set_input): ApolloResponse<ChangePostMutation.Data> {
        val mutation = ChangePostMutation(id,input)
        return  apolloClient.mutation(mutation).execute()
    }

}