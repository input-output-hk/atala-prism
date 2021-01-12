package io.iohk.atala.prism.app.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.support.DaggerDialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoDialogAcceptConnectionBinding
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents
import io.iohk.atala.prism.app.viewmodel.AcceptConnectionDialogViewModel
import io.iohk.atala.prism.app.viewmodel.AcceptConnectionDialogViewModelFactory
import io.iohk.atala.prism.app.views.interfaces.MainActivityEventHandler
import javax.inject.Inject

class AcceptConnectionDialogFragment : DaggerDialogFragment() {

    companion object {
        const val KEY_TOKEN = "token"

        // @TODO momentary solution, the use of "safeArgs" will be implemented
        fun build(token: String): AcceptConnectionDialogFragment {
            val dialog = AcceptConnectionDialogFragment()
            val args = Bundle()
            args.putString(KEY_TOKEN, token)
            dialog.arguments = args
            return dialog
        }
    }

    @Inject
    lateinit var factory: AcceptConnectionDialogViewModelFactory

    private
    val viewModel: AcceptConnectionDialogViewModel by lazy {
        ViewModelProvider(this, factory).get(AcceptConnectionDialogViewModel::class.java)
    }

    private lateinit var binding: NeoDialogAcceptConnectionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_dialog_accept_connection, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        setDismissButtonsListeners()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO find how to set this style within the app theme (R.style.AppTheme)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AlertDialogTheme)
        // TODO this will be replaced with the use of "SafeArgs"
        val token = arguments?.getString(KEY_TOKEN, "") ?: ""
        if (token.isNotBlank()) {
            viewModel.fetchConnectionTokenInfo(token)
        } else {
            dismiss()
        }
    }

    private fun setObservers() {
        viewModel.connectionError.observe(viewLifecycleOwner, EventWrapperObserver {
            if (it) {
                requireActivity().showErrorDialog(R.string.server_error_message)
                dismiss()
            }
        })
        viewModel.connectionIsConfirmed.observe(viewLifecycleOwner, EventWrapperObserver { confirmed ->
            if (confirmed) {
                FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalyticsEvents.NEW_CONNECTION_CONFIRM, null)
                handleNewUserEvent()
                dismiss()
            }
        })
        viewModel.connectionIsDeclined.observe(viewLifecycleOwner, EventWrapperObserver { declined ->
            if (declined) {
                FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalyticsEvents.NEW_CONNECTION_DECLINE, null)
                dismiss()
            }
        })
    }

    private fun setDismissButtonsListeners() {
        // this "okButton" is only visible when a connection already added previously is showing
        binding.okButton.setOnClickListener { dismiss() }
    }

    /*
     * TODO Momentary solution, this will be discarded when we have the appropriate data repositories in this application.
     */
    private fun handleNewUserEvent() {
        (requireActivity() as? MainActivityEventHandler)?.handleEvent(MainActivityEventHandler.MainActivityEvent.SYNC_REQUEST)
    }
}