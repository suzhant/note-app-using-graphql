package com.example.tweetapp.ui.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.doOnPreDraw
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
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
import com.example.tweetapp.datastore.SettingPref
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.PostType
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.viewmodel.PostViewModel
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainFragment : Fragment(){

    private val binding : FragmentMainBinding by lazy {
        FragmentMainBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private val userViewModel : UserViewModel by activityViewModels()
    private var adapter : PostAdapter ?= null
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
               if (posts.isNotEmpty()){
                   binding.progressCircular.visibility = View.GONE
               }
               adapter?.differ?.submitList(posts.sortedByDescending { it.timestamp})
               val key = booleanPreferencesKey(auth.uid.toString())
               SettingPref(requireContext(),key).setUserFirstTime(false)
           }
       }

        binding.fabAdd.setOnClickListener {
            val action = Action.CREATE
            val post = Post("","","", timestamp = 0L, uuid = "")
            val postType = PostType(action,post)
            viewModel.setSelectedPost(postType)
            val arg = MainFragmentDirections.actionMainFragmentToDetailFragment(post)
            val extras = FragmentNavigatorExtras(binding.fabAdd to "shared_element_container")
            findNavController().navigate(arg,extras)
        }

        binding.toolbar.menu.findItem(R.id.setting).setOnMenuItemClickListener {
             findNavController().navigate(R.id.action_mainFragment_to_settingFragment)
            false
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                showLogoutDialog()
            }
        })
    }

    private fun showLogoutDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
        dialog.setTitle("Do you want to logout?")
        dialog.setPositiveButton("Yes"){_, _ ->
            auth.signOut()
            if (auth.currentUser==null){
                userViewModel.setLogin(false)
                findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
            }
        }
        dialog.setNegativeButton("No"){_, _ ->

        }
        dialog.show()
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
        }, onClick = {post,view ->
            val p = Post(
                id = post.id,
                title = post.title,
                body = post.body,
                timestamp = post.timestamp,
                uuid = post.uuid
            )
            val action = MainFragmentDirections.actionMainFragmentToDetailFragment(p)
            val extras = FragmentNavigatorExtras(view to post.id)
            findNavController().navigate(action,extras)
        })
        binding.recyclerPost.adapter = adapter

        postponeEnterTransition()
        binding.recyclerPost.doOnPreDraw {
            if (binding.recyclerPost.isLaidOut){
                startPostponedEnterTransition()
            }
        }
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