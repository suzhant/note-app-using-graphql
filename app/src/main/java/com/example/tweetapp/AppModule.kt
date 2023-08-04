package com.example.tweetapp

import com.apollographql.apollo3.ApolloClient
import com.example.tweetapp.repository.PostRepository
import com.example.tweetapp.repository.PostRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApolloClient() : ApolloClient{
        return ApolloClient.Builder()
            .serverUrl("https://informed-ostrich-38.hasura.app/v1/graphql")
            .addHttpHeader("x-hasura-admin-secret","2xt3Gr0Ki0T014lWeRBowMWfl244YGgyVCi4ruXxvCToUJGGFlKeyT0Lv032e7wZ")
            .build()
    }

    @Provides
    @Singleton
    fun providePostRepository(apolloClient: ApolloClient) : PostRepository {
        return PostRepositoryImpl(apolloClient)
    }
}