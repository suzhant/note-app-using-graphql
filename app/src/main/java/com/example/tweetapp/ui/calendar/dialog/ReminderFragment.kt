package com.example.tweetapp.ui.calendar.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.PopupWindow
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tweetapp.databinding.FragmentReminderBinding
import com.example.tweetapp.databinding.WidgetPopupReminderBinding
import com.example.tweetapp.ui.calendar.SharedViewModel
import com.example.tweetapp.ui.calendar.adapter.ReminderAdapter
import com.example.tweetapp.utils.setSize

class ReminderFragment : DialogFragment() {

    private val binding: FragmentReminderBinding by lazy {
        FragmentReminderBinding.inflate(layoutInflater)
    }
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var reminderAdapter: ReminderAdapter? = null
    private lateinit var popupWindow: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPopUpWindow()
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

        binding.btnReminderAt.setOnClickListener {
            popupWindow.showAsDropDown(it)
        }

        binding.switchReminder.apply {
            isChecked = true
            sharedViewModel.setFirstItemChecked()
        }

        binding.switchReminder.setOnCheckedChangeListener { compoundButton, b ->
            binding.btnReminderAt.isEnabled = b
            if (b) {
                sharedViewModel.setFirstItemChecked()
            }
        }

        sharedViewModel.reminderState.asLiveData().observe(viewLifecycleOwner) { state ->
            state.let {
                reminderAdapter?.differ?.submitList(state.items)
                binding.btnReminderAt.text =
                    state.checkedItems.joinToString(", ") { it.name }.ifEmpty {
                        "No"
                    }
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnDone.setOnClickListener { dismiss() }

    }

    private fun initPopUpWindow() {
        val popupView = WidgetPopupReminderBinding.inflate(layoutInflater)
        initRecycler(popupView.recyclerPopUp)
        popupWindow = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 10f
        }

        popupWindow.setOnDismissListener {
            val checkedItem = sharedViewModel.getCheckedReminders()
            Log.d("calendarData", "dismiss checked: " + checkedItem.toString())
            if (checkedItem.isEmpty()) {
                binding.switchReminder.isChecked = false
            }
        }
    }

    private fun initRecycler(recyclerPopUp: RecyclerView) {
        recyclerPopUp.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = ReminderAdapter(onChecked = {
                sharedViewModel.updateItems(it)
            }).also {
                reminderAdapter = it
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setSize(widthPercentage = 85)
    }

}