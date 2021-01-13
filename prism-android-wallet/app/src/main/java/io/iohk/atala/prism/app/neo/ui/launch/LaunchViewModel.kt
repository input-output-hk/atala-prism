package io.iohk.atala.prism.app.neo.ui.launch

import androidx.lifecycle.*
import io.iohk.atala.prism.app.core.PrismApplication
import io.iohk.atala.prism.app.neo.data.SessionRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class LaunchViewModel @Inject constructor(private val sessionRepository: SessionRepository) : ViewModel() {

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
/*
/**
 * Factory for [LaunchViewModel].
 * */
object LaunchViewModelFactory : ViewModelProvider.Factory {

    private val sessionRepository = PrismApplication.applicationComponent.sessionRepository()

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LaunchViewModel(sessionRepository) as T
    }
}*/