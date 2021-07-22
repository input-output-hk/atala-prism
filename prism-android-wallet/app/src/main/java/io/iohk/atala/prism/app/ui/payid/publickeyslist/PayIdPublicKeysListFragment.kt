package io.iohk.atala.prism.app.ui.payid.publickeyslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.OnSelectItem
import io.iohk.atala.prism.app.neo.common.extensions.copyToClipBoard
import io.iohk.atala.prism.app.ui.utils.adapters.AddressOrPublicKeyListAdapter
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentPayIdPublicKeysListBinding
import javax.inject.Inject

class PayIdPublicKeysListFragment : DaggerFragment(), OnSelectItem<String> {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: PayIdPublicKeysListViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PayIdPublicKeysListViewModel::class.java)
    }

    private lateinit var binding: FragmentPayIdPublicKeysListBinding

    private val listAdapter = AddressOrPublicKeyListAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPayIdPublicKeysListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setupRecyclerView()
        setObservers()
        viewModel.loadPayIdData()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecyclerView.adapter = listAdapter
    }

    private fun setObservers() {
        viewModel.publicKeys.observe(viewLifecycleOwner) { publicKey ->
            listAdapter.clear()
            listAdapter.addAll(publicKey.map { it.publicKey })
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onSelect(item: String) {
        if (requireContext().copyToClipBoard("", item)) {
            Toast.makeText(requireContext(), R.string.fragment_pay_id_address_list_address_on_clipboard, Toast.LENGTH_SHORT).show()
        }
    }
}
