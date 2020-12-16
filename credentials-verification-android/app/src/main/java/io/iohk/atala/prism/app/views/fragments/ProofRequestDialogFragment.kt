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
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.viewmodel.ProofRequestDialogViewModel
import io.iohk.atala.prism.app.viewmodel.ProofRequestDialogViewModelFactory
import io.iohk.atala.prism.app.views.interfaces.MainActivityEventHandler
import io.iohk.atala.prism.app.views.utils.adapters.ProofRequestDialogAdapter
import io.iohk.atala.prism.app.views.utils.dialogs.SuccessDialog
import io.iohk.cvp.databinding.DialogFragmentProofRequestBinding
import javax.inject.Inject
import io.iohk.cvp.R
import kotlin.properties.Delegates

class ProofRequestDialogFragment : DaggerDialogFragment(), OnSelectItem<Credential> {

    companion object {
        // TODO this has to be removed when the Android navigation components are implemented
        fun build(proofRequestId: Long): ProofRequestDialogFragment {
            val fragment = ProofRequestDialogFragment()
            fragment.proofRequestId = proofRequestId
            return fragment
        }
    }

    private var proofRequestId by Delegates.notNull<Long>()

    @Inject
    lateinit var viewModelFactory: ProofRequestDialogViewModelFactory

    lateinit var binding: DialogFragmentProofRequestBinding

    private val viewModel: ProofRequestDialogViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ProofRequestDialogViewModel::class.java)
    }

    private val adapter: ProofRequestDialogAdapter by lazy {
        ProofRequestDialogAdapter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_proof_request, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        // TODO find how to set this style within the app theme (R.style.AppTheme)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AlertDialogTheme)
        viewModel.fetchProofRequestInfo(proofRequestId)
    }

    private fun configureRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setObservers() {
        viewModel.showError.observe(viewLifecycleOwner, EventWrapperObserver { existError ->
            if (existError) {
                requireActivity().showErrorDialog(R.string.server_error_message)
            }
        })
        viewModel.proofRequestAccepted.observe(viewLifecycleOwner, EventWrapperObserver { accepted ->
            if (accepted) {
                val successDialog = SuccessDialog.newInstance(this, R.string.server_share_successfully)
                successDialog.show(requireActivity().supportFragmentManager, null)
                handleSyncRequest()
            }
            dismiss()
        })
        viewModel.requestedCredentials.observe(viewLifecycleOwner) {
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onSelect(item: Credential) {
        viewModel.switchCredentialSelection(item)
    }

    /*
     * TODO Momentary solution, this will be discarded when we have the appropriate data repositories in this application.
     */
    private fun handleSyncRequest() {
        (requireActivity() as? MainActivityEventHandler)?.handleEvent(MainActivityEventHandler.MainActivityEvent.SYNC_REQUEST)
    }
}