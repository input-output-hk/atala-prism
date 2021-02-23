package io.iohk.atala.prism.app.neo.ui.onboarding.phraseverification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.lowerCaseInputFilter
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentPhraseVerificationBinding
import javax.inject.Inject

class PhraseVerificationFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: NeoFragmentPhraseVerificationBinding

    private val args: PhraseVerificationFragmentArgs by navArgs()

    private val viewModel: PhraseVerificationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PhraseVerificationViewModel::class.java).apply {
            setArguments(args.mnemonicList.toList(), args.verificationIndex1, args.verificationIndex2)
        }
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_phrase_verification, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        configureInputsViews()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // show soft keyboard
        requireContext()
            .getSystemService(InputMethodManager::class.java)
            .showSoftInput(binding.editText1, InputMethodManager.SHOW_FORCED)
    }

    private fun setObservers() {
        viewModel.shouldNavigateToNextScreen.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                navigateToAccountCreated()
            }
        )
    }

    private fun navigateToAccountCreated() {
        // log event in firebase analytics
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, FirebaseAnalyticsEvents.VERIFY_RECOVERY_PHRASE_SUCCESS)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        findNavController().navigate(R.id.action_phraseVerificationFragment_to_accountCreatedFragment)
    }

    private fun configureInputsViews() {
        // cast all text to lowercase
        binding.editText1.filters = arrayOf(lowerCaseInputFilter)
        binding.editText2.filters = arrayOf(lowerCaseInputFilter)
    }
}
