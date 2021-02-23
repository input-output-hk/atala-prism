package io.iohk.atala.prism.app.neo.ui.onboarding.restoreaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.ui.commondialogs.LoadingDialog
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentRestoreAccountBinding
import javax.inject.Inject

class RestoreAccountFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: NeoFragmentRestoreAccountBinding

    private var loadingDialog: LoadingDialog? = null

    private val viewModel: RestoreAccountViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(RestoreAccountViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.neo_fragment_restore_account, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        addObservers()
        return binding.root
    }

    private fun addObservers() {
        viewModel.error.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {
                    RestoreAccountViewModel.ErrorType.InvalidSecurityWordsLength -> {
                        binding.errorTextView.setText(R.string.recovery_must_have_twelve_words)
                    }
                    RestoreAccountViewModel.ErrorType.InvalidSecurityWord -> {
                        binding.errorTextView.setText(R.string.invalid_recovery_phrase)
                    }
                    RestoreAccountViewModel.ErrorType.UnknownError -> {
                        binding.errorTextView.setText(R.string.server_error_message)
                    }
                }
            }
        )

        viewModel.isLoading.observe(
            viewLifecycleOwner,
            Observer {
                if (it == true) {
                    loadingDialog = LoadingDialog()
                    loadingDialog?.show(requireActivity().supportFragmentManager, null)
                } else {
                    loadingDialog?.dismiss()
                }
            }
        )

        viewModel.accountRestoredSuccessfully.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().navigate(R.id.action_restoreAccountFragment_to_restoreAccountSuccessFragment)
                }
            }
        )
    }
}
