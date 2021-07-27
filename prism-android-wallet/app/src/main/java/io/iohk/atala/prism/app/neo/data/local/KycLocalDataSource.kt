package io.iohk.atala.prism.app.neo.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.KycRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.KycRequest
import io.iohk.atala.prism.app.data.local.preferences.models.AcuantUserInfo
import io.iohk.atala.prism.app.neo.common.extensions.Bitmap
import io.iohk.atala.prism.app.neo.common.extensions.toEncodedBase64String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class KycLocalDataSource(private val kycRequestDao: KycRequestDao, private val contactDao: ContactDao, context: Context) : KycLocalDataSourceInterface, BaseLocalDataSource(context) {

    companion object {
        private const val KYC_CONNECTION_ID = "kyc_connection_id"
        private const val ACUANT_USER_INFO_FULL_NAME = "acuant_user_info_full_name"
        private const val ACUANT_USER_INFO_GENDER = "acuant_user_info_gender"
        private const val ACUANT_USER_INFO_COUNTRY_CODE = "acuant_user_info_country_code"
        private const val ACUANT_USER_INFO_DOCUMENT_NUMBER = "acuant_user_info_document_number"
        private const val ACUANT_USER_INFO_BIRTH_DATE = "acuant_user_info_birth_date"
        private const val ACUANT_USER_INFO_DOCUMENT_ISSUE_DATE = "acuant_user_info_document_issue_date"
        private const val ACUANT_USER_INFO_DOCUMENT_EXPIRATION_DATE = "acuant_user_info_document_expiration_date"
        private const val ACUANT_USER_INFO_DOCUMENT_FRONT_IMAGE = "acuant_user_info_document_front_image"
        private const val ACUANT_USER_INFO_DOCUMENT_BACK_IMAGE = "acuant_user_info_document_back_image"
        private const val ACUANT_USER_INFO_DOCUMENT_FACE_IMAGE = "acuant_user_info_document_face_image"
        private const val ACUANT_USER_INFO_SELFIE_IMAGE = "acuant_user_info_selfie_image"
        private const val ACUANT_USER_INFO_INSTANCE_ID = "acuant_user_info_instance_id"
        private const val ACUANT_USER_INFO_IS_CONFIRMED = "acuant_user_info_is_confirmed"
    }

    override suspend fun storeKycRequest(kycRequest: KycRequest) {
        return withContext(Dispatchers.IO) {
            kycRequestDao.insertSync(kycRequest)
        }
    }

    override fun kycRequestAsync(): LiveData<KycRequest?> = kycRequestDao.first()

    override suspend fun kycRequestSync(): KycRequest? = withContext(Dispatchers.IO) {
        return@withContext kycRequestDao.firstSync()
    }

    override suspend fun kycContact(): Contact? {
        preferences.getString(KYC_CONNECTION_ID, null)?.let {
            return contactDao.getContactByConnectionId(it)
        }
        return null
    }

    override suspend fun storeKycContact(contact: Contact) {
        withContext(Dispatchers.IO) {
            contactDao.insert(contact)
            val editor = preferences.edit()
            editor.putString(KYC_CONNECTION_ID, contact.connectionId)
            editor.apply()
        }
    }

    override suspend fun storeAcuantUserInfo(userInfo: AcuantUserInfo) {
        withContext(Dispatchers.IO) {
            val editor = preferences.edit()
            editor.putString(ACUANT_USER_INFO_FULL_NAME, userInfo.fullName)
            editor.putInt(ACUANT_USER_INFO_GENDER, userInfo.gender)
            editor.putString(ACUANT_USER_INFO_COUNTRY_CODE, userInfo.countryCode)
            editor.putString(ACUANT_USER_INFO_DOCUMENT_NUMBER, userInfo.documentNumber)
            editor.putString(ACUANT_USER_INFO_BIRTH_DATE, userInfo.birthDate)
            editor.putString(ACUANT_USER_INFO_DOCUMENT_ISSUE_DATE, userInfo.documentIssueDate)
            editor.putString(ACUANT_USER_INFO_DOCUMENT_EXPIRATION_DATE, userInfo.documentExpirationDate)
            editor.putString(ACUANT_USER_INFO_DOCUMENT_FRONT_IMAGE, userInfo.documentFrontImage.toEncodedBase64String())
            editor.putString(ACUANT_USER_INFO_DOCUMENT_BACK_IMAGE, userInfo.documentBackImage.toEncodedBase64String())
            editor.putString(ACUANT_USER_INFO_DOCUMENT_FACE_IMAGE, userInfo.documentFaceImage.toEncodedBase64String())
            editor.putString(ACUANT_USER_INFO_SELFIE_IMAGE, userInfo.selfieImage.toEncodedBase64String())
            editor.putString(ACUANT_USER_INFO_INSTANCE_ID, userInfo.instanceId)
            editor.putBoolean(ACUANT_USER_INFO_IS_CONFIRMED, userInfo.isConfirmed)
            editor.apply()
        }
    }

    override suspend fun acuantUserInfo(): AcuantUserInfo? = withContext(Dispatchers.IO) {
        try {
            return@withContext AcuantUserInfo(
                preferences.getString(ACUANT_USER_INFO_FULL_NAME, null)!!,
                preferences.getInt(ACUANT_USER_INFO_GENDER, -1)!!,
                preferences.getString(ACUANT_USER_INFO_COUNTRY_CODE, null)!!,
                preferences.getString(ACUANT_USER_INFO_DOCUMENT_NUMBER, null)!!,
                preferences.getString(ACUANT_USER_INFO_BIRTH_DATE, null)!!,
                preferences.getString(ACUANT_USER_INFO_DOCUMENT_ISSUE_DATE, null)!!,
                preferences.getString(ACUANT_USER_INFO_DOCUMENT_EXPIRATION_DATE, null)!!,
                Bitmap(preferences.getString(ACUANT_USER_INFO_DOCUMENT_FRONT_IMAGE, "")!!)!!,
                Bitmap(preferences.getString(ACUANT_USER_INFO_DOCUMENT_BACK_IMAGE, "")!!)!!,
                Bitmap(preferences.getString(ACUANT_USER_INFO_DOCUMENT_FACE_IMAGE, "")!!)!!,
                Bitmap(preferences.getString(ACUANT_USER_INFO_SELFIE_IMAGE, "")!!)!!,
                preferences.getString(ACUANT_USER_INFO_INSTANCE_ID, null)!!,
                preferences.getBoolean(ACUANT_USER_INFO_IS_CONFIRMED, false)
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext null
        }
    }

    override fun kycCredential(): LiveData<Credential?> = kycRequestDao.kycCredential()
}
