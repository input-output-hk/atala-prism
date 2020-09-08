package io.iohk.cvp.neo.ui.onboarding.walletsetup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.analytics.FirebaseAnalytics
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentWalletSetupBinding
import io.iohk.cvp.neo.common.EventWrapperObserver
import io.iohk.cvp.utils.FirebaseAnalyticsEvents
import io.iohk.cvp.views.activities.SeedPhraseVerificationActivity
import java.util.*

class WalletSetupFragment : Fragment() {

    private val viewModel: WalletSetupViewModel by viewModels { WalletSetupViewModelFactory }

    private lateinit var binding: NeoFragmentWalletSetupBinding

    private val mnemonicAdapter: MnemonicAdapter by lazy {
        MnemonicAdapter(requireContext(), R.layout.neo_row_mnemonic_word, R.id.text_view_seed)
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_wallet_setup, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        // configure words grid adapter
        binding.gridViewSeedPhrase.adapter = mnemonicAdapter
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.mnemonicList.observe(viewLifecycleOwner, Observer {
            // update words adapter
            mnemonicAdapter.clear()
            mnemonicAdapter.addAll(it)
        })
        viewModel.shouldGoToNextScreen.observe(viewLifecycleOwner, EventWrapperObserver { list ->
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.ACCEPT_RECOVERY_PHRASE_CONTINUE, null)
            navigateToPhraseVerification(list, viewModel.userVerificationWordsIndexes[0], viewModel.userVerificationWordsIndexes[1])
        })
    }

    private fun navigateToPhraseVerification(mnemonicList: List<String>, firstIndex: Int, secondIndex: Int) {
        val intent = Intent(requireActivity(), SeedPhraseVerificationActivity::class.java)
        val bundle = Bundle()
        bundle.putStringArray(SeedPhraseVerificationActivity.SEED_PHRASE_KEY, Arrays.copyOf<String, Any>(mnemonicList.toTypedArray(),
                mnemonicList.size,
                Array<String>::class.java))
        bundle.putInt(SeedPhraseVerificationActivity.FIRST_WORD_INDEX_KEY, firstIndex + 1)
        bundle.putInt(SeedPhraseVerificationActivity.SECOND_WORD_INDEX_KEY, secondIndex + 1)
        intent.putExtras(bundle)
        requireActivity().startActivity(intent)
    }
}