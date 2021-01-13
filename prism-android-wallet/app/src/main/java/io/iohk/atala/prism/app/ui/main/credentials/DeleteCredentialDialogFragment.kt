package io.iohk.atala.prism.app.ui.main.credentials

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import dagger.android.support.DaggerDialogFragment
import io.iohk.cvp.R
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.cvp.databinding.DialogFragmentDeleteCredentialBinding
import javax.inject.Inject

class DeleteCredentialDialogFragment : DaggerDialogFragment() {

    companion object {
        fun build(credentialId: String): DeleteCredentialDialogFragment {
            val dialog = DeleteCredentialDialogFragment()
            dialog.credentialId = credentialId
            return dialog
        }
    }

    private lateinit var credentialId: String

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
        viewModel.fetchCredentialInfo(credentialId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_delete_credential, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.credentialDeleted.observe(viewLifecycleOwner, EventWrapperObserver {
            if (it) {
                dismiss()
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
            }
        })
        viewModel.canceled.observe(viewLifecycleOwner, EventWrapperObserver {
            if (it) {
                dismiss()
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
            }
        })
        viewModel.credential.observe(viewLifecycleOwner) {
            binding.credentialNameTextView.text = CredentialUtil.getName(it, requireContext())
            binding.credentialLogoImageView.setImageDrawable(CredentialUtil.getLogo(it.credentialType, requireContext()))
        }
    }
}