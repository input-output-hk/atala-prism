package io.iohk.atala.prism.app.ui.idverification.step1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentIdTypeSelectionBinding

class IdTypeSelectionFragment : Fragment() {

    private lateinit var binding: FragmentIdTypeSelectionBinding

    private val viewModel: IdTypeSelectionViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_id_type_selection, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        setObservers()
        supportActionBar?.show()
        return binding.root
    }

    private fun setObservers() {
        viewModel.shouldContinue.observe(
            viewLifecycleOwner,
            EventWrapperObserver {
                it?.let {
                    // TODO to implement
                }
            }
        )
    }
}
