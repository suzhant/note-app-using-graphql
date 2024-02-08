package com.example.tweetapp.ui.calendar.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentCalendarBinding
import com.example.tweetapp.model.AlarmItem
import com.example.tweetapp.ui.calendar.SharedViewModel
import com.example.tweetapp.utils.DateTimeUtil
import com.example.tweetapp.utils.setSize
import java.util.Calendar
import java.util.Date

class CalendarFragment : DialogFragment() {


    private val binding: FragmentCalendarBinding by lazy {
        FragmentCalendarBinding.inflate(layoutInflater)
    }
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val args: CalendarFragmentArgs? by navArgs()
    private var date : Long ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args?.note?.let {
            sharedViewModel.setCurrentPost(it)
        }
        sharedViewModel.setTime(null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year,month,day)
            date = cal.timeInMillis
            Log.d("calendarDate","$year $month $day $date")
        }

        binding.btnDone.setOnClickListener {
            val time = sharedViewModel.time.value
            if (date == null){
                date = binding.calendarView.date
            }
            if (time != null) {
                val combinedTimeInMillis = combine(date!!, time)
                val post = sharedViewModel.currentPost.value
                Log.d("calendarDate", post.toString())
                Log.d("calendarDate", combinedTimeInMillis.toString())
                post?.let { note ->
                    val alarmItem = AlarmItem(
                        triggerTimeInMillis = combinedTimeInMillis,
                        message = note.body,
                        title = note.title,
                        postId = note.id,
                        timeStamp = note.timestamp
                    )
                    sharedViewModel.scheduleAlarm(alarmItem)
                }
                dismiss()
            }

        }

        binding.linearTime.setOnClickListener {
            findNavController().navigate(R.id.action_calendarFragment_to_timeFragment)
        }

        binding.linearReminder.setOnClickListener {
            findNavController().navigate(R.id.action_calendarFragment_to_reminderFragment)
        }

        binding.linearRepeat.setOnClickListener {

        }

        sharedViewModel.time.observe(viewLifecycleOwner) { time ->
            if (time != null){
                binding.tvTime.text = DateTimeUtil.convertMillisToTime(time)
            }else{
                binding.tvTime.text = "No"
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismiss()
                }
            })
    }

    private fun combine(date: Long, time: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = time
            val hour = get(Calendar.HOUR_OF_DAY)
            val min = get(Calendar.MINUTE)

            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val now = Calendar.getInstance()
            if (this.before(now)){
                this.add(Calendar.DAY_OF_MONTH,1)
            }
        }

        return cal.timeInMillis
    }



    override fun onStart() {
        super.onStart()
        setSize(widthPercentage = 85)
    }
}