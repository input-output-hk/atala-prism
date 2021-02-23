package io.iohk.atala.prism.app.ui.main.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.FileUtils
import io.iohk.atala.prism.app.neo.data.PreferencesRepository
import io.iohk.atala.prism.app.neo.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProfileViewModel @Inject constructor(private val repository:PreferencesRepository) : ViewModel() {

    companion object {
        private const val MAX_PROFILE_PHOTO_PX_SIZE = 800
    }

    private val _editMode = MutableLiveData<Boolean>()

    val editMode: LiveData<Boolean> = _editMode

    val fullName = MutableLiveData<String?>()

    val country = MutableLiveData<String?>()

    val email = MutableLiveData<String?>()

    private val _profileImage = MutableLiveData<Bitmap?>()

    val profileImage: LiveData<Bitmap?> = _profileImage

    private val _showLoading = MutableLiveData<EventWrapper<Boolean>>()

    val showLoading: LiveData<EventWrapper<Boolean>> = _showLoading

    fun loadPreferences() {
        viewModelScope.launch {
            val userProfile = repository.getUserProfile()
            fullName.value = userProfile.name
            country.value = userProfile.country
            email.value = userProfile.email
            _profileImage.value = userProfile.profileImage
        }
    }

    fun setEditMode() {
        _editMode.value = true
    }

    fun savePreferences() {
        if (editMode.value == true) {
            _showLoading.value = EventWrapper(true)
            viewModelScope.launch {
                repository.storeUserProfile(UserProfile(fullName.value,country.value,email.value,profileImage.value))
                _editMode.value = false
                _showLoading.value = EventWrapper(false)
            }
        }
    }

    fun clearProfileImage() {
        _profileImage.value = null
    }

    fun handleProfilePhotoUri(imageUri: Uri, ctx:Context){
        viewModelScope.launch(Dispatchers.Main) {
            _showLoading.postValue(EventWrapper(true))
            FileUtils.decodeBitmapFromUri(ctx, imageUri, MAX_PROFILE_PHOTO_PX_SIZE)?.let {
                _profileImage.postValue(it)
            }
            _showLoading.postValue(EventWrapper(false))
        }
    }
}