package com.example.tweetapp.ui.auth

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tweetapp.databinding.FragmentRegisterBinding
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private val binding : FragmentRegisterBinding by lazy {
        FragmentRegisterBinding.inflate(layoutInflater)
    }
    private val userViewModel : UserViewModel by activityViewModels()
    private lateinit var auth : FirebaseAuth
    private lateinit var progressDialog : Dialog


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
        progressDialog = ProgressHelper.buildProgressDialog(requireContext())

        binding.txtLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSignUp.setOnClickListener {
             signUp()
        }
    }

    private fun signUp() {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val emailValid = isEmailValid(email)
        val passwordValid = isPasswordValid(password)

        if (name.isEmpty()){
            binding.ipName.error = "Name cannot be empty"
            binding.etName.requestFocus()
            return
        }
        binding.ipName.isErrorEnabled = false

        if (!emailValid){
            binding.ipEmail.error = "Email is not valid"
            binding.etEmail.requestFocus()
            return
        }
        binding.ipEmail.isErrorEnabled = false

        if (!passwordValid){
            binding.ipPassword.error = "Password must contain at least 8 character, one digit, one uppercase and one lowercase"
            binding.etPassword.requestFocus()
            return
        }
        binding.ipPassword.isErrorEnabled = false
        progressDialog.show()
        createAccountWithPassword(email,password,name)
    }

    private fun createAccountWithPassword(email: String, password: String,name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    var created = false
                    user?.let {
                        lifecycleScope.launch {
                            userViewModel.createUser(
                                uuid = user.uid,
                                username = name,
                                profilePic = "",
                                email = email
                            ).takeWhile { !created }.collectLatest {response ->
                                when(response){
                                    is ApiState.Success ->{
                                        created = true
                                        if (progressDialog.isShowing){
                                            progressDialog.dismiss()
                                        }
                                        Toast.makeText(
                                            requireContext(),
                                            "Account created successfully",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        resetFields()
                                        auth.signOut()
                                    }

                                    is  ApiState.Error -> {
                                        if (progressDialog.isShowing){
                                            progressDialog.dismiss()
                                        }
                                        Toast.makeText(
                                            requireContext(),
                                            task.exception?.message.toString(),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }

                                    is ApiState.Loading -> {

                                    }
                                }

                            }
                        }
                    }
                } else {
                    //If sign in fails, display a message to the user.
                    Toast.makeText(
                        requireContext(),
                        task.exception?.message.toString(),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun resetFields(){
        binding.etName.text?.clear()
        binding.etEmail.text?.clear()
        binding.etPassword.text?.clear()
    }

    private fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
        val emailPattern = Regex(emailRegex)
        return emailPattern.matches(email)
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{8,}\$"
        //8 length,should contain digit,uppercase,lowercase and special char
        val passwordPattern = Regex(passwordRegex)
        return passwordPattern.matches(password)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            auth.signOut()
        }
    }

}