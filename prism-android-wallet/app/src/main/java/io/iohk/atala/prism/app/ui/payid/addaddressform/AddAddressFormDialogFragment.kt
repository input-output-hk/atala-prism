package io.iohk.atala.prism.app.ui.payid.addaddressform

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.android.support.DaggerDialogFragment
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
import io.iohk.cvp.databinding.DialogFragmentAddAddressFormBinding
import javax.inject.Inject

class AddAddressFormDialogFragment : DaggerDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

    private lateinit var binding: DialogFragmentAddAddressFormBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: AddAddressFormViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(AddAddressFormViewModel::class.java)
    }

    private val qrActivityResultLauncher = buildActivityResultLauncher { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data?.hasExtra(IntentDataConstants.QR_RESULT) == true) {
            viewModel.walletPublicKey.value = activityResult.data!!.getStringExtra(IntentDataConstants.QR_RESULT)!!
        }
    }

    private val cameraPermissionLauncher = buildRequestPermissionLauncher { permissionGranted ->
        if (permissionGranted) showQRScanner()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogFragmentAddAddressFormBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        binding.walletPublicKeyTextInput.setEndIconOnClickListener {
            showQRScanner()
        }
        return binding.root
    }

    private fun setObservers() {
        viewModel.error.observe(
            viewLifecycleOwner,
            EventWrapperObserver { error ->
                when (error) {
                    is AddAddressFormViewModel.Error.ServerError -> {
                        requireActivity().showErrorDialog(error.message)
                    }
                    else -> {
                        requireActivity().showErrorDialog(R.string.server_error_message)
                    }
                }
            }
        )
        viewModel.successEvent.observe(
            viewLifecycleOwner,
            EventWrapperObserver { success ->
                if (success) {
                    dismissAndShowSuccessDialog()
                }
            }
        )
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) requireActivity().showBlockUILoading() else requireActivity().hideBlockUILoading()
        }
    }

    private fun showQRScanner() {
        if (PermissionUtils.checkIfAlreadyHavePermission(requireContext(), Manifest.permission.CAMERA)) {
            val intent = IntentUtils.intentQRCodeScanner(requireContext())
            qrActivityResultLauncher.launch(intent)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun dismissAndShowSuccessDialog() {
        findNavController().popBackStack()
        SuccessDialog.Builder(requireContext())
            .setPrimaryText(R.string.dialog_fragment_new_address_form_success_dialog_primary_text)
            .setSecondaryText(R.string.dialog_fragment_new_address_form_success_dialog_secondary_text)
            .setCustomButtonText(R.string.dialog_fragment_new_address_form_success_dialog_button_text)
            .build()
            .show(requireActivity().supportFragmentManager, null)
    }
}
