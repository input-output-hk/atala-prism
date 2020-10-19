package io.iohk.atala.prism.app.views.utils

import android.text.InputFilter

class EditTextFiltersUtils {
    companion object {
        fun characterFilter(characterToFilter: String): InputFilter {
            return InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    if (Character.isWhitespace(source[i])) {
                        return@InputFilter characterToFilter
                    }
                }
                null
            }
        }
    }
}