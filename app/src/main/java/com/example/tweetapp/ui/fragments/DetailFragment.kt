package com.example.tweetapp.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.apollographql.apollo3.api.Optional
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentDetailBinding
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.Post
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.PostViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.hasura.type.Note_pk_columns_input
import com.hasura.type.Note_set_input
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit


class DetailFragment : Fragment() {

    private val binding : FragmentDetailBinding by lazy {
        FragmentDetailBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private val detailArgs: DetailFragmentArgs by navArgs()
    private lateinit var textWatcher : TextWatcher
    private var isUpdated = true
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
        initView()
        initListeners()
    }

    private fun initListeners() {
        binding.toolbar.setNavigationOnClickListener {
            checkIfPostIsUpdated()
        }

        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isUpdated = false
                showMenuItems()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
        binding.postTitleTextView.addTextChangedListener(textWatcher)
        binding.postBodyTextView.addTextChangedListener(textWatcher)

        binding.toolbar.menu.findItem(R.id.done).setOnMenuItemClickListener {
            updatePost()
            false
        }

        binding.toolbar.menu.findItem(R.id.cancel).setOnMenuItemClickListener {
            if (isSoftKeyboardVisible()){
                hideKeyboard()
            }
            hideMenuItems()
            false
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                checkIfPostIsUpdated()
            }
        })
    }

    private fun checkIfPostIsUpdated() {
        if (!isUpdated){
            val dialog = MaterialAlertDialogBuilder(requireContext())
            dialog.setTitle("You have not saved the changes. Do you want to save your work?")
            dialog.setPositiveButton("Yes"){ _, _ ->
                updatePost()
            }
            dialog.setNegativeButton("No"){_ , _ ->
                findNavController().navigateUp()
            }
            dialog.show()
        }else{
            findNavController().navigateUp()
        }
    }
    private fun initView() {
        binding.postTitleTextView.setText(detailArgs.post.title)
        binding.postBodyTextView.setText(detailArgs.post.body)
        hideMenuItems()
    }

    private fun updatePost(){
        val title = binding.postTitleTextView.text.toString()
        val body = binding.postBodyTextView.text.toString()
        if (isSoftKeyboardVisible()){
            hideKeyboard()
        }
        val time = Date().time
        val note = Post(id = detailArgs.post.id,body = body, title = title, timestamp = time, uuid = auth.uid.toString())
        viewModel.upsertNote(note).also {
            isUpdated = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduleRemoteSyncWorker(Action.EDIT.name)
            }
            hideMenuItems()
            Snackbar.make(binding.root,"Updated",Snackbar.LENGTH_SHORT).setAction("Dismiss"){

            }.show()
        }
    }

    private fun showMenuItems(){
        binding.toolbar.menu.findItem(R.id.done).isVisible = true
        binding.toolbar.menu.findItem(R.id.cancel).isVisible = true
    }

    private fun hideMenuItems(){
        binding.toolbar.menu.findItem(R.id.done).isVisible = false
        binding.toolbar.menu.findItem(R.id.cancel).isVisible = false
    }

    private fun isSoftKeyboardVisible(): Boolean {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputMethodManager.isAcceptingText
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
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