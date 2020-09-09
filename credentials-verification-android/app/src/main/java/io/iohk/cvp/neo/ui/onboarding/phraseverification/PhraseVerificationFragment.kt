package io.iohk.cvp.neo.ui.onboarding.phraseverification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentPhraseVerificationBinding
import io.iohk.cvp.neo.common.EventWrapperObserver
import io.iohk.cvp.utils.FirebaseAnalyticsEvents

class PhraseVerificationFragment : Fragment() {

    private lateinit var binding: NeoFragmentPhraseVerificationBinding

    private val args: PhraseVerificationFragmentArgs by navArgs()

    private val viewModel: PhraseVerificationViewModel by viewModels {
        PhraseVerificationViewModelFactory(args.mnemonicList.toList(), args.verificationIndex1, args.verificationIndex2)
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_phrase_verification, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.shouldNavigateToNextScreen.observe(viewLifecycleOwner, EventWrapperObserver {
            navigateToAccountCreated()
        })
    }

    private fun navigateToAccountCreated() {
        // log event in firebase analytics
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, FirebaseAnalyticsEvents.VERIFY_RECOVERY_PHRASE_SUCCESS)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        val extras = ActivityNavigator.Extras.Builder()
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .build()
        val action = PhraseVerificationFragmentDirections.actionPhraseVerificationFragmentToAccountCreatedActivity()
        findNavController().navigate(action, extras)
    }
}