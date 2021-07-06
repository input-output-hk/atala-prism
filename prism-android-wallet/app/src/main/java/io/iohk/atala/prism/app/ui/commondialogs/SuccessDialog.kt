package io.iohk.atala.prism.app.ui.commondialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.iohk.cvp.R
import io.iohk.cvp.databinding.DialogSuccessBinding

class SuccessDialog : DialogFragment() {

    class Builder(private val context: Context) {
        private val dialog = SuccessDialog()

        fun setPrimaryText(resId: Int): Builder = setPrimaryText(context.getString(resId))

        fun setPrimaryText(text: String): Builder {
            dialog.primaryText = text
            return this
        }

        fun setSecondaryText(resId: Int): Builder = setSecondaryText(context.getString(resId))

        fun setSecondaryText(text: String): Builder {
            dialog.secondaryText = text
            return this
        }

        fun setCustomButtonText(resId: Int): Builder = setCustomButtonText(context.getString(resId))

        fun setCustomButtonText(text: String): Builder {
            dialog.customButtonText = text
            return this
        }

        fun setOkButtonClickLister(listener: View.OnClickListener): Builder {
            dialog.okButtonClickListener = listener
            return this
        }

        fun build(): SuccessDialog = dialog
    }

    private lateinit var binding: DialogSuccessBinding

    private var primaryText: String? = null

    private var secondaryText: String? = null

    private var customButtonText: String? = null

    private var okButtonClickListener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AlertDialogTheme) // add App custom style
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = false
        binding = DialogSuccessBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.primaryText = primaryText
        binding.secondaryText = secondaryText
        binding.customButtonText = customButtonText
        binding.okButton.setOnClickListener {
            dismiss()
            okButtonClickListener?.onClick(it)
        }
        dialog?.window?.setBackgroundDrawableResource(R.drawable.rounded_corner_white)
        return binding.root
    }
}
