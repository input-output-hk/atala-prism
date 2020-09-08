package io.iohk.cvp.neo.ui.onboarding.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.iohk.cvp.databinding.NeoFragmentWelcomeBinding
import io.iohk.cvp.R

class WelcomeFragment : Fragment() {

    private lateinit var binding: NeoFragmentWelcomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_fragment_welcome, container, false)
        binding.lifecycleOwner = this
        setViewListeners()
        return binding.root
    }

    private fun setViewListeners() {
        binding.getStartedButton.setOnClickListener {
            navigateToNext()
        }
    }

    private fun navigateToNext() {
        findNavController().navigate(R.id.action_welcomeFragment_to_welcomeTutorialFragment)
    }
}