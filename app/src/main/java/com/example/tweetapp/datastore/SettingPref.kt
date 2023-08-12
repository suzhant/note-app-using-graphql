package com.example.tweetapp.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingPref(private val context: Context, private val key : Preferences.Key<Boolean>){

    companion object{
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    }

     suspend fun setUserFirstTime(firstTime : Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = firstTime
        }
    }

    suspend fun setNetworkState(network : Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = network
        }
    }

    val getUserFirstTime : Flow<Boolean> =  context.dataStore.data
        .map { preferences ->
            preferences[key] ?: true
        }

    val getNetworkState : Flow<Boolean> =  context.dataStore.data
        .map { preferences ->
            preferences[key] ?: false
        }
}
