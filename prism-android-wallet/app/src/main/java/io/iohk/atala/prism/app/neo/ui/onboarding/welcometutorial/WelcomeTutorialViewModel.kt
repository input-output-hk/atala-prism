package io.iohk.atala.prism.app.neo.ui.onboarding.welcometutorial

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.neo.common.EventWrapper

class WelcomeTutorialViewModel : ViewModel() {

    companion object {
        const val TOTAL_STEPS = 3
    }

    /*
    * Current Tutorial Step
    * */
    private val _step = MutableLiveData<Int>().apply { value = 0 }

    val step: MutableLiveData<Int> = _step

    /*
    * Handle when Tutorial should go to the create account screen
    * */
    private val _shouldGoToCreateAccount = MutableLiveData<EventWrapper<Boolean>>()

    val shouldGoToCreateAccount: LiveData<EventWrapper<Boolean>> = _shouldGoToCreateAccount

    /*
    * Handle when Tutorial should go to the recovery account screen
    * */
    private val _shouldGoToRestoreAccount = MutableLiveData<EventWrapper<Boolean>>()

    val shouldGoToRestoreAccount: LiveData<EventWrapper<Boolean>> = _shouldGoToRestoreAccount

    /*
    * Handle when the tutorial need to return to a previous screen
    * */
    private val _shouldReturn = MutableLiveData<EventWrapper<Boolean>>()

    val shouldReturn: LiveData<EventWrapper<Boolean>> = _shouldReturn

    /*
     * Go to the next step, if it is the last one, it
     * should not do anything
     * */
    fun next() {
        if (step.value!! < (TOTAL_STEPS - 1)) {
            _step.value = (_step.value ?: 0) + 1
        }
    }

    /*
     * Go to the previous step, if it is the first one,
     * must establish that the tutorial should go back
     * to the previous screen
     * */
    fun previous() {
        if (step.value == 0) {
            _shouldReturn.value = EventWrapper(true)
        } else {
            _step.value = (_step.value ?: 0) - 1
        }
    }


    /*
     * If it is the last step, it should go to the create account screen
     * if it is not the last step, you must go to the next.
     * */
    fun onCreateAccountButtonTaped() {
        if (step.value == (TOTAL_STEPS - 1)) {
            _shouldGoToCreateAccount.value = EventWrapper(true)
        } else {
            next()
        }
    }

    fun onRestoreAccountButtonTaped() {
        _shouldGoToRestoreAccount.value = EventWrapper(true)
    }
}