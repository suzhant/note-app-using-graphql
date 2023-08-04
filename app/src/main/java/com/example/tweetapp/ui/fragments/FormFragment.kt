package com.example.tweetapp.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.api.Optional
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentFormBinding
import com.example.tweetapp.model.Action
import com.example.tweetapp.model.PostType
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.PostViewModel
import com.google.android.material.snackbar.Snackbar
import com.hasura.type.Post_pk_columns_input
import com.hasura.type.Post_set_input
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FormFragment : Fragment() {


    private val binding : FragmentFormBinding by lazy {
        FragmentFormBinding.inflate(layoutInflater)
    }
    private val viewModel : PostViewModel by activityViewModels()
    private lateinit var postType : PostType
    private lateinit var progressDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = ProgressHelper.buildProgressDialog(requireContext())

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
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAction.setOnClickListener {
            when(postType.type){
                Action.CREATE ->{
                        progressDialog.show()
                        hideKeyboard(binding.root)
                        binding.etBody.clearFocus()
                        binding.etTitle.clearFocus()
                        createPost()
                }

                Action.EDIT -> {
                    progressDialog.show()
                    hideKeyboard(binding.root)
                    binding.etBody.clearFocus()
                    binding.etTitle.clearFocus()
                    updatePost()
                }
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
            progressDialog.dismiss()
            return
        }

        val id = Post_pk_columns_input(postType.post.id)
        val input = Post_set_input(body = Optional.present(body),id = Optional.present(postType.post.id), title = Optional.present(title))

        lifecycleScope.launch(Dispatchers.IO){
            viewModel.updatePost(id,input)
            withContext(Dispatchers.Main){
                if (progressDialog.isShowing){
                    progressDialog.dismiss()
                }
                Snackbar.make(binding.root,"Post Updated Successfully", Snackbar.LENGTH_SHORT).setAction("Dismiss"
                ){

                }.show()
                findNavController().navigate(R.id.action_formFragment_to_mainFragment)
            }
        }

    }

    private fun createPost() {
        val title = binding.etTitle.text.toString()
        val body = binding.etBody.text.toString()
        if (title.isEmpty() || body.isEmpty()){
            Snackbar.make(binding.root,"Please fill all the fields", Snackbar.LENGTH_SHORT).setAction("Dismiss"
            ){

            }.show()
            progressDialog.dismiss()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.createPost(title,body)
            withContext(Dispatchers.Main){
                if (progressDialog.isShowing){
                    progressDialog.dismiss()
                }
                Snackbar.make(binding.root,"Post Created Successfully", Snackbar.LENGTH_SHORT).setAction("Dismiss"
                ){

                }.show()
                findNavController().navigate(R.id.action_formFragment_to_mainFragment)
            }
        }

    }


    private fun hideKeyboard(view: View) {
        val imm: InputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}