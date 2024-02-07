package com.example.tweetapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.apollographql.apollo3.ApolloClient
import com.codelab.android.datastore.UserPreferences
import com.example.tweetapp.dao.PostDao
import com.example.tweetapp.database.AppDatabase
import com.example.tweetapp.datastore.UserPreferenceSerializer
import com.example.tweetapp.repository.ProtoRepository
import com.example.tweetapp.repository.RemoteRepository
import com.example.tweetapp.repository.RemoteRepositoryImpl
import com.example.tweetapp.repository.RoomRepository
import com.example.tweetapp.repository.UserRepository
import com.example.tweetapp.repository.UserRepositoryImpl
import com.example.tweetapp.service.AlarmController
import com.example.tweetapp.service.AlarmService
import com.example.tweetapp.utils.Constants.DATA_STORE_FILE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    @Singleton
    @Provides
    fun provideProtoDataStore(@ApplicationContext appContext: Context): DataStore<UserPreferences> {
        return DataStoreFactory.create(
            serializer = UserPreferenceSerializer,
            produceFile = { appContext.dataStoreFile(DATA_STORE_FILE_NAME) },
            corruptionHandler =null,
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        )
    }

    @Singleton
    @Provides
    fun provideProtoRepository(protoDataStore: DataStore<UserPreferences>) : ProtoRepository{
        return ProtoRepository(protoDataStore)
    }

    @Provides
    @Singleton
    fun providesAlarmService(@ApplicationContext context: Context) : AlarmController{
        return AlarmService(context)
    }

}