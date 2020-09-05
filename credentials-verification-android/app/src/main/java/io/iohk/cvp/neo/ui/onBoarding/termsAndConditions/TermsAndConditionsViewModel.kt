package io.iohk.cvp.neo.ui.onBoarding.termsAndConditions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.iohk.cvp.neo.common.EventWrapper

class TermsAndConditionsViewModel : ViewModel() {
    /**
    * [NavigationAction] This class describes all the navigation options that TermsAndConditionsViewModel should have
    * */
    enum class NavigationAction { TERMS_AND_CONDITIONS, PRIVACY_POLICY, NEXT }

    val termsAndConditionsAccepted = MutableLiveData<Boolean>().apply { value = false }

    val privacyPolicyAccepted = MutableLiveData<Boolean>().apply { value = false }

    val acceptButtonEnable = MediatorLiveData<Boolean>().apply {
        addSource(termsAndConditionsAccepted) { value = computeAcceptButtonEnable() }
        addSource(privacyPolicyAccepted) { value = computeAcceptButtonEnable()}
    }

    private val _shouldNavigate = MutableLiveData<EventWrapper<NavigationAction>>()

    val shouldNavigate: LiveData<EventWrapper<NavigationAction>> = _shouldNavigate

    /*
    * Handle when should go to the next screen
    * */
    fun acceptButtonTaped() {
        _shouldNavigate.value = EventWrapper(NavigationAction.NEXT)
    }

    /*
    * Handle when should show terms and conditions screen
    * */
    fun termsAndConditionsButtonTaped() {
        _shouldNavigate.value = EventWrapper(NavigationAction.TERMS_AND_CONDITIONS)
    }

    /*
    * Handle when should show privacy policy screen
    * */
    fun privacyPolicyButtonTaped() {
        _shouldNavigate.value = EventWrapper(NavigationAction.PRIVACY_POLICY)
    }

    private fun computeAcceptButtonEnable():Boolean{
        return termsAndConditionsAccepted.value == true && privacyPolicyAccepted.value == true
    }
}