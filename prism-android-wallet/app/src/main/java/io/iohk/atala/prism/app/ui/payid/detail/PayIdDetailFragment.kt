package io.iohk.atala.prism.app.ui.payid.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.IntentUtils
import io.iohk.atala.prism.app.neo.common.extensions.buildActivityResultLauncher
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.atala.prism.app.ui.commondialogs.SuccessDialog
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

    private val sharePayIdLauncher = buildActivityResultLauncher { activityResult ->
        SuccessDialog.Builder(requireContext())
            .setPrimaryText(R.string.fragment_pay_id_detail_share_success_dialog_text_1)
            .setSecondaryText(R.string.fragment_pay_id_detail_share_success_dialog_text_2)
            .setCustomButtonText(R.string.fragment_pay_id_detail_share_success_dialog_custom_button_text)
            .build().show(requireActivity().supportFragmentManager, null)
    }

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
        binding.shareButton.setOnClickListener { sharePayId() }
        supportActionBar?.show()
        setHasOptionsMenu(true)
        return binding.root
    }

    private fun sharePayId() {
        viewModel.payIdName.value?.let { payIdName ->
            val shareText = getString(R.string.fragment_pay_id_detail_share_pay_id_text, payIdName)
            val intent = IntentUtils.intentShareText(shareText)
            sharePayIdLauncher.launch(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish() // finish activity when back menu is pressed
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
