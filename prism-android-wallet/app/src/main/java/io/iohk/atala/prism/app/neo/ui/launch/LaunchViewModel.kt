package io.iohk.atala.prism.app.neo.ui.launch

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
