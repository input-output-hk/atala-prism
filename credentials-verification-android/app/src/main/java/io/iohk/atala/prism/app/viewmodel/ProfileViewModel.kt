package io.iohk.atala.prism.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.*
import io.iohk.atala.prism.app.views.Preferences
import kotlinx.coroutines.launch

class ProfileViewModel(private val preferences: Preferences) : ViewModel() {

    private val _editMode = MutableLiveData<Boolean>()

    val editMode: LiveData<Boolean> = _editMode

    val fullName = MutableLiveData<String>()

    val country = MutableLiveData<String>()

    val email = MutableLiveData<String>()

    private val _profileImage = MutableLiveData<Bitmap>()

    val profileImage: LiveData<Bitmap> = _profileImage

    private val _showLoading = MutableLiveData<Boolean>()

    val showLoading: LiveData<Boolean> = _showLoading

    fun loadPreferences() {
        viewModelScope.launch {
            fullName.value = preferences.getString(Preferences.USER_PROFILE_NAME)
            country.value = preferences.getString(Preferences.USER_PROFILE_COUNTRY)
            email.value = preferences.getString(Preferences.USER_PROFILE_EMAIL)
            _profileImage.value = preferences.getProfileImage()
        }
    }

    fun setEditMode() {
        _editMode.value = true
    }

    fun savePreferences() {
        if (editMode.value == true) {
            _showLoading.value = true
            viewModelScope.launch {
                preferences.saveUserProfile(fullName.value, country.value, email.value, profileImage.value)
                _editMode.value = false
                _showLoading.value = false
            }
        }
    }

    fun setProfileImage(image: Bitmap) {
        _profileImage.value = image
    }

    fun clearProfileImage() {
        _profileImage.value = null
    }
}

/**
 * Factory for [ProfileViewModel].
 * */
class ProfileViewModelFactory(private val preferences: Preferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Preferences::class.java).newInstance(preferences)
    }
}