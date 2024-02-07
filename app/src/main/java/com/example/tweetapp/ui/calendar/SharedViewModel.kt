package com.example.tweetapp.ui.calendar

import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tweetapp.model.AlarmItem
import com.example.tweetapp.model.Post
import com.example.tweetapp.service.AlarmController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val alarmController: AlarmController
) : ViewModel() {

    private val _time = MutableLiveData<Long?>(null)
    val time: LiveData<Long?> = _time

    private val _currentPost = MutableLiveData<Post>(null)
    val currentPost : LiveData<Post> = _currentPost

    fun setCurrentPost(post: Post){
        _currentPost.value = post
    }

    fun getCurrentTime(hour : Int, minute : Int) : Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun setTime(time: Long?) {
        _time.value = time
    }


    fun scheduleAlarm(alarmItem: AlarmItem) {
        alarmController.scheduleAlarm(alarmItem)
    }

    fun cancelAlarm(alarmItem: AlarmItem){
        alarmController.cancelAlarm(alarmItem)
    }
}