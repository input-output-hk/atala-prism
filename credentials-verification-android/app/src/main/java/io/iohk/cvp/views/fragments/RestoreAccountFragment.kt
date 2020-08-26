package io.iohk.cvp.views.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.SupportErrorDialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.utils.CryptoUtils
import io.iohk.cvp.viewmodel.RestoreAccountViewModel
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator
import io.iohk.cvp.views.fragments.utils.NoAppBar
import io.iohk.cvp.views.utils.EditTextFiltersUtils
import io.iohk.cvp.views.utils.components.ChipsAtala
import kotlinx.android.synthetic.main.restore_account_fragment.*
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class RestoreAccountFragment : CvpFragment<RestoreAccountViewModel>() {

    companion object {
        private const val MAX_PHRASE_LENGTH = 15
        private const val WHITE_SPACES: String = ""
    }

    private var phrasesList : MutableList<String> = LinkedList()

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        phrases_input.setOnEditorActionListener { _, _, _ ->
            addNewChip(phrases_input.text.toString())
            true
        }
        accept_button.setOnClickListener {
            showLoading()
            getViewModel().validateMnemonics(phrasesList)
        }
        phrases_input.filters = arrayOf(EditTextFiltersUtils.characterFilter(WHITE_SPACES), InputFilter.LengthFilter(MAX_PHRASE_LENGTH))
        phrases_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hideErrors()
            }
        })
        phrases_input.requestFocus()
        initObservers()
    }

    private fun initObservers() {
        getViewModel().getErrorMessageLiveData().observe(viewLifecycleOwner, Observer {
            hideLoading()
            showErrorMessage(it)
        })
        getViewModel().getAccountRecoveredMessageLiveData().observe(viewLifecycleOwner, Observer {
            hideLoading()
            if (it) navigator.showRecoveryAccountSuccess(activity)
        })
    }

    private fun addNewChip(word: String) {
        if(word.trim().isEmpty()) {
            showErrorMessage(getString(R.string.word_not_empty))
            return
        }
        if(phrasesList.size == 12)
            return
        phrases_input.text.clear()

        val textToAdd = "${phrasesList.size + 1}. $word"
        val chip = ChipsAtala(requireContext())
        chip.text = textToAdd

        chips_container.addView(chip as View, phrasesList.size)
        phrasesList.add(word)
        chip.setOnCloseIconClickListener {
            phrasesList.removeAt(chips_container.indexOfChild(chip as View))
            val newList = ArrayList(phrasesList)
            chips_container.removeViewsInLayout(0, chips_container.childCount - 1)
            phrasesList.clear()
            newList.forEach { wordToAdd ->
                addNewChip(wordToAdd)
            }
            checkListCount()
        }
        checkListCount()
    }

    private fun showErrorMessage(errorMessage: String) {
        error_text_view.text = errorMessage
        showErrorsViews()
    }

    private fun checkListCount() {
        phrases_input.visibility = if(phrasesList.size == 12) View.GONE else View.VISIBLE
    }

    private fun showErrorsViews() {
        material_card_container.strokeColor = resources.getColor(R.color.bt_error_red,null)
        error_message_container.visibility = View.VISIBLE
    }

    private fun hideErrors() {
        material_card_container.strokeColor = resources.getColor(R.color.phrases_cointaner_border_color,null)
        error_message_container.visibility = View.GONE
    }

    override fun getViewModel(): RestoreAccountViewModel {
        return ViewModelProviders.of(this, factory).get(RestoreAccountViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        return NoAppBar()
    }

    override fun getViewId(): Int {
        return R.layout.restore_account_fragment
    }
}
