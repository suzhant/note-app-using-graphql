package com.example.tweetapp.utils

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.tweetapp.adapter.PostAdapter
import java.util.Collections


class ItemTouchHelperCallback() : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN // Specify drag directions
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END // Specify swipe directions
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val adapter = recyclerView.adapter as PostAdapter
        val from = source.adapterPosition
        val to = target.adapterPosition

        Collections.swap(adapter.differ.currentList.toMutableList(), from, to)
        adapter.notifyItemMoved(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        when (direction) {
            ItemTouchHelper.END -> {}
            ItemTouchHelper.START -> {

            }
        }
    }
}
