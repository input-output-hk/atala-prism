package io.iohk.atala.prism.app.ui.main.credentials

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
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
import org.json.JSONObject

object CredentialUtil {
    fun getType(acceptedCredential: Credential, context: Context): String {
        val credentialTypeOptional = CredentialType.getByValue(acceptedCredential.credentialType)
        return if (credentialTypeOptional.isPresent) {
            when (credentialTypeOptional.get()) {
                CredentialType.DEMO_ID_CREDENTIAL -> context.resources.getString(R.string.credential_government_type_title)
                CredentialType.DEMO_DEGREE_CREDENTIAL -> context.resources.getString(R.string.credential_degree_type_title)
                CredentialType.DEMO_EMPLOYMENT_CREDENTIAL -> context.resources.getString(R.string.credential_employed_type_title)
                CredentialType.DEMO_INSURANCE_CREDENTIAL -> context.resources.getString(R.string.credential_insurance_type_title)
                CredentialType.ETHIOPIA_NATIONAL_ID, CredentialType.GEORGIA_NATIONAL_ID -> context.resources.getString(R.string.credential_georgia_national_id_type_title)
                CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE, CredentialType.GEORGIA_EDUCATIONAL_DEGREE -> context.resources.getString(R.string.credential_georgia_educational_degree_type_title)
                CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT, CredentialType.GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT -> context.resources.getString(R.string.credential_georgia_educational_degree_transcript_type_title)
                else -> ""
            }
        } else ""
    }

    fun getName(credential: Credential, context: Context): String {
        return context.resources.getString(getNameResource(credential))
    }

    fun getNameResource(credential: Credential): Int {
        return getNameResource(credential.credentialType)
    }

    fun getNameResource(credentialType: String?): Int {
        val credentialTypeOptional = CredentialType.getByValue(credentialType)
        return if (credentialTypeOptional.isPresent) {
            when (credentialTypeOptional.get()) {
                CredentialType.DEMO_ID_CREDENTIAL -> R.string.credential_government_name
                CredentialType.DEMO_DEGREE_CREDENTIAL -> R.string.credential_degree_name
                CredentialType.DEMO_EMPLOYMENT_CREDENTIAL -> R.string.credential_employment_name
                CredentialType.DEMO_INSURANCE_CREDENTIAL -> R.string.credential_insurance_name
                CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT, CredentialType.GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT -> R.string.credential_georgia_educational_degree_transcript_name
                CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE, CredentialType.GEORGIA_EDUCATIONAL_DEGREE -> R.string.credential_georgia_educational_degree_name
                CredentialType.ETHIOPIA_NATIONAL_ID, CredentialType.GEORGIA_NATIONAL_ID -> R.string.credential_georgia_national_id_name
                else -> 0
            }
        } else 0
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getLogo(credentialType: String?, context: Context): Drawable? {
        return when (CredentialType.getByValue(credentialType).get()) {
            CredentialType.DEMO_ID_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_government, null)
            CredentialType.DEMO_DEGREE_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_university, null)
            CredentialType.DEMO_EMPLOYMENT_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_proof, null)
            CredentialType.DEMO_INSURANCE_CREDENTIAL -> context.resources.getDrawable(R.drawable.ic_id_insurance, null)
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE, CredentialType.GEORGIA_EDUCATIONAL_DEGREE -> context.resources.getDrawable(R.mipmap.ic_educational_degree, null)
            CredentialType.ETHIOPIA_NATIONAL_ID, CredentialType.GEORGIA_NATIONAL_ID -> context.resources.getDrawable(R.mipmap.ic_national_id, null)
            CredentialType.ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT, CredentialType.GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT -> context.resources.getDrawable(R.mipmap.ic_educational_degree_transcript, null)
            else -> null
        }
    }

    /*
    * Logic to get the HTML String
    * */
    suspend fun getHtmlString(credentialData: CredentialWithEncodedCredential): String? = withContext(Dispatchers.Main) {
        if (CredentialMapper.isADemoCredential(credentialData.credential)) {
            return@withContext getHtmlStringFromDemoCredential(credentialData.encodedCredential)
        } else {
            return@withContext getHtmlStringFromPlainTextCredential(credentialData.encodedCredential)
        }
    }

    private fun getHtmlStringFromDemoCredential(encodedCredential: EncodedCredential): String? {
        try {
            val credentialProto = io.iohk.atala.prism.protos.Credential.parseFrom(encodedCredential.credentialEncoded)
            val jsonObject = JSONObject(credentialProto.credentialDocument)
            val viewObject = jsonObject.getJSONObject("view")
            return viewObject.getString("html")
        } catch (ex: InvalidProtocolBufferException) {
            ex.printStackTrace()
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
        return null
    }

    private fun getHtmlStringFromPlainTextCredential(encodedCredential: EncodedCredential): String? {
        try {
            val atalaMessage = AtalaMessage.parseFrom(encodedCredential.credentialEncoded)
            val plainTextCredential = atalaMessage.plainCredential
            val encodedCredential = plainTextCredential.encodedCredentialBytes.toStringUtf8()
            val plainTextCredentialJson = CredentialMapper.parsePlainTextCredential(encodedCredential)
            return Html.fromHtml(plainTextCredentialJson.html, Html.FROM_HTML_MODE_COMPACT).toString()
        } catch (ex: InvalidProtocolBufferException) {
            ex.printStackTrace()
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
        return null
    }
}
