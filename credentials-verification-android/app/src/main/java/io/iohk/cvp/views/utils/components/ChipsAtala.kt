package io.iohk.cvp.views.utils.components

import android.app.ActionBar
import android.content.Context
import com.google.android.material.chip.Chip
import io.iohk.cvp.R
import io.iohk.cvp.utils.DimensionsUtils

class ChipsAtala(context: Context) : Chip(context) {

    companion object {
        private const val CHIP_LEFT_MARGIN_DPS: Float = 8F
    }

    init {
        this.closeIcon = resources.getDrawable(R.drawable.chip_close_icon, null)
        this.closeIconTint = resources.getColorStateList(R.color.atala_red_text_color, null)
        this.setTextAppearanceResource(R.style.chip_text_style)
        this.chipBackgroundColor = resources.getColorStateList(R.color.chip_bg_color, null)
        val params = ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        params.setMargins(left, top, DimensionsUtils.densityPointsToPixels(context, CHIP_LEFT_MARGIN_DPS), bottom)
        this.layoutParams = params
        this.isCloseIconVisible = true
        this.isClickable = true
        this.isCheckable = false
    }

}