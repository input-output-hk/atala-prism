package io.iohk.atala.prism.app.ui.commondialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.support.DaggerDialogFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.ui.utils.interfaces.MainActivityEventHandler
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoDialogAcceptConnectionBinding
import javax.inject.Inject

class AcceptConnectionDialogFragment : DaggerDialogFragment() {

    private val args: AcceptConnectionDialogFragmentArgs by navArgs()

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private
    val viewModel: AcceptConnectionDialogViewModel by lazy {
        ViewModelProvider(this, factory).get(AcceptConnectionDialogViewModel::class.java)
    }

    private lateinit var binding: NeoDialogAcceptConnectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        viewModel.fetchConnectionTokenInfo(args.token)
    }

    private fun setObservers() {
        viewModel.connectionError.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    requireActivity().showErrorDialog(R.string.server_error_message)
                    dismiss()
                }
            }
        )
        viewModel.connectionIsConfirmed.observe(
            viewLifecycleOwner,
            EventWrapperObserver { confirmed ->
                if (confirmed) {
                    FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalyticsEvents.NEW_CONNECTION_CONFIRM, null)
                    handleNewUserEvent()
                    dismiss()
                }
            }
        )
        viewModel.connectionIsDeclined.observe(
            viewLifecycleOwner,
            EventWrapperObserver { declined ->
                if (declined) {
                    FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalyticsEvents.NEW_CONNECTION_DECLINE, null)
                    dismiss()
                }
            }
        )
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
