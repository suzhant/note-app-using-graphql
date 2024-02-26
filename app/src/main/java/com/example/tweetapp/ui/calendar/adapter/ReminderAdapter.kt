package com.example.tweetapp.ui.calendar.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.tweetapp.databinding.MenuItemWithCheckboxBinding
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.ReminderItem

class ReminderAdapter(val onChecked : (ReminderItem) -> Unit) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private lateinit var context: Context
    inner class ReminderViewHolder(val binding : MenuItemWithCheckboxBinding) : ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        context = parent.context
        val view = MenuItemWithCheckboxBinding.inflate(LayoutInflater.from(context),parent,false)
        return ReminderViewHolder(view)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val item = differ.currentList[position]
        with(holder.binding){
            textView.text = item.reminderDates.toString()
            checkBox.isChecked = item.checked

            checkBox.setOnClickListener {
                val checked = checkBox.isChecked
                val newItem = item.copy(checked = checked)
                Log.d("calendarData",newItem.toString())
                onChecked(newItem)
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<ReminderItem>(){
        override fun areItemsTheSame(oldItem: ReminderItem, newItem: ReminderItem): Boolean {
            return  oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReminderItem, newItem: ReminderItem): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this,differCallback)
}