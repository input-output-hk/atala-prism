package io.iohk.atala.prism.app.ui.main.credentials

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import com.google.protobuf.InvalidProtocolBufferException
import io.iohk.atala.prism.app.core.enums.CredentialType
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.cvp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

object CredentialUtil {
    fun getType(acceptedCredential: Credential, context: Context): String {
        return when (CredentialType.getByValue(acceptedCredential.credentialType)) {
            CredentialType.DEMO_ID_CREDENTIAL -> context.resources.getString(R.string.credential_government_type_title)
            CredentialType.DEMO_DEGREE_CREDENTIAL -> context.resources.getString(R.string.credential_degree_type_title)
            CredentialType.DEMO_EMPLOYMENT_CREDENTIAL -> context.resources.getString(R.string.credential_employed_type_title)
            CredentialType.DEMO_INSURANCE_CREDENTIAL -> context.resources.getString(R.string.credential_insurance_type_title)
            CredentialType.ETHIOPIA_NATIONAL_ID, CredentialType.GEORGIA_NATIONAL_ID -> context.resources.getString(R.string.credential_georgia_national_id_type_title)
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE, CredentialType.GEORGIA_EDUCATIONAL_DEGREE -> context.resources.getString(
                R.string.credential_georgia_educational_degree_type_title
            )
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT, CredentialType.GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT -> context.resources.getString(
                R.string.credential_georgia_educational_degree_transcript_type_title
            )
            CredentialType.KYC_CREDENTIAL -> context.resources.getString(R.string.credential_kyc_type_title)
            CredentialType.UNKNOWN -> ""
        }
    }

    fun getName(credential: Credential, context: Context): String {
        return context.resources.getString(getNameResource(credential))
    }

    fun getNameResource(credential: Credential): Int {
        return getNameResource(credential.credentialType ?: "")
    }

    fun getNameResource(credentialType: String): Int {
        return when (CredentialType.getByValue(credentialType)) {
            CredentialType.DEMO_ID_CREDENTIAL -> R.string.credential_government_name
            CredentialType.DEMO_DEGREE_CREDENTIAL -> R.string.credential_degree_name
            CredentialType.DEMO_EMPLOYMENT_CREDENTIAL -> R.string.credential_employment_name
            CredentialType.DEMO_INSURANCE_CREDENTIAL -> R.string.credential_insurance_name
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT, CredentialType.GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT -> R.string.credential_georgia_educational_degree_transcript_name
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE, CredentialType.GEORGIA_EDUCATIONAL_DEGREE -> R.string.credential_georgia_educational_degree_name
            CredentialType.ETHIOPIA_NATIONAL_ID, CredentialType.GEORGIA_NATIONAL_ID -> R.string.credential_georgia_national_id_name
            CredentialType.KYC_CREDENTIAL -> R.string.credential_kyc
            CredentialType.UNKNOWN -> R.string.credential_unknown
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getLogo(credentialType: String, context: Context): Drawable? {
        return when (CredentialType.getByValue(credentialType)) {
            CredentialType.DEMO_ID_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_government, null)
            CredentialType.DEMO_DEGREE_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_university, null)
            CredentialType.DEMO_EMPLOYMENT_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_proof, null)
            CredentialType.DEMO_INSURANCE_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_insurance, null)
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE, CredentialType.GEORGIA_EDUCATIONAL_DEGREE -> context.resources.getDrawable(
                R.mipmap.ic_educational_degree,
                null
            )
            CredentialType.ETHIOPIA_NATIONAL_ID, CredentialType.GEORGIA_NATIONAL_ID -> context.resources.getDrawable(
                R.mipmap.ic_national_id,
                null
            )
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT, CredentialType.GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT -> context.resources.getDrawable(
                R.mipmap.ic_educational_degree_transcript,
                null
            )
            CredentialType.KYC_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_government, null)
            CredentialType.UNKNOWN -> null
        }
    }

    /*
    * Logic to get the HTML String
    * */
    suspend fun getHtmlString(credentialData: CredentialWithEncodedCredential): String? =
        withContext(Dispatchers.Main) {
            return@withContext getHtmlStringFromPlainTextCredential(credentialData.encodedCredential)
        }

    private fun getHtmlStringFromPlainTextCredential(encodedCredential: EncodedCredential): String? {
        try {
            val atalaMessage = AtalaMessage.parseFrom(encodedCredential.credentialEncoded)
            val plainTextCredential = atalaMessage.plainCredential
            val encodedCredential = plainTextCredential.encodedCredentialBytes.toStringUtf8()
            val plainTextCredentialJson = CredentialMapper.parsePlainTextCredential(encodedCredential)
            return plainTextCredentialJson.html
        } catch (ex: InvalidProtocolBufferException) {
            ex.printStackTrace()
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
        return null
    }
}
