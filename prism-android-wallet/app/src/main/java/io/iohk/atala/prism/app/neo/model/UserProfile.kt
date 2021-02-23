package io.iohk.atala.prism.app.neo.model

import android.graphics.Bitmap

data class UserProfile(
    var name: String?,
    var country: String?,
    var email: String?,
    var profileImage: Bitmap?
)
