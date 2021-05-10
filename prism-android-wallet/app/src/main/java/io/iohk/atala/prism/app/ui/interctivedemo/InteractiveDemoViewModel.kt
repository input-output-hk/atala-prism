package io.iohk.atala.prism.app.ui.interctivedemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.neo.common.EventWrapper

class InteractiveDemoViewModel : ViewModel() {

    companion object {
        const val TOTAL_STEPS = 3
    }

    /*
    * Current Tutorial Step
    * */
    private val _step = MutableLiveData<Int>().apply { value = 0 }

    val step: MutableLiveData<Int> = _step

    /*
    * Handle when the demo need to finish
    * */
    private val _shouldFinish = MutableLiveData<EventWrapper<Boolean>>()

    val shouldFinish: LiveData<EventWrapper<Boolean>> = _shouldFinish

    val isLastStep = Transformations.map(step) {
        it == (TOTAL_STEPS - 1)
    }

    fun next() {
        if (step.value!! < (InteractiveDemoViewModel.TOTAL_STEPS - 1)) {
            _step.value = (_step.value ?: 0) + 1
        } else {
            _shouldFinish.value = EventWrapper(true)
        }
    }

    fun previous() {
        if (step.value == 0) {
            _shouldFinish.value = EventWrapper(true)
        } else {
            _step.value = (_step.value ?: 0) - 1
        }
    }
}
