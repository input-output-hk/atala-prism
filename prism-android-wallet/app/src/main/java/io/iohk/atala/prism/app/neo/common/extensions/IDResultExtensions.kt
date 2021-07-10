package io.iohk.atala.prism.app.neo.common.extensions

import android.annotation.SuppressLint
import com.acuant.acuantdocumentprocessing.model.IDResult
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
val dateFormatMMDDYYYYAcuant = SimpleDateFormat("MM-dd-YYYY")

fun IDResult.documentIssueDate(): String? {
    this.dataFields.dataFields.first {
        it.key.contains("Issue Date", true)
    }?.let { dataField ->
        // parse Microsoft Json Date Format /Date(1325134800000data
        dataField.value.substring(6, dataField.value.length - 2).toLongOrNull()?.let {
            return dateFormatMMDDYYYYAcuant.format(Date(it))
        }
    }
    return null
}

fun IDResult.documentNumber(): String? {
    this.dataFields.dataFields.firstOrNull {
        it.key.contains("Document Number", true)
    }?.let {
        return it.value
    }
    this.dataFields.dataFields.firstOrNull {
        it.key.contains("Registration Number", true)
    }?.let {
        return it.value
    }
    return null
}

fun IDResult.getGender(): String = if (biographic.gender == 1) "Male" else "Female"
