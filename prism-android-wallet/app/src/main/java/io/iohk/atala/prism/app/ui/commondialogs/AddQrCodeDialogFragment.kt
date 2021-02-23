package io.iohk.atala.prism.app.ui.commondialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerDialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.FragmentAddQrCodeDialogBinding

class AddQrCodeDialogFragment : DaggerDialogFragment(){

    companion object{
        const val KEY_REQUEST_ADD_QR = "KEY_REQUEST_ADD_QR"
        const val KEY_RESULT_CODE = "KEY_RESULT_CODE"
    }

    lateinit var binding:FragmentAddQrCodeDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_add_qr_code_dialog,container,false)
        binding.lifecycleOwner = this
        binding.confirmButton.setOnClickListener { onClickConfirmButton() }
        binding.cancelButton.setOnClickListener { onClickCancelButton() }
        return binding.root
    }

    private fun onClickConfirmButton() {
        findNavController().popBackStack()
        setFragmentResult(KEY_REQUEST_ADD_QR, bundleOf(KEY_RESULT_CODE to binding.body.text.toString()))
    }

    private fun onClickCancelButton() {
        findNavController().popBackStack()
    }
}