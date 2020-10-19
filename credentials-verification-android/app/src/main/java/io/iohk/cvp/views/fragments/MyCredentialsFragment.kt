package io.iohk.cvp.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.iohk.cvp.R
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.databinding.NeoFragmentMyCredentialsBinding
import io.iohk.cvp.neo.common.OnSelectItem
import io.iohk.cvp.viewmodel.MyCredentialsViewModel
import io.iohk.cvp.viewmodel.MyCredentialsViewModelFactory
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator
import io.iohk.cvp.views.fragments.utils.RootAppBar
import io.iohk.cvp.views.utils.adapters.CredentialsAdapter
import javax.inject.Inject

class MyCredentialsFragment : CvpFragment<MyCredentialsViewModel>(), OnSelectItem<Credential> {

    @Inject
    lateinit var viewModelFactory: MyCredentialsViewModelFactory

    private lateinit var binding: NeoFragmentMyCredentialsBinding

    private val credentialsAdapter = CredentialsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, viewId, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = getViewModel()
        configureRecyclerView()
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        getViewModel().filteredCredentials.observe(viewLifecycleOwner, Observer {
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
        // TODO This is logic inherited from old code we need to use the navigation components
        val credentialFragment = CredentialDetailFragment()
        credentialFragment.setCredential(item)
        credentialFragment.setCredentialIsNew(false)
        navigator.showFragmentOnTop(
                requireActivity().supportFragmentManager, credentialFragment)
    }

    override fun getViewModel(): MyCredentialsViewModel {
        return ViewModelProvider(this, viewModelFactory).get(MyCredentialsViewModel::class.java)
    }

    override fun getAppBarConfigurator(): AppBarConfigurator {
        return RootAppBar(R.string.home_title)
    }

    override fun getViewId(): Int {
        return R.layout.neo_fragment_my_credentials
    }
}