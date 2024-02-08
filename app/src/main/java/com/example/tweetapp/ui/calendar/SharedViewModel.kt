package com.example.tweetapp.ui.calendar

import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tweetapp.model.AlarmItem
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.ReminderItem
import com.example.tweetapp.service.AlarmController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val alarmController: AlarmController
) : ViewModel() {
    private val reminders = listOf(
        ReminderItem("1", "Same with due date"),
        ReminderItem("2", "5 minutes before"),
        ReminderItem("3", "10 minutes before"),
        ReminderItem("4", "1 days before"),
        ReminderItem("5", "2 days before"),
    )

    private val _reminderState = MutableStateFlow((ReminderUiState(items = reminders)))
    val reminderState = _reminderState.asStateFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2000), _reminderState.value
    )

    private val _time = MutableLiveData<Long?>(null)
    val time: LiveData<Long?> = _time

    private val _currentPost = MutableLiveData<Post>(null)
    val currentPost: LiveData<Post> = _currentPost


    fun setCurrentPost(post: Post) {
        _currentPost.value = post
    }

    fun getCurrentTime(hour: Int, minute: Int): Long {
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
    fun updateItems(item: ReminderItem) {
        _reminderState.value.checkedItems.toMutableList().also { list ->
            val index = list.indexOfFirst { it.id == item.id }
            if (item.name == "Same with due date") {
                _reminderState.value.items.toMutableList().also { items ->
                    items[0] = item
                    _reminderState.update {
                        it.copy(
                            items = items
                        )
                    }
                }
            }
            if (item.checked){
                list.add(item)
            }else{
                list.removeAt(index)
            }

            _reminderState.update {
                it.copy(
                    checkedItems = list
                )
            }
            Log.d("calendarData", "updated Item :" + list.toString())
        }
    }



    fun setFirstItemChecked() {
        val checkedItems = _reminderState.value.checkedItems.filter { it.checked }
        if (checkedItems.isNotEmpty()) return

        _reminderState.update { currentState ->
            val firstItem = currentState.items[0].copy(checked = true)
            val newList = currentState.items.toMutableList().apply { set(0, firstItem) }

            currentState.copy(
                checkedItems = listOf(firstItem),
                items = newList
            )
        }

        Log.d("calendarData", "checked :${_reminderState.value.checkedItems}")
    }

    fun getCheckedReminders(): List<ReminderItem> {
        return reminderState.value.checkedItems
    }

    fun scheduleAlarm(alarmItem: AlarmItem) {
        alarmController.scheduleAlarm(alarmItem)
    }

    fun cancelAlarm(alarmItem: AlarmItem) {
        alarmController.cancelAlarm(alarmItem)
    }
}