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
import com.example.tweetapp.databinding.FragmentTimeBinding
import com.example.tweetapp.ui.calendar.SharedViewModel
import com.example.tweetapp.utils.setSize
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes

class TimeFragment : DialogFragment() {

    private val binding : FragmentTimeBinding by lazy {
        FragmentTimeBinding.inflate(layoutInflater)
    }
    private val sharedViewModel : SharedViewModel by activityViewModels()
    private var selectedTimeInMillis :Long ?= null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismiss()
                }
            })

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

//        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
//            val time = sharedViewModel.getCurrentTime(hourOfDay,minute)
//            selectedTimeInMillis = time
//        }
        binding.timePicker.setIs24HourView(true)

        binding.btnDone.setOnClickListener {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val time = sharedViewModel.getCurrentTime(hour,minute)
            Log.d("calendarData",time.toString())
            sharedViewModel.setTime(time)
            dismiss()
        }

    }



    override fun onStart() {
        super.onStart()
        setSize(widthPercentage = 85)
    }

}