package io.iohk.atala.prism.app.ui.payid.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentPayIdDetailBinding
import javax.inject.Inject

class PayIdDetailFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: PayIdDetailViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PayIdDetailViewModel::class.java)
    }

    private lateinit var binding: FragmentPayIdDetailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPayIdDetailBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.loadPayIdData()
        binding.payIdAddressListButton.setOnClickListener {
            findNavController().navigate(R.id.action_payIdDetailFragment_to_payIdAddressListFragment)
        }
        binding.addAddressButton.setOnClickListener {
            findNavController().navigate(R.id.action_payIdDetailFragment_to_addAddressFormDialogFragment)
        }
        return binding.root
    }
}
