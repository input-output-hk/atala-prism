package io.iohk.atala.prism.app.ui.payid.instructions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentPayIdInstructionsBinding
import javax.inject.Inject

class PayIdInstructionsFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: PayIdInstructionsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PayIdInstructionsViewModel::class.java)
    }

    private lateinit var binding: FragmentPayIdInstructionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pay_id_instructions, container, false)
        binding.lifecycleOwner = this
        supportActionBar?.hide()
        binding.skipButton.setOnClickListener { activity?.finish() }
        binding.nextButton.setOnClickListener { findNavController().navigate(R.id.action_payIdInstructionsFragment_to_payIdSelectIdentityCredentialFragment) }
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.shouldGoToPayIdDetail.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigate(R.id.action_payIdInstructionsFragment_to_payIdDetailFragment)
            }
        }
    }
}
