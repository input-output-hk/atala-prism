package io.iohk.atala.prism.app.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerDialogFragment
import io.iohk.cvp.R
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.cvp.databinding.NeoDialogDeleteContactAlertBinding
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.SimpleTextRecyclerViewAdapter
import io.iohk.atala.prism.app.utils.IntentDataConstants
import io.iohk.atala.prism.app.viewmodel.DeleteContactAlertDialogViewModel
import io.iohk.atala.prism.app.viewmodel.DeleteContactAlertDialogViewModelFactory
import javax.inject.Inject

class DeleteContactAlertDialogFragment : DaggerDialogFragment() {

    companion object {
        // @TODO momentary solution, the use of "safeArgs" will be implemented
        fun build(contactId: Int): DeleteContactAlertDialogFragment {
            val dialog = DeleteContactAlertDialogFragment()
            val args = Bundle()
            args.putInt(IntentDataConstants.CONTACT_ID_KEY, contactId)
            dialog.arguments = args
            return dialog
        }
    }

    @Inject
    lateinit var factory: DeleteContactAlertDialogViewModelFactory

    private lateinit var binding: NeoDialogDeleteContactAlertBinding

    private val credentialsAdapter = object : SimpleTextRecyclerViewAdapter<Credential>(R.layout.row_simple_text_view, R.id.textView1) {
        override fun parseItemToString(item: Credential): String {
            return CredentialUtil.getType(item, requireContext())
        }
    }

    val viewModel: DeleteContactAlertDialogViewModel by lazy {
        return@lazy ViewModelProvider(this, factory).get(DeleteContactAlertDialogViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        // TODO this will be replaced by "safeArgs" when we implement navigation components
        val contactId = arguments?.getInt(IntentDataConstants.CONTACT_ID_KEY, -1) ?: -1
        if (contactId == -1) {
            dismiss()
            return
        }
        viewModel.fetchContactInfo(contactId)
    }

    private fun setObservers() {
        viewModel.dismiss.observe(viewLifecycleOwner, EventWrapperObserver {
            if (it) {
                dismiss()
            }
        })
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