package io.iohk.atala.prism.app.ui.main.credentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.cvp.R
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.cvp.databinding.NeoFragmentMyCredentialsBinding
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.ui.utils.adapters.CredentialsAdapter
import javax.inject.Inject

class MyCredentialsFragment : DaggerFragment(), OnSelectItem<Credential> {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel:MyCredentialsViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MyCredentialsViewModel::class.java)
    }

    private lateinit var binding: NeoFragmentMyCredentialsBinding

    private val credentialsAdapter = CredentialsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_my_credentials, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.filteredCredentials.observe(viewLifecycleOwner, Observer {
            credentialsAdapter.clear()
            credentialsAdapter.addAll(it)
            credentialsAdapter.notifyDataSetChanged()
        })
    }

    private fun configureRecyclerView() {
        binding.credentialsList.adapter = credentialsAdapter
        binding.credentialsList.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onSelect(item: Credential) {
        val direction = MyCredentialsFragmentDirections
                .actionMyCredentialsFragmentToCredentialDetailNavigation(item.credentialId)
        findNavController().navigate(direction)
    }
}