package io.iohk.atala.prism.app.neo.common

/**
 * This is a Simple class for storing when an event has been handled or not,
 * for example, if a ViewModel decides show a Toast or Navigate to another
 * activity or fragment can use a MutableLiveData<EventWrapper>
 * for this.
 *
 * Problem example:
 *
 * In this first example everything is apparently fine, when the showErrorMsg value is true
 * the Fragment shows the message but ... if the Fragment has been resumed because the user
 * navigated to another activity or fragment, the message will be displayed again, the same
 * if the livedata will be used to navigate to another view, in fact in this last situation
 * the problem is worse.
 *
 * class MyViewModel : ViewModel(){
 *
 *  private val _showErrorMsg = MutableLiveData<Boolean>()
 *  val showErrorMsg: LiveData<Boolean>  = _showErrorMsg
 *
 *  fun someFunction(){
 *      if(anyHasWrong){
 *          _showErrorMsg.value = true
 *      }
 *  }
 *
 * }
 *
 * class MyFragment : Fragment() {
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *          super.onViewCreated(view, savedInstanceState)
 *
 *          viewModel.showErrorMsg.observe(viewLifecycleOwner, Observer {
 *              if(it == true){
 *                  Toast.makeText(context, "Error Message", Toast.LENGTH_SHORT).show()
 *              }
 *          })
 *     }
 *
 * }
 *
 *
 *
 * THE SOLUTION WITH [EventWrapper]
 *
 * * class MyViewModel : ViewModel(){
 *
 *  private val _showErrorMsg = MutableLiveData<EventWrapper<String>>()
 *  val showErrorMsg: LiveData<EventWrapper<String>>  = _showErrorMsg
 *
 *  fun someFunction(){
 *      if(anyHasWrong){
 *          _showErrorMsg.value = EventWrapper<String>("Error Message")
 *      }
 *  }
 *
 * }
 *
 * class MyFragment : Fragment() {
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *          super.onViewCreated(view, savedInstanceState)
 *
 *          viewModel.showErrorMsg.observe(viewLifecycleOwner, Observer {
 *              it.getContentIfNotHandled()?.let { errorMsg ->
 *                  Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
 *              }
 *          })
 *     }
 * }
 *
 *  PLUS.- You can use a [EventWrapperObserver] for a cleaner code when observing the event
 *
 *      viewModel.showErrorMsg.observe(viewLifecycleOwner, EventWrapperObserver { errorMsg ->
 *          Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
 *      })
 *
 * */

class EventWrapper<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        if (hasBeenHandled) {
            return null
        }
        hasBeenHandled = true
        return content
    }

    fun peekContent(): T = content
}
