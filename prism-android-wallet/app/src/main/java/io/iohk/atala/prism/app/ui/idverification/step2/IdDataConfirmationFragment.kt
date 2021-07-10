package io.iohk.atala.prism.app.ui.idverification.step2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.iohk.atala.prism.app.neo.common.extensions.supportActionBar
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentIdDataConfirmationBinding

class IdDataConfirmationFragment : Fragment() {

    private lateinit var binding: FragmentIdDataConfirmationBinding

    private val args: IdDataConfirmationFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_id_data_confirmation, container, false)
        binding.lifecycleOwner = this
        binding.scanAgainBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        // TODO This information has to be managed by a data repository, in the following tickets that will be added with an appropriate data model
        binding.fullName = args.fullName
        binding.gender = args.gender
        binding.nationality = args.nationality
        binding.nationalIdNumber = args.nationalIdNumber
        binding.birthDate = args.birthDate
        binding.issueDate = args.issueDate
        binding.expirationDate = args.expirationDate
        supportActionBar?.show()
        return binding.root
    }
}
