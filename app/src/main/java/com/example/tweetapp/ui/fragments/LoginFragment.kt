package com.example.tweetapp.ui.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentLoginBinding
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.model.User
import com.example.tweetapp.utils.ProgressHelper
import com.example.tweetapp.viewmodel.PostViewModel
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val binding : FragmentLoginBinding by lazy {
        FragmentLoginBinding.inflate(layoutInflater)
    }
    private val userViewModel : UserViewModel by activityViewModels()
    private val postViewModel : PostViewModel by activityViewModels()
    private val TAG = "google_sign"
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private var showOneTapUI = true
    private var auth: FirebaseAuth? = null
    private lateinit var progressDialog: Dialog
    private var loggedIn = false

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

        binding.btnLogin.setOnClickListener {

        }

        binding.txtSignUp.setOnClickListener {
           findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }


        binding.btnGoogleLogin.setOnClickListener {
            loginWithGoogle()
        }

        binding.btnLogin.setOnClickListener {
            loginWithPassword()
        }

        binding.btnForgotPassword.setOnClickListener {

        }

    }



    private fun loginWithPassword() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        if (email.isEmpty()){
            binding.ipEmail.error = "Email can't be empty"
            binding.etEmail.requestFocus()
            return
        }
        binding.ipEmail.isErrorEnabled = false

        if (password.isEmpty()){
            binding.ipPassword.error = "Email can't be empty"
            binding.etPassword.requestFocus()
            return
        }
        binding.ipPassword.isErrorEnabled = false


        progressDialog.show()
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth?.currentUser
                    if (user!=null){
                        progressDialog.dismiss()
                        goToNextFragment()
                    }
                } else {
                    progressDialog.dismiss()
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        context,
                        task.exception?.message,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun loginWithGoogle() {
        oneTapClient = Identity.getSignInClient(requireActivity())
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        signInWithIntent()
    }


    private fun signInWithIntent(){
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(requireActivity()) { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                    googleIntentResultLauncher.launch(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    Log.d(TAG,"failed : ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(requireActivity()) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG,"failed : ${e.localizedMessage}")
            }
    }

    private val googleIntentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result != null) {
                val data = result.data
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val password = credential.password

                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            Log.d(TAG, "Got ID token.")
                            if (!progressDialog.isShowing){
                                progressDialog.show()
                            }
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth?.signInWithCredential(firebaseCredential)
                                ?.addOnCompleteListener(requireActivity()) { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithCredential:success")

                                        val user = auth?.currentUser
                                        val userData =  user?.run {
                                            User(
                                                uuid = uid,
                                                username = displayName.toString(),
                                                profilePic = photoUrl.toString(),
                                                email = email.toString()
                                            )
                                        }

                                        if (userData != null) {
                                            checkIfKeyExists(key = auth?.uid!!, userData = userData)
                                        }

                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                                    }
                                }
                        }

                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d(TAG, "Got password.")
                        }

                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                        }

                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }

                        else -> {
                            Log.d(
                                TAG, "Couldn't get credential from result." +
                                        " (${e.localizedMessage})"
                            )
                        }
                    }
                }
            }
        }

    private fun checkIfKeyExists(key: String, userData: User) {
            userViewModel.getAllUsers()
            userViewModel.users.observe(viewLifecycleOwner) {response ->
                when(response){
                    is ApiState.Success ->{
                        progressDialog.dismiss()
                       val userExist = response.data.any {
                            it.uuid == key
                        }
                        if (userExist){
                            val pic = auth?.currentUser?.photoUrl.toString()
                            viewLifecycleOwner.lifecycleScope.launch {
                                userViewModel.updateUser(
                                    uuid = key,
                                    email = userData.email,
                                    username = userData.username,
                                    profilePic = pic
                                ).takeWhile { !loggedIn }.collect(collector)
                            }
                        }else{
                            viewLifecycleOwner.lifecycleScope.launch{
                                userViewModel.createUser(
                                    uuid = userData.uuid,
                                    email = userData.email,
                                    username = userData.username,
                                    profilePic = userData.profilePic
                                ).takeWhile { !loggedIn }.collect(collector)
                            }
                        }
                    }

                    is ApiState.Error -> {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(),response.message,Toast.LENGTH_SHORT).show()
                    }

                    is ApiState.Loading -> {

                    }
                }
            }
    }


    private val collector = FlowCollector<ApiState<Any>>{response ->
        when(response){
            is ApiState.Success ->{
                // Update was successful
                loggedIn = true
                if (progressDialog.isShowing){
                    progressDialog.dismiss()
                }
                goToNextFragment()
            }

            is  ApiState.Error -> {
                progressDialog.dismiss()
                Toast.makeText(requireContext(),response.message,Toast.LENGTH_SHORT).show()
            }

            is ApiState.Loading -> {

            }
        }

    }

    private fun goToNextFragment(){
        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
    }

    override fun onStart() {
        super.onStart()
        if (auth?.currentUser!=null){
            goToNextFragment()
        }
    }

}