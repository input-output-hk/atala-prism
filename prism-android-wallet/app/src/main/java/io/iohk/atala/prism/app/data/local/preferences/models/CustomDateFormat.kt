package io.iohk.atala.prism.app.data.local.preferences.models

import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import io.iohk.atala.prism.app.neo.common.dateFormatMMDDYYYY
import io.iohk.atala.prism.app.neo.common.dateFormatYYYYMMDD
import java.text.SimpleDateFormat

enum class CustomDateFormat(val value: Int, val title: String, val dateFormat: SimpleDateFormat) {
    DDMMYYYY(0, "Day / Month / Year", dateFormatDDMMYYYY),
    MMDDYYYY(1, "Month / Day / Year", dateFormatMMDDYYYY),
    YYYYMMDD(2, "Year / Month / Day", dateFormatYYYYMMDD)
}

fun customDateFormatFrom(value: Int): CustomDateFormat? {
    return CustomDateFormat.values().find { it.value == value }
}

fun customDateFormatFrom(value: Int, default: CustomDateFormat): CustomDateFormat {
    return CustomDateFormat.values().find { it.value == value } ?: default
}