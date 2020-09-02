package io.iohk.cvp.neo.ui.launch

import androidx.lifecycle.*
import io.iohk.cvp.core.CvpApplication
import io.iohk.cvp.neo.data.SessionRepository
import kotlinx.coroutines.launch

class LaunchViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    val sessionDataHasStored: LiveData<Boolean> = sessionRepository.sessionDataHasStored

    /*
    * make a session data request
    * */
    fun checkSession() {
        viewModelScope.launch {
            sessionRepository.fetchSession()
        }
    }
}

/**
 * Factory for [LaunchViewModel].
 * */
object LaunchViewModelFactory : ViewModelProvider.Factory {

    private val sessionRepository = CvpApplication.applicationComponent.sessionRepository()

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LaunchViewModel(sessionRepository) as T
    }
}