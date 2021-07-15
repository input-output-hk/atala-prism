package io.iohk.atala.prism.app.ui.idverification.step2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.iohk.cvp.databinding.FragmentIdDataConfirmationBinding
import javax.inject.Inject

class IdDataConfirmationFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: IdDataConfirmationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(IdDataConfirmationViewModel::class.java)
    }

    private lateinit var binding: FragmentIdDataConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_id_data_confirmation, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.scanAgainBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.confirmButton.setOnClickListener {
            viewModel.confirmData()
        }
        supportActionBar?.show()
        setObservers()
        return binding.root
    }

    private fun setObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                requireActivity().showBlockUILoading()
            } else {
                requireActivity().hideBlockUILoading()
            }
        }

        viewModel.showError.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    requireActivity().showErrorDialog(R.string.generic_error_message)
                }
            }
        )

        viewModel.dataConfirmedCorrectly.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                if (it) {
                    goToMainActivity()
                }
            }
        )
    }

    private fun goToMainActivity() {
        val extras = ActivityNavigator.Extras.Builder()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .build()
        val action = IdDataConfirmationFragmentDirections.actionIdDataConfirmationFragmentToMainActivity()
        findNavController().navigate(action, extras)
    }
}
