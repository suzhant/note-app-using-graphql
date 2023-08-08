package com.example.tweetapp.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.apollographql.apollo3.api.Optional
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentFormBinding
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.Post
import com.example.tweetapp.model.PostType
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.PostViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.hasura.type.Note_pk_columns_input
import com.hasura.type.Note_set_input
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class FormFragment : Fragment() {


    private val binding : FragmentFormBinding by lazy {
        FragmentFormBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private lateinit var postType : PostType
    private lateinit var auth : FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        viewModel.selectedPost.observe(viewLifecycleOwner){postType ->
            this.postType = postType
            val post = postType.post
            binding.etTitle.setText(post.title)
            binding.etBody.setText(post.body)

            when (postType.type){
                Action.CREATE -> {
                    binding.btnAction.text = "Create Post"
                }

                Action.EDIT -> {
                    binding.btnAction.text = "Update Post"
                }
                else -> {}
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAction.setOnClickListener {
            when(postType.type){
                Action.CREATE ->{
                        hideKeyboard(binding.root)
                        binding.etBody.clearFocus()
                        binding.etTitle.clearFocus()
                        createPost()
                }

                Action.EDIT -> {
                    hideKeyboard(binding.root)
                    binding.etBody.clearFocus()
                    binding.etTitle.clearFocus()
                    updatePost()
                }
                else -> {}
            }
        }
    }

    private fun updatePost() {
        val title = binding.etTitle.text.toString()
        val body = binding.etBody.text.toString()
        if (title.isEmpty() || body.isEmpty()){
            Snackbar.make(binding.root,"Please fill all the fields", Snackbar.LENGTH_SHORT).setAction("Dismiss"
            ){

            }.show()
            return
        }
        val time = Date().time
        val note = Post(id = postType.post.id,body = body, title = title, timestamp = time, uuid = auth.uid.toString())
        viewModel.upsertNote(note).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduleRemoteSyncWorker(Action.EDIT.name)
            }
            Snackbar.make(binding.root,"Post Updated Successfully", Snackbar.LENGTH_SHORT).setAction("Dismiss"
            ){}.show()
            findNavController().navigate(R.id.action_formFragment_to_mainFragment)
        }
    }

    private fun createPost() {
        val title = binding.etTitle.text.toString()
        val body = binding.etBody.text.toString()
        if (title.isEmpty() || body.isEmpty()){
            Snackbar.make(binding.root,"Please fill all the fields", Snackbar.LENGTH_SHORT).setAction("Dismiss"
            ){

            }.show()
            return
        }
        val uuid = UUID.randomUUID()
        val time = Date().time
        val post = Post(
            id = uuid.toString(),
            title = title,
            body = body,
            timestamp = time,
            uuid = auth.uid.toString()
        )
        viewModel.upsertNote(post).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduleRemoteSyncWorker(Action.CREATE.name)
            }
            Snackbar.make(binding.root,"Post Created Successfully", Snackbar.LENGTH_SHORT).setAction("Dismiss"
            ){}.show()
            findNavController().navigate(R.id.action_formFragment_to_mainFragment)
        }
    }


    private fun hideKeyboard(view: View) {
        val imm: InputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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