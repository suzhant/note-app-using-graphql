package com.example.tweetapp.ui
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tweetapp.databinding.ActivityMainBinding
import com.example.tweetapp.datastore.SettingPref
import com.example.tweetapp.service.ConnectivityReceiver
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.service.RoomSyncWorker
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val userViewModel : UserViewModel by viewModels()
    private var isFirstTime = true
    private lateinit var auth : FirebaseAuth
    private lateinit var wifiReceiver: BroadcastReceiver

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        initReceiver()
        userViewModel.login.observe(this){isLoggedIn ->
            lifecycleScope.launch {
                if (isLoggedIn){
                    scheduleDataSyncWorker()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun scheduleDataSyncWorker() {
        Log.d("remoteWorker","sync")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRemoteRequest = OneTimeWorkRequestBuilder<RemoteSyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                5000L,
                TimeUnit.MILLISECONDS
            )
            .setInputData(inputData = workDataOf(RemoteSyncWorker.ACTION to "SYNC"))
            .setConstraints(constraints)
            .build()

        val syncRoomRequest = OneTimeWorkRequestBuilder<RoomSyncWorker>().setBackoffCriteria(
            BackoffPolicy.LINEAR,
            5000L,
            TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()


        val key = booleanPreferencesKey(auth.uid.toString())
        isFirstTime = SettingPref(this@MainActivity,key).getUserFirstTime.first()

        val firstTask = if (isFirstTime) syncRoomRequest else syncRemoteRequest
        val secondTask = if (isFirstTime) syncRemoteRequest else syncRoomRequest

        WorkManager.getInstance(applicationContext)
            .beginWith(firstTask)
            .then(secondTask).enqueue()

    }

    private fun initReceiver() {
        wifiReceiver = ConnectivityReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(wifiReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiReceiver)
    }

}