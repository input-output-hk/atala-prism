package io.iohk.atala.prism.app.ui.payid.step2

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.buildRequestPermissionLauncher
import io.iohk.atala.prism.app.neo.common.extensions.hideBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.ui.commondialogs.SuccessDialog
import io.iohk.atala.prism.app.utils.IntentDataConstants
import io.iohk.atala.prism.app.utils.PermissionUtils
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentPayIdSetupFormBinding
import javax.inject.Inject

class PayIdSetupFormFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: PayIdSetupFormViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PayIdSetupFormViewModel::class.java)
    }

    private lateinit var binding: FragmentPayIdSetupFormBinding

    // Launcher for QrCodeScannerActivity
    private val qrActivityResultLauncher = buildActivityResultLauncher { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data?.hasExtra(IntentDataConstants.QR_RESULT) == true) {
            viewModel.walletPublicKey.value = activityResult.data!!.getStringExtra(IntentDataConstants.QR_RESULT)!!
        }
    }

    private val cameraPermissionLauncher = buildRequestPermissionLauncher { permissionGranted ->
        if (permissionGranted) showQRScanner()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pay_id_setup_form, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.walletPublicKeyTextInput.setEndIconOnClickListener {
            showQRScanner()
        }
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.error.observe(
            viewLifecycleOwner,
            EventWrapperObserver { error ->
                when (error) {
                    is PayIdSetupFormViewModel.Error.PayIdNameAlreadyTaken -> {
                        val errorMessage = getString(
                            R.string.fragment_pay_id_setup_form_error_msg_unavailable_pay_id_name,
                            error.payIdName
                        )
                        requireActivity().showErrorDialog(errorMessage)
                    }
                    is PayIdSetupFormViewModel.Error.ServerError -> {
                        requireActivity().showErrorDialog(error.message)
                    }
                    is PayIdSetupFormViewModel.Error.TimeOutError -> {
                        requireActivity().showErrorDialog(R.string.error_connection_timeout)
                    }
                    else -> {
                        requireActivity().showErrorDialog(R.string.server_error_message)
                    }
                }
            }
        )

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) requireActivity().showBlockUILoading() else requireActivity().hideBlockUILoading()
        }

        viewModel.eventRegistrationIsCompleted.observe(
            viewLifecycleOwner,
            EventWrapperObserver { success ->
                if (success) {
                    showSuccessDialog()
                }
            }
        )
    }

    private fun showQRScanner() {
        if (PermissionUtils.checkIfAlreadyHavePermission(requireContext(), Manifest.permission.CAMERA)) {
            val intent = IntentUtils.intentQRCodeScanner(requireContext())
            qrActivityResultLauncher.launch(intent)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showSuccessDialog() {
        SuccessDialog.Builder(requireContext())
            .setPrimaryText(R.string.fragment_pay_id_setup_form_success_dialog_primary_text)
            .setSecondaryText(R.string.fragment_pay_id_setup_form_success_dialog_secondary_text)
            .setCustomButtonText(R.string.fragment_pay_id_setup_form_success_dialog_button_text)
            .setOkButtonClickLister {
                navigateToPayIdDetail()
            }.build()
            .show(requireActivity().supportFragmentManager, null)
    }

    private fun navigateToPayIdDetail() {
        findNavController().navigate(R.id.action_payIdSetupFormFragment_to_payIdDetailFragment)
    }
}
