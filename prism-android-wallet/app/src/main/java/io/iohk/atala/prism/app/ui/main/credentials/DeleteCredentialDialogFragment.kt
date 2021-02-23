package io.iohk.atala.prism.app.ui.main.credentials

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.android.support.DaggerDialogFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.KEY_RESULT
import io.iohk.cvp.R
import io.iohk.cvp.databinding.DialogFragmentDeleteCredentialBinding
import javax.inject.Inject

class DeleteCredentialDialogFragment : DaggerDialogFragment() {

    companion object {
        const val REQUEST_DELETE_CREDENTIAL = "REQUEST_DELETE_CREDENTIAL"
    }

    private val args: DeleteCredentialDialogFragmentArgs by navArgs()

    private lateinit var binding: DialogFragmentDeleteCredentialBinding

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: DeleteCredentialDialogViewModel by lazy {
        ViewModelProvider(this, factory).get(DeleteCredentialDialogViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO find how to set this style within the app theme (R.style.AppTheme)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AlertDialogTheme)
        KEY_RESULT
        viewModel.fetchCredentialInfo(args.credentialId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_delete_credential, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.credentialDeleted.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().popBackStack()
                    setFragmentResult(REQUEST_DELETE_CREDENTIAL, bundleOf(KEY_RESULT to Activity.RESULT_OK))
                }
            }
        )
        viewModel.canceled.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().popBackStack()
                    setFragmentResult(REQUEST_DELETE_CREDENTIAL, bundleOf(KEY_RESULT to Activity.RESULT_CANCELED))
                }
            }
        )
        viewModel.credential.observe(viewLifecycleOwner) {
            binding.credentialNameTextView.text = CredentialUtil.getName(it, requireContext())
            binding.credentialLogoImageView.setImageDrawable(CredentialUtil.getLogo(it.credentialType, requireContext()))
        }
    }
}
