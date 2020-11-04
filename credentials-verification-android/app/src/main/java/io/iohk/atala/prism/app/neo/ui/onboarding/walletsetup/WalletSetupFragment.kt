package io.iohk.atala.prism.app.neo.ui.onboarding.walletsetup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentWalletSetupBinding
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver

class WalletSetupFragment : Fragment() {

    private val viewModel: WalletSetupViewModel by viewModels { WalletSetupViewModelFactory }

    private lateinit var binding: NeoFragmentWalletSetupBinding

    private val mnemonicAdapter: MnemonicAdapter by lazy { MnemonicAdapter() }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_wallet_setup, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        // configure words recyclerView adapter

        val layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewSeedPhrase.layoutManager = layoutManager
        binding.recyclerViewSeedPhrase.adapter = mnemonicAdapter
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.mnemonicList.observe(viewLifecycleOwner, Observer {
            // update words adapter
            mnemonicAdapter.clear()
            mnemonicAdapter.addAll(it)
            mnemonicAdapter.notifyDataSetChanged()
        })
        viewModel.shouldGoToNextScreen.observe(viewLifecycleOwner, EventWrapperObserver { list ->
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.ACCEPT_RECOVERY_PHRASE_CONTINUE, null)
            navigateToPhraseVerification(list, viewModel.userVerificationWordsIndexes[0], viewModel.userVerificationWordsIndexes[1])
        })
    }

    private fun navigateToPhraseVerification(mnemonicList: List<String>, firstIndex: Int, secondIndex: Int) {
        val action = WalletSetupFragmentDirections
                .actionWalletSetupFragmentToPhraseVerificationFragment(mnemonicList.toTypedArray(), firstIndex, secondIndex)
        findNavController().navigate(action)
    }
}