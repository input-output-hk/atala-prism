package io.iohk.cvp.neo.ui.commonDialogs

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.NeoDialogFragmentWebViewBinding

class WebViewDialogFragment : BottomSheetDialogFragment() {

    companion object {
        private const val TOP_MARGIN_IN_DP = 168
    }

    private val args: WebViewDialogFragmentArgs by navArgs()

    private lateinit var binding: NeoDialogFragmentWebViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_dialog_fragment_web_view, container, false)
        binding.lifecycleOwner = this
        configureViews()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            // Setting top margin
            val displayMetrics = Resources.getSystem().displayMetrics
            binding.dialogContainer.layoutParams.height = displayMetrics.heightPixels - (TOP_MARGIN_IN_DP.toFloat() * displayMetrics.scaledDensity).toInt()
            dialog.behavior.peekHeight = binding.dialogContainer.layoutParams.height
        }
        return dialog
    }

    private fun configureViews() {
        binding.closeBtn.setOnClickListener {
            dismiss()
        }
        binding.webView.loadUrl(args.url)
    }
}