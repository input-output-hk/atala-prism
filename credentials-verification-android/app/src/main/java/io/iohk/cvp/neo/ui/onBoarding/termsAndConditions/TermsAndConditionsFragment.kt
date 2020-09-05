package io.iohk.cvp.neo.ui.onBoarding.termsAndConditions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoFragmentTermsAndConditionBinding
import io.iohk.cvp.neo.common.EventWrapperObserver
import io.iohk.cvp.utils.Constants
import io.iohk.cvp.utils.FirebaseAnalyticsEvents

class TermsAndConditionsFragment : Fragment() {

    private lateinit var binding: NeoFragmentTermsAndConditionBinding

    private val viewModel: TermsAndConditionsViewModel by lazy {
        ViewModelProvider(this).get(TermsAndConditionsViewModel::class.java)
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_terms_and_condition, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
    }

    private fun setObservers() {
        viewModel.shouldNavigate.observe(viewLifecycleOwner, EventWrapperObserver { navigationAction ->
            when (navigationAction) {
                TermsAndConditionsViewModel.NavigationAction.TERMS_AND_CONDITIONS -> {
                    navigateTermsAndConditionsDialog()
                }
                TermsAndConditionsViewModel.NavigationAction.PRIVACY_POLICY -> {
                    navigatePrivacyPolicyDialog()
                }
                TermsAndConditionsViewModel.NavigationAction.NEXT -> {
                    firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.CONTINUE_AFTER_TC_PP, null)
                    findNavController().navigate(R.id.action_termsAndConditionsFragment_to_walletSetupActivity)
                }
            }
        })
    }

    private fun navigateTermsAndConditionsDialog() {
        val direction = TermsAndConditionsFragmentDirections
                .actionTermsAndConditionsFragmentToWebViewDialogFragment("${Constants.LEGAL_BASE_URL}${Constants.LEGAL_TERMS_AND_CONDITIONS}")
        findNavController().navigate(direction)
    }

    private fun navigatePrivacyPolicyDialog() {
        val direction = TermsAndConditionsFragmentDirections
                .actionTermsAndConditionsFragmentToWebViewDialogFragment("${Constants.LEGAL_BASE_URL}${Constants.LEGAL_PRIVACY_POLICY}")
        findNavController().navigate(direction)
    }
}