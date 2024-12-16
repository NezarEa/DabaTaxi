package com.dabataxi.Auth.Customer

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://daba-taxi-43b44-default-rtdb.europe-west1.firebasedatabase.app/"
    ).reference
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _userProfile = MutableLiveData<Customer?>()
    val userProfile: LiveData<Customer?> = _userProfile

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: MutableLiveData<String?> = _statusMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: run {
            _statusMessage.value = "User not authenticated"
            _userProfile.value = null
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    database.child("users").child(userId).get().await()
                }

                if (snapshot.exists()) {
                    val profile = Customer(
                        name = snapshot.child("name").getValue(String::class.java) ?: "",
                        email = snapshot.child("email").getValue(String::class.java) ?: "",
                        phone = snapshot.child("phone").getValue(String::class.java) ?: "",
                        photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""
                    )
                    _userProfile.value = profile
                    _statusMessage.value = null
                } else {
                    _statusMessage.value = "No user data found."
                    _userProfile.value = null
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load user data: ${e.localizedMessage}"
                _userProfile.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun updateUserProfile(
        name: String,
        email: String,
        phone: String,
        password: String? = null,
        profileImageUri: Uri? = null
    ) {
        val user = auth.currentUser
        val userId = user?.uid ?: run {
            _statusMessage.value = "User not authenticated."
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Update password if provided
                password?.takeIf { it.isNotEmpty() }?.let {
                    withContext(Dispatchers.IO) {
                        user.updatePassword(it).await()
                    }
                }

                // Upload image if provided
                val profileImageUrl = profileImageUri?.let {
                    uploadProfileImage(it, userId)
                }

                // Prepare update data
                val updates = mutableMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone
                ).apply {
                    profileImageUrl?.let { this["profileImageUrl"] = it }
                }

                // Update user data in Realtime Database
                withContext(Dispatchers.IO) {
                    database.child("users").child(userId).updateChildren(updates as Map<String, Any>).await()
                }

                // Reload user profile to reflect changes
                loadUserProfile()

                _statusMessage.value = "Profile updated successfully."
            } catch (e: Exception) {
                _statusMessage.value = "Error updating profile: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadProfileImage(imageUri: Uri, userId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val storageRef = storage.reference.child("profileImages/$userId.jpg")
                storageRef.putFile(imageUri).await()
                storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                _statusMessage.postValue("Error uploading image: ${e.localizedMessage}")
                throw e
            }
        }
    }
}