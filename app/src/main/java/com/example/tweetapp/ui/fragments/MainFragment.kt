package com.example.tweetapp.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tweetapp.R
import com.example.tweetapp.adapter.PostAdapter
import com.example.tweetapp.databinding.FragmentMainBinding
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.PostType
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.model.Post
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.PostViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hasura.FetchPostQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainFragment : Fragment(){

    private val binding : FragmentMainBinding by lazy {
        FragmentMainBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private lateinit var adapter : PostAdapter
    private var posts = mutableListOf<FetchPostQuery.Post>()
    private lateinit var progressDialog: Dialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialog = ProgressHelper.buildProgressDialog(requireContext())
        fetchPosts()
        initView()
        initRecycler()
    }

    private fun initView() {
        viewModel.post.observe(viewLifecycleOwner){response ->
            when(response){
                is ApiState.Success -> {
                    binding.progressCircular.visibility = View.GONE
                    posts = response.data as MutableList<FetchPostQuery.Post>
                    adapter.differ.submitList(posts.sortedByDescending { it.id })
                }

                is ApiState.Error -> {
                    binding.progressCircular.visibility = View.GONE
                    Log.d("error", response.message)
                }

                is ApiState.Loading ->{
                    binding.progressCircular.visibility = View.VISIBLE
                }
            }
        }

        binding.fabAdd.setOnClickListener {
            val action = Action.CREATE
            val post = FetchPostQuery.Post(0,"","")
            val postType = PostType(action,post)
            viewModel.setSelectedPost(postType)
            findNavController().navigate(R.id.action_mainFragment_to_formFragment)
        }

    }

    private fun fetchPosts() {
        viewModel.getPost()
    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPost.layoutManager = layoutManager
        adapter = PostAdapter(onPopUpMenuClicked = {post,context,view ->
           showMenu(context,view,post)
        }, onClick = {post ->
            val p = Post(
                id = post.id,
                title = post.title,
                body = post.body
            )
            val action = MainFragmentDirections.actionMainFragmentToDetailFragment(p)
            findNavController().navigate(action)
        })
        binding.recyclerPost.adapter = adapter
    }

    private fun showMenu(context : Context, view: View, post: FetchPostQuery.Post){
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.pop_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(object : android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.update -> {
                        val action = Action.EDIT
                        val postType = PostType(action,post)
                        navigateToForm(postType)
                        true
                    }
                    R.id.delete -> {
                        showDialog(post)
                        true
                    }
                    else -> false
                }
            }

        })
        popupMenu.show()
    }

    private fun showDialog(post: FetchPostQuery.Post) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
        dialog.setTitle("Are you sure?")
        dialog.setPositiveButton("Yes") { _, _ ->
            deletePost(post)
        }
        dialog.setNegativeButton("No") { _, _ ->

        }
        dialog.show()
    }

    private fun deletePost(post: FetchPostQuery.Post){
        progressDialog.show()
        lifecycleScope.launch(Dispatchers.IO){
            viewModel.deletePost(post.id)
            withContext(Dispatchers.Main){
                if (progressDialog.isShowing){
                    progressDialog.dismiss()
                }
                Snackbar.make(binding.root,"Post Deleted Successfully",Snackbar.LENGTH_SHORT).setAction("Dismiss"){

                }.show()
                fetchPosts()
            }
        }
    }

    private fun navigateToForm(post: PostType) {
        viewModel.setSelectedPost(post = post)
        findNavController().navigate(R.id.action_mainFragment_to_formFragment)
    }
}