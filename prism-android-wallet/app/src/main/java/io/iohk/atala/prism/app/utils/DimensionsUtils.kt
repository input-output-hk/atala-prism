package io.iohk.atala.prism.app.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

class DimensionsUtils {

    companion object {
        fun densityPointsToPixels(context: Context, sizeInDps: Float): Int {
            val r: Resources = context.resources
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeInDps,
                r.displayMetrics
            ).toInt()
        }
    }
}
