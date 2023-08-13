package com.example.tweetapp.ui.fragments

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentDetailBinding
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.Post
import com.example.tweetapp.service.RemoteSyncWorker
import com.example.tweetapp.utils.DateTimeUtil
import com.example.tweetapp.viewmodel.PostViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.auth.FirebaseAuth
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit


class DetailFragment : Fragment() {

    private val binding : FragmentDetailBinding by lazy {
        FragmentDetailBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private val detailArgs: DetailFragmentArgs? by navArgs()
    private lateinit var textWatcher : TextWatcher
    private var isUpdated = true
    private lateinit var auth : FirebaseAuth
    private var postId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            duration = resources.getInteger(R.integer.motion_large).toLong()
            interpolator = PathInterpolator(0.05f,0.7f,0.1f,1f)
            scrimColor = Color.TRANSPARENT
        }

//        sharedElementReturnTransition = MaterialContainerTransform().apply {
//            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
//            duration = resources.getInteger(R.integer.motion_large).toLong()
//            shapeMaskProgressThresholds = MaterialContainerTransform.ProgressThresholds(0.5f,1.0f)
//            scaleProgressThresholds =  MaterialContainerTransform.ProgressThresholds(0.5f,1.0f)
//            fadeProgressThresholds =  MaterialContainerTransform.ProgressThresholds(0.1f,1.0f)
//            containerColor  = ContextCompat.getColor(requireContext(),R.color.white)
//            setAllContainerColors(containerColor)
//            scrimColor = Color.TRANSPARENT
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        postId = detailArgs?.post?.id.toString()
        if (postId.isNotEmpty()){
            binding.root.transitionName = detailArgs?.post?.id
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textLength = s?.length ?: 0
                binding.postLength.text = "$textLength characters"
                isUpdated = false
                showMenuItems()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }

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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initView() {

        setEnterSharedElementCallback(object : androidx.core.app.SharedElementCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                binding.etPostTitle.setText(detailArgs?.post?.title)
                binding.postTime.text = detailArgs?.post?.timestamp?.let {
                    DateTimeUtil.convertMillisToDate(
                        it
                    )
                }
                binding.etPostBody.apply {
                    setText(detailArgs?.post?.body)
                    visibility = View.VISIBLE
                    alpha = 0f
                    ViewCompat.animate(this)
                        .alpha(1f)
                        .setDuration(200)
                        .setStartDelay(200)
                        .start()
                }
                val textLength = binding.etPostBody.text.length
                binding.postLength.text = "$textLength characters"
                binding.etPostTitle.addTextChangedListener(textWatcher)
                binding.etPostBody.addTextChangedListener(textWatcher)
            }
        })
        hideMenuItems()
    }

    private fun updatePost(){
        val title = binding.etPostTitle.text.toString()
        val body = binding.etPostBody.text.toString()
        val time = Date().time
        if (postId.isEmpty()){
            val uuid = UUID.randomUUID().toString()
            postId = uuid
        }
        if (isSoftKeyboardVisible()){
            hideKeyboard()
        }
        val note = Post(id = postId,body = body, title = title, timestamp = time, uuid = auth.uid.toString())
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.etPostBody.removeTextChangedListener(textWatcher)
        binding.etPostTitle.removeTextChangedListener(textWatcher)
    }
}