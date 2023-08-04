package com.example.tweetapp.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.apollographql.apollo3.api.Optional
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentDetailBinding
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.PostViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hasura.type.Post_pk_columns_input
import com.hasura.type.Post_set_input
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DetailFragment : Fragment() {

    private val binding : FragmentDetailBinding by lazy {
        FragmentDetailBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private val detailArgs: DetailFragmentArgs by navArgs()
    private lateinit var textWatcher : TextWatcher
    private lateinit var progressbar: Dialog
    private var isUpdated = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        progressbar = ProgressHelper.buildProgressDialog(requireContext())
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
        val id = Post_pk_columns_input(detailArgs.post.id)
        val input = Post_set_input(title = Optional.present(title), body = Optional.present(body))
        progressbar.show()
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.updatePost(id,input)
            withContext(Dispatchers.Main){
                isUpdated = true
                hideMenuItems()
                Snackbar.make(binding.root,"Updated",Snackbar.LENGTH_SHORT).setAction("Dismiss"){

                }.show()
                progressbar.dismiss()
            }
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
}