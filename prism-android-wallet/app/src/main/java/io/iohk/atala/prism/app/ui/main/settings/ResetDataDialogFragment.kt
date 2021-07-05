package io.iohk.atala.prism.app.ui.main.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import io.iohk.atala.prism.app.neo.common.extensions.KEY_RESULT
import io.iohk.cvp.R
import io.iohk.cvp.databinding.DialogFragmentResetDataBinding

class ResetDataDialogFragment : DialogFragment() {

    companion object {
        const val REQUEST_DELETE_DATA = "REQUEST_DELETE_DATA"
    }

    private lateinit var binding: DialogFragmentResetDataBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_reset_data, container, false)
        binding.lifecycleOwner = this
        setClickActions()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO find how to set this style within the app theme (R.style.AppTheme)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AlertDialogTheme)
    }

    private fun setClickActions() {
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.resetButton.setOnClickListener {
            findNavController().popBackStack()
            setFragmentResult(REQUEST_DELETE_DATA, bundleOf(KEY_RESULT to Activity.RESULT_OK))
        }
    }
}
