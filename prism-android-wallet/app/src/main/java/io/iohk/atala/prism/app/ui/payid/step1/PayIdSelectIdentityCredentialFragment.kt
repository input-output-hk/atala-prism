package io.iohk.atala.prism.app.ui.payid.step1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentPayIdSelectIdentityCredentialBinding
import javax.inject.Inject

class PayIdSelectIdentityCredentialFragment : DaggerFragment(), OnSelectItem<CheckableData<Credential>> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: PayIdSelectIdentityCredentialViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PayIdSelectIdentityCredentialViewModel::class.java)
    }

    private lateinit var binding: FragmentPayIdSelectIdentityCredentialBinding

    private val credentialsAdapter: IdentityCredentialsAdapter by lazy {
        IdentityCredentialsAdapter(this)
    }

    private val identityCredentialsHeaderText: String by lazy {
        getString(R.string.fragment_pay_id_select_identity_credential_instructions)
    }

    private val otherCredentialsHeaderText: String by lazy {
        getString(R.string.fragment_pay_id_select_identity_credential_optionals_instructions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pay_id_select_identity_credential, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        viewModel.loadCredentials()
        supportActionBar?.show()
        setViewListeners()
        return binding.root
    }

    private fun configureRecyclerView() {
        binding.credentialsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.credentialsRecyclerView.adapter = credentialsAdapter
    }

    override fun onSelect(item: CheckableData<Credential>) {
        viewModel.selectCredential(item)
    }

    private fun setObservers() {
        viewModel.identityCredentials.observe(viewLifecycleOwner) { rebuiltCredentialsList() }
        viewModel.othersCredentials.observe(viewLifecycleOwner) { rebuiltCredentialsList() }
        viewModel.shouldContinue.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                findNavController().navigate(R.id.action_payIdSelectIdentityCredentialFragment_to_payIdSetupFormFragment)
            }
        )
    }

    private fun rebuiltCredentialsList() {
        val map = HashMap<String, List<CheckableData<Credential>>>()
        map[identityCredentialsHeaderText] = viewModel.identityCredentials.value?.toList() ?: listOf()
        map[otherCredentialsHeaderText] = viewModel.othersCredentials.value?.toList() ?: listOf()
        credentialsAdapter.updateAllContent(map)
    }

    private fun setViewListeners() {
        binding.verifyIdButton.setOnClickListener {
            // Navigate to Acuant verification flow
            val extras = ActivityNavigator.Extras.Builder()
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .build()
            val action = PayIdSelectIdentityCredentialFragmentDirections.actionPayIdSelectIdentityCredentialFragmentToIdVerificationNavActivity3()
            findNavController().navigate(action, extras)
        }
    }
}
