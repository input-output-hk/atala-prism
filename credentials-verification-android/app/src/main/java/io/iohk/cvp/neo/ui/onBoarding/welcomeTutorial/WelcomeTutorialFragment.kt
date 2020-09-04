package io.iohk.cvp.neo.ui.onBoarding.welcomeTutorial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.FirebaseAnalytics
import io.iohk.cvp.databinding.NeoFragmentWelcomeTutorialBinding
import io.iohk.cvp.R
import io.iohk.cvp.neo.common.EventWrapperObserver
import io.iohk.cvp.utils.FirebaseAnalyticsEvents

class WelcomeTutorialFragment : Fragment() {

    private lateinit var binding: NeoFragmentWelcomeTutorialBinding

    private val viewModel: WelcomeTutorialViewModel by lazy {
        ViewModelProvider(this).get(WelcomeTutorialViewModel::class.java)
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_welcome_tutorial, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureUI()
        handleBackPressed()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
    }

    private fun configureUI() {
        // Set the fragment adapter to the ViewPager2 instance
        binding.vpPager.adapter = TutorialScrollableAdapter(activity)
        // Link ViewPager2 with TabLayout (dots indicators)
        TabLayoutMediator(binding.tabDots, binding.vpPager, TabLayoutMediator.TabConfigurationStrategy { tab, position ->
            binding.vpPager.setCurrentItem(position, true)
        }).attach()
    }

    private fun setObservers() {
        viewModel.shouldGoToCreateAccount.observe(viewLifecycleOwner, EventWrapperObserver {
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.CREATE_ACCOUNT, null)
            findNavController().navigate(R.id.action_welcomeTutorialFragment_to_termsAndConditionsActivity)
        })
        viewModel.shouldReturn.observe(viewLifecycleOwner, EventWrapperObserver {
            findNavController().popBackStack()
        })
        viewModel.shouldGoToRestoreAccount.observe(viewLifecycleOwner, EventWrapperObserver {
            findNavController().navigate(R.id.action_welcomeTutorialFragment_to_restoreAccountActivity)
        })
    }

    /*
    * Registers a callback in the activity to know when the back button is pressed
    * */
    private fun handleBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            viewModel.previous()
        }
    }
}