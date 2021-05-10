package io.iohk.atala.prism.app.data.local.preferences.models

import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYYSimple
import io.iohk.atala.prism.app.neo.common.dateFormatMMDDYYYY
import io.iohk.atala.prism.app.neo.common.dateFormatMMDDYYYYSimple
import io.iohk.atala.prism.app.neo.common.dateFormatYYYYMMDD
import io.iohk.atala.prism.app.neo.common.dateFormatYYYYMMDDSimple
import java.text.SimpleDateFormat

enum class CustomDateFormat(val value: Int, val title: String, val dateFormat: SimpleDateFormat, val dateFormatSimple: SimpleDateFormat) {
    DDMMYYYY(0, "Day / Month / Year", dateFormatDDMMYYYY, dateFormatDDMMYYYYSimple),
    MMDDYYYY(1, "Month / Day / Year", dateFormatMMDDYYYY, dateFormatMMDDYYYYSimple),
    YYYYMMDD(2, "Year / Month / Day", dateFormatYYYYMMDD, dateFormatYYYYMMDDSimple)
}

fun customDateFormatFrom(value: Int): CustomDateFormat? {
    return CustomDateFormat.values().find { it.value == value }
}

fun customDateFormatFrom(value: Int, default: CustomDateFormat): CustomDateFormat {
    return CustomDateFormat.values().find { it.value == value } ?: default
}
