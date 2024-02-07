package com.example.tweetapp.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.tweetapp.R
import com.example.tweetapp.databinding.FragmentSettingBinding
import com.example.tweetapp.datastore.SettingPref
import com.example.tweetapp.model.ApiState
import com.example.tweetapp.ui.auth.LoginActivity
import com.example.tweetapp.utils.Constants
import com.example.tweetapp.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private val binding : FragmentSettingBinding by lazy {
        FragmentSettingBinding.inflate(layoutInflater)
    }
    private lateinit var auth : FirebaseAuth
    private val userViewModel : UserViewModel by activityViewModels()
    private var connection = false

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
        binding.logout.setOnClickListener{
            if (connection){
                auth.signOut()
                if (auth.currentUser == null){
                    userViewModel.setLogin(false)
                    startActivity(Intent(requireContext(),LoginActivity::class.java))
                }
            }else{
                Toast.makeText(requireContext(),"No connection",Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            userViewModel.getUserById(auth.uid.toString())
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val key = booleanPreferencesKey(Constants.NETWORK_STATE)
        SettingPref(requireContext(),key).getNetworkState.asLiveData()
            .observe(viewLifecycleOwner) {
            connection = it
        }

        userViewModel.userData.observe(viewLifecycleOwner){response ->
            when(response){
                is ApiState.Success -> {
                    binding.txtName.text = response.data.user_name
                    binding.txtEmail.text = response.data.email
                    activity?.let {
                        Glide.with(requireContext()).load(response.data.profile_pic).placeholder(R.drawable.img).into(binding.imgProfile)
                    }
                }
                is ApiState.Error -> {
                    Toast.makeText(requireContext(),response.message,Toast.LENGTH_SHORT).show()
                }
                is ApiState.Loading ->{

                }
            }
        }

    }

}