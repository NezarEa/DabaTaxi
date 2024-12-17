package com.dabataxi.Auth.Customer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.dabataxi.databinding.FragmentLoginHomeCstBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.material.snackbar.Snackbar

class LoginHomeCST : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentLoginHomeCstBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // For handling Google sign-in result using ActivityResult API
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data: Intent? = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginHomeCstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }

        binding.btnForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        binding.btnGoogleLogin.setOnClickListener {
            performGoogleLogin()
        }

        binding.btnCreateAccount.setOnClickListener {
            navigateToSignUp()
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var isValid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_cannot_be_empty)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email_format)
            isValid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_cannot_be_empty)
            isValid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.password_must_be_at_least_8_characters)
            isValid = false
        }

        return isValid
    }

    private fun performLogin(email: String, password: String) {
        showProgress(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    private fun handleLoginError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                Snackbar.make(requireView(),
                    getString(R.string.user_not_found), Snackbar.LENGTH_LONG).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Snackbar.make(requireView(),
                    getString(R.string.invalid_credentials), Snackbar.LENGTH_LONG).show()
            }
            else -> {
                Snackbar.make(requireView(),
                    getString(R.string.login_failed_try_again), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_loginHomeCST_to_homeAppCST)
    }

    private fun navigateToForgotPassword() {
        findNavController().navigate(R.id.action_loginHomeCST_to_forgetPassCst)
    }

    private fun navigateToSignUp() {
        findNavController().navigate(R.id.action_loginHomeCST_to_signUpCst)
    }

    private fun performGoogleLogin() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                Snackbar.make(requireView(),
                    getString(R.string.google_sign_in_failed), Snackbar.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignInError", "Error code: ${e.statusCode}")
            Snackbar.make(requireView(), "Google sign-in failed", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    Snackbar.make(requireView(), "Google sign-in failed", Snackbar.LENGTH_LONG).show()
                }
            }
    }
}
