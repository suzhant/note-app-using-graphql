package com.example.tweetapp.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.tweetapp.R
import com.example.tweetapp.databinding.PostItemLayoutBinding
import com.example.tweetapp.model.Post
import com.example.tweetapp.ui.fragments.MainFragmentDirections
import com.hasura.FetchPostQuery

class PostAdapter(
    private val onPopUpMenuClicked: (FetchPostQuery.Post, Context, View) -> Unit,
    private val onClick: (FetchPostQuery.Post) -> Unit,
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private lateinit var context: Context

    inner class PostViewHolder(val binding: PostItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        context = parent.context
        val itemView = PostItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = differ.currentList[position]
        with(holder.binding){
            postTitleTextView.text = post.title
            postBodyTextView.text = post.body
            menu.setOnClickListener {
                onPopUpMenuClicked(
                    post,
                    it.context,
                    it
                )
            }
            card.setOnClickListener {
                onClick(post)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private val differCallback = object : DiffUtil.ItemCallback<FetchPostQuery.Post>(){
        override fun areItemsTheSame(oldItem: FetchPostQuery.Post, newItem: FetchPostQuery.Post): Boolean {
            return  oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FetchPostQuery.Post, newItem: FetchPostQuery.Post): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this,differCallback)

}
