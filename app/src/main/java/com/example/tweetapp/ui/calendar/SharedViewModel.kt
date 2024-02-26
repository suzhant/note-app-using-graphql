package com.example.tweetapp.ui.calendar

import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.tweetapp.model.AlarmItem
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.ReminderItem
import com.example.tweetapp.model.enums.ReminderDates
import com.example.tweetapp.service.AlarmController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
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
        ReminderItem("1", reminderDates = ReminderDates.SAME_WITH_DUE_DATE),
        ReminderItem("2", reminderDates = ReminderDates._5_MIN_BEFORE),
        ReminderItem("3", reminderDates = ReminderDates._10_MIN_BEFORE),
        ReminderItem("4", reminderDates = ReminderDates._1_DAY),
        ReminderItem("5", reminderDates = ReminderDates._2_DAY),
    )

    private val _reminderState =
        MutableStateFlow((ReminderUiState(items = reminders, checkedItems = reminders)))
    val reminderState = _reminderState.asStateFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2000), _reminderState.value
    )

    private val _time = MutableLiveData<Long?>(null)
    val time: LiveData<Long?> = _time

    private val _currentPost = MutableLiveData<Post>(null)
    val currentPost: LiveData<Post> = _currentPost

    private val _reminderDate = MutableStateFlow(reminders)
    val reminderDate : StateFlow<List<ReminderItem>> = _reminderDate.asStateFlow()

    init {
        combine(_reminderDate,reminderState) { _, state ->
            state.items.filter { it.checked }
        }
    }


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
            _reminderState.value.items.toMutableList().also { items ->
                val indexToAdd = items.indexOfFirst { it.id == item.id }
                list[indexToAdd] = item
                if (item.reminderDates.toString() == "Same with due date") {
                    items[0] = item
                    _reminderState.update {
                        it.copy(
                            items = items
                        )
                    }
                }
            }
            _reminderState.update {
                it.copy(
                    checkedItems = list
                )
            }
            Log.d("calendarData", "updated Item :" + list.toString())
        }
    }

    fun resetList() {
        val newList = _reminderState.value.items.map { item ->
            item.copy(checked = false)
        }
        _reminderState.update {
            it.copy(
                items = newList,
                checkedItems = newList
            )
        }
    }

    fun updateReminderItems() {
        val checkedList = _reminderState.value.checkedItems
        val newList = _reminderState.value.items.map { item1 ->
            checkedList.find { it.id == item1.id }?.let {
                item1.copy(checked = it.checked)
            } ?: item1
        }
        _reminderState.update { currentState ->
            currentState.copy(
                items = newList
            )
        }
    }


    fun setFirstItemChecked() {
        val checkedItems = _reminderState.value.checkedItems.filter { it.checked }
        if (checkedItems.isNotEmpty() && checkedItems[0].checked) return

        _reminderState.value.items.toMutableList().also { list ->
            val firstItem = list[0].copy(checked = true)
            val newList = list.toMutableList().also { items ->
                items[0] = firstItem
            }
            _reminderState.update { currentState ->
                currentState.copy(
                    checkedItems = newList,
                    items = newList
                )
            }
        }
        Log.d("calendarData", "checked :${_reminderState.value.checkedItems}")
        Log.d("calendarData", "reminderItem :${_reminderState.value.items}")
    }

    fun getCheckedReminders(): List<ReminderItem> {
        return reminderState.value.checkedItems.filter { it.checked }
    }

    fun scheduleAlarm(alarmItem: AlarmItem) {
        alarmController.scheduleAlarm(alarmItem)
    }

    fun cancelAlarm(alarmItem: AlarmItem) {
        alarmController.cancelAlarm(alarmItem)
    }
}