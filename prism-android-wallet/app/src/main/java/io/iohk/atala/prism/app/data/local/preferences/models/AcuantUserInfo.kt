package io.iohk.atala.prism.app.data.local.preferences.models

import android.graphics.Bitmap

data class AcuantUserInfo(
    var fullName: String,
    var gender: Int,
    var countryCode: String,
    var documentNumber: String,
    var birthDate: String,
    var documentIssueDate: String,
    var documentExpirationDate: String,
    var documentFrontImage: Bitmap,
    var documentBackImage: Bitmap,
    var documentFaceImage: Bitmap,
    var selfieImage: Bitmap,
    var instanceId: String,
    var isConfirmed: Boolean = false // should be true when a message is sent with the id of the acuant instance and the image of the selfie through the connector
) {
    fun recycleImages() {
        selfieImage.recycle()
        documentFaceImage.recycle()
        documentBackImage.recycle()
        documentFrontImage.recycle()
    }
}
