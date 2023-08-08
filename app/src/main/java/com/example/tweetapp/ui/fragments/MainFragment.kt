package com.example.tweetapp.ui.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tweetapp.R
import com.example.tweetapp.adapter.PostAdapter
import com.example.tweetapp.databinding.FragmentMainBinding
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.PostType
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.viewmodel.PostViewModel
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.hasura.FetchNoteQuery
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainFragment : Fragment(){

    private val binding : FragmentMainBinding by lazy {
        FragmentMainBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private val userViewModel : UserViewModel by activityViewModels()
    private lateinit var adapter : PostAdapter
    private var posts = mutableListOf<FetchNoteQuery.Note>()
    private lateinit var auth : FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        initRecycler()
        initView()
    }

    private fun initView() {
       lifecycleScope.launch {
           viewModel.getNotesByUserId(auth.uid.toString()).collectLatest{posts ->
               adapter.differ.submitList(posts.sortedByDescending { it.timestamp})
           }
       }

        binding.fabAdd.setOnClickListener {
            val action = Action.CREATE
            val post = Post("","","", timestamp = 0L, uuid = "")
            val postType = PostType(action,post)
            viewModel.setSelectedPost(postType)
            findNavController().navigate(R.id.action_mainFragment_to_formFragment)
        }

        binding.toolbar.menu.findItem(R.id.setting).setOnMenuItemClickListener {
             findNavController().navigate(R.id.action_mainFragment_to_settingFragment)
            false
        }

    }

   private val postObserver = Observer<ApiState<List<FetchNoteQuery.Note>>>{response ->
        when(response){
            is ApiState.Success -> {
                binding.progressCircular.visibility = View.GONE
                posts = response.data as MutableList<FetchNoteQuery.Note>
                val postEntities = posts.map {
                    Post(
                        id = it.id,
                        title = it.title,
                        body = it.body,
                        timestamp = it.timestamp.toString().toLong(),
                        uuid = it.uuid
                    )
                }
                adapter.differ.submitList(postEntities.sortedByDescending { it.timestamp })
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

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPost.layoutManager = layoutManager
        adapter = PostAdapter(onPopUpMenuClicked = {post,context,view ->
           showMenu(context,view,Post(
               id = post.id,
               title = post.title,
               body = post.body,
               timestamp = post.timestamp,
               uuid = post.uuid
           ))
        }, onClick = {post ->
            val p = Post(
                id = post.id,
                title = post.title,
                body = post.body,
                timestamp = post.timestamp,
                uuid = post.uuid
            )
            val action = MainFragmentDirections.actionMainFragmentToDetailFragment(p)
            findNavController().navigate(action)
        })
        binding.recyclerPost.adapter = adapter
    }

    private fun showMenu(context : Context, view: View, post: Post){
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


    private fun showDialog(post: Post) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
        dialog.setTitle("Are you sure?")
        dialog.setPositiveButton("Yes") { _, _ ->
            deletePost(post)
        }
        dialog.setNegativeButton("No") { _, _ ->

        }
        dialog.show()
    }


    private fun deletePost(post: Post){
        viewModel.deleteNote(post).also {
            Snackbar.make(binding.root,"Post Deleted Successfully",Snackbar.LENGTH_SHORT).setAction("Dismiss"){}.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduleRemoteSyncWorker(Action.DELETE.name)
            }
        }

    }

    private fun navigateToForm(post: PostType) {
        viewModel.setSelectedPost(post = post)
        findNavController().navigate(R.id.action_mainFragment_to_formFragment)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleRemoteSyncWorker(action : String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncDataRequest = OneTimeWorkRequestBuilder<RemoteSyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                5000L,
                TimeUnit.MILLISECONDS
            ).addTag("remoteData")
            .setConstraints(constraints)
            .setInputData(inputData = workDataOf(RemoteSyncWorker.ACTION to action))
            .build()

        WorkManager.getInstance(requireContext())
            .enqueueUniqueWork(
                syncDataRequest.stringId,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncDataRequest
            )
    }
}