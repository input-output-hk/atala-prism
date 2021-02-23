package io.iohk.atala.prism.app.neo.common

import android.text.InputFilter

val lowerCaseInputFilter: InputFilter = InputFilter { source, start, end, _, _, _ ->
    source.subSequence(start, end).toString().toLowerCase()
}
