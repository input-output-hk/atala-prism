package io.iohk.atala.prism.app.ui.idverification.tutorial

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.hideBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showBlockUILoading
import io.iohk.atala.prism.app.neo.common.extensions.showErrorDialog
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentIdVerificationTutorialBinding
import javax.inject.Inject

class IdVerificationTutorialFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: IdVerificationTutorialViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(IdVerificationTutorialViewModel::class.java)
    }

    lateinit var binding: FragmentIdVerificationTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            goToMainActivity()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_id_verification_tutorial, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.nextButton.setOnClickListener {
            viewModel.startkycInitializationProcess()
        }
        binding.skipButton.setOnClickListener {
            goToMainActivity()
        }
        supportActionBar?.hide()
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.errorStartingKycFlow.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    requireActivity().hideBlockUILoading()
                    requireActivity().showErrorDialog(R.string.id_verification_tutorial_initialization_error_msg)
                }
            }
        )
        viewModel.showLoading.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    requireActivity().showBlockUILoading()
                }
            }
        )
        viewModel.acuantSDKIsAlreadyInitialized.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                it?.let {
                    requireActivity().hideBlockUILoading()
                    findNavController().navigate(R.id.action_idVerificationTutorialFragment_to_idTypeSelectionFragment)
                }
            }
        )
    }

    private fun goToMainActivity() {
        val extras = ActivityNavigator.Extras.Builder()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .build()
        val action = IdVerificationTutorialFragmentDirections.actionIdVerificationTutorialFragmentToMainActivity()
        findNavController().navigate(action, extras)
    }
}
