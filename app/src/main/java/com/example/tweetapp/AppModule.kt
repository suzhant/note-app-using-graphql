package com.example.tweetapp

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.work.impl.Migration_11_12
import androidx.work.impl.Migration_1_2
import com.apollographql.apollo3.ApolloClient
import com.example.tweetapp.dao.PostDao
import com.example.tweetapp.database.AppDatabase
import com.example.tweetapp.repository.RemoteRepository
import com.example.tweetapp.repository.RemoteRepositoryImpl
import com.example.tweetapp.repository.RoomRepository
import com.example.tweetapp.repository.UserRepository
import com.example.tweetapp.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun providePostRepository(apolloClient: ApolloClient) : RemoteRepository {
        return RemoteRepositoryImpl(apolloClient)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "note_database"
        ).addMigrations(AppDatabase.MigrationFrom1To2(),AppDatabase.MigrationFrom2To3(),AppDatabase.MigrationFrom3To4()).build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(db : AppDatabase) : PostDao{
        return db.postDao()
    }

    @Provides
    @Singleton
    fun provideRoomRepository(noteDao: PostDao) : RoomRepository {
        return RoomRepository(noteDao)
    }

    @Provides
    @Singleton
    fun provideUserRepository(apolloClient: ApolloClient) : UserRepository{
        return UserRepositoryImpl(apolloClient)
    }
}