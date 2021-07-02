package io.iohk.atala.prism.app.ui.payid.instructions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentPayIdInstructionsBinding

class PayIdInstructionsFragment : Fragment() {

    private lateinit var binding: FragmentPayIdInstructionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pay_id_instructions, container, false)
        binding.lifecycleOwner = this
        supportActionBar?.hide()
        binding.skipButton.setOnClickListener { activity?.finish() }
        return binding.root
    }
}
