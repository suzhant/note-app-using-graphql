package com.example.tweetapp.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.asLiveData
import com.codelab.android.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import java.io.IOException
import javax.inject.Inject


class ProtoRepository @Inject constructor(private val userPreferencesStore: DataStore<UserPreferences>) {

    private val TAG = "UserPrefRepo"

    val userPreferenceFLow : Flow<UserPreferences> = userPreferencesStore.data
        .catch {exception ->
            if (exception is IOException){
                Log.d(TAG,"Error reading data")
                emit(UserPreferences.getDefaultInstance())
            }else{
                throw exception
            }
        }

    suspend fun updateValue(isFirstTime : Boolean,userId: String){
        userPreferencesStore.updateData {preference ->
            preference.toBuilder().setFirstTime(isFirstTime).build()
            preference.toBuilder().setUserId(userId).build()
        }
    }

    fun getKeyByUserId(userId: String) = userPreferencesStore.data.filter {
             it.userId == userId
    }.asLiveData()

}