package com.example.tweetapp.ui.main.fragments

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tweetapp.R
import com.example.tweetapp.ui.main.adapter.PostAdapter
import com.example.tweetapp.databinding.FragmentMainBinding
import com.example.tweetapp.datastore.SettingPref
import com.example.tweetapp.model.enums.Action
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.enums.PostType
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.ui.auth.LoginActivity
import com.example.tweetapp.utils.Constants
import com.example.tweetapp.utils.ItemTouchHelperCallback
import com.example.tweetapp.utils.navigateSafe
import com.example.tweetapp.viewmodel.PostViewModel
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit


class MainFragment : Fragment() {

    private val binding: FragmentMainBinding by lazy {
        FragmentMainBinding.inflate(layoutInflater)
    }
    private val viewModel: PostViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private var adapter: PostAdapter? = null
    private lateinit var auth: FirebaseAuth
    private var connection = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRecycler()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        checkPermission()
        initView()
    }

    private fun initView() {
        userViewModel.workState.observe(viewLifecycleOwner) { workState ->
            workState?.let {
                if (workState) {
                    fetchNotes()
                } else {
                    if (!connection) {
                        fetchNotes()
                    }
                }
            }
        }

        val key = booleanPreferencesKey(Constants.NETWORK_STATE)
        SettingPref(requireContext(), key).getNetworkState.asLiveData()
            .observe(viewLifecycleOwner) {
                connection = it
            }

        binding.fabAdd.setOnClickListener {
            val action = Action.CREATE
            val post = Post("", "", "", timestamp = Date().time, uuid = "")
            val postType = PostType(action, post)
            viewModel.setSelectedPost(postType)
            val arg = MainFragmentDirections.actionMainFragmentToDetailFragment(post = post)
            val extras = FragmentNavigatorExtras(binding.fabAdd to "shared_element_container")
            findNavController().navigate(arg, extras)
        }

        binding.toolbar.menu.findItem(R.id.setting).setOnMenuItemClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingFragment)
            false
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showLogoutDialog()
                }
            })
    }

    private fun showLogoutDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
        dialog.setTitle("Do you want to logout?")
        dialog.setPositiveButton("Yes") { _, _ ->
            if (connection) {
                auth.signOut()
                if (auth.currentUser == null) {
                    userViewModel.setLogin(false)
                    startActivity(Intent(requireContext(),LoginActivity::class.java))
                }
            } else {
                Toast.makeText(requireContext(), "No connection", Toast.LENGTH_SHORT).show()
            }

        }
        dialog.setNegativeButton("No") { _, _ ->

        }
        dialog.show()
    }

    private fun fetchNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getNotesByUserId(auth.uid.toString()).collectLatest { posts ->
                posts.let {
                    binding.progressCircular.visibility = View.GONE
                    adapter?.differ?.submitList(posts.sortedByDescending { it.timestamp })
                    val key = booleanPreferencesKey(auth.uid.toString())
                    SettingPref(requireContext(), key).setUserFirstTime(false)
                }
            }
        }
    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPost.layoutManager = layoutManager
        adapter = PostAdapter(onPopUpMenuClicked = { post, context, view ->
            showMenu(
                context, view, post
            )
        }, onClick = { post, view ->
            val action = MainFragmentDirections.actionMainFragmentToDetailFragment(post = post)
            val extras = FragmentNavigatorExtras(view to post.id)
            with(findNavController()) {
                currentDestination?.getAction(R.id.action_mainFragment_to_detailFragment)?.let {
                    navigate(action, extras)
                }
            }

        })
        binding.recyclerPost.adapter = adapter
        val callback = ItemTouchHelperCallback()
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerPost)

        postponeEnterTransition()
        binding.recyclerPost.doOnPreDraw {
            if (binding.recyclerPost.isLaidOut) {
                startPostponedEnterTransition()
            }
        }
    }

    private fun showMenu(context: Context, view: View, post: Post) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.pop_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(object :
            android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.delete -> {
                        showDialog(post)
                        true
                    }

                    R.id.schedule -> {
                        val arg = MainFragmentDirections.actionMainFragmentToCalendarFragment(note = post)
                        navigateSafe(arg)
                        true
                    }

                    else -> false
                }
            }

        })
        popupMenu.show()
    }


    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationPermission) {
                requestNotificationPermissionLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                return
            }
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                requireContext(),
                "Please grant permission to show notification",
                Toast.LENGTH_SHORT
            ).show()
        }
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


    private fun deletePost(post: Post) {
        viewModel.deleteNote(post).also {
            Snackbar.make(binding.root, "Post Deleted Successfully", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduleRemoteSyncWorker(Action.DELETE.name)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleRemoteSyncWorker(action: String) {
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