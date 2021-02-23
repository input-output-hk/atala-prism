package io.iohk.atala.prism.app.ui.main.contacts

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
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerDialogFragment
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.SimpleTextRecyclerViewAdapter
import io.iohk.atala.prism.app.neo.common.extensions.KEY_RESULT
import io.iohk.atala.prism.app.ui.main.credentials.CredentialUtil
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoDialogDeleteContactAlertBinding
import javax.inject.Inject

class DeleteContactAlertDialogFragment : DaggerDialogFragment() {

    companion object {
        const val REQUEST_DELETE_CONTACT = "REQUEST_DELETE_CONTACT"
    }

    private val args: DeleteContactAlertDialogFragmentArgs by navArgs()

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var binding: NeoDialogDeleteContactAlertBinding

    private val credentialsAdapter = object : SimpleTextRecyclerViewAdapter<Credential>(R.layout.row_simple_text_view, R.id.textView1) {
        override fun parseItemToString(item: Credential): String {
            return CredentialUtil.getType(item, requireContext())
        }
    }

    val viewModel: DeleteContactAlertDialogViewModel by lazy {
        return@lazy ViewModelProvider(this, factory).get(DeleteContactAlertDialogViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_dialog_delete_contact_alert, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        configureRecyclerView()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO find how to set this style within the app theme (R.style.AppTheme)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AlertDialogTheme)
        viewModel.fetchContactInfo(args.contactId.toInt())
    }

    private fun setObservers() {
        viewModel.dismiss.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().popBackStack()
                }
            }
        )
        viewModel.contactDeleted.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    findNavController().popBackStack()
                    setFragmentResult(REQUEST_DELETE_CONTACT, bundleOf(KEY_RESULT to Activity.RESULT_OK))
                }
            }
        )
        viewModel.credentials.observe(viewLifecycleOwner) {
            credentialsAdapter.clear()
            credentialsAdapter.addAll(it)
            credentialsAdapter.notifyDataSetChanged()
        }
    }

    private fun configureRecyclerView() {
        binding.credentialsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.credentialsRecyclerView.adapter = credentialsAdapter
    }
}
