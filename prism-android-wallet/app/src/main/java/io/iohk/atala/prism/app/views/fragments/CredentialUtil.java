package io.iohk.atala.prism.app.views.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

import io.iohk.cvp.R;
import io.iohk.atala.prism.app.core.enums.CredentialType;
import io.iohk.atala.prism.app.data.local.db.model.Credential;

/**
 * TODO this is just to help handle legacy code which is used to locally compute the name and icon of the credentials based on the type of credential
 * when there is a proper way to get that data from the backend this should be removed,
 */
public class CredentialUtil {
    public static String getType(Credential acceptedCredential, Context context) {
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(acceptedCredential.credentialType);
        if (credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get()) {
                case DEMO_ID_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_government_type_title);
                case DEMO_DEGREE_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_degree_type_title);
                case DEMO_EMPLOYMENT_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_employed_type_title);
                case DEMO_INSURANCE_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_insurance_type_title);
                case GEORGIA_NATIONAL_ID:
                    return context.getResources().getString(R.string.credential_georgia_national_id_type_title);
                case GEORGIA_EDUCATIONAL_DEGREE:
                    return context.getResources().getString(R.string.credential_georgia_educational_degree_type_title);
                case GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT:
                    return context.getResources().getString(R.string.credential_georgia_educational_degree_transcript_type_title);
                default:
                    return "";
            }
        }
        return "";
    }

    public static String getName(Credential credential, Context context) {
        return context.getResources().getString(getNameResource(credential));
    }

    public static Integer getNameResource(Credential credential) {
        return getNameResource(credential.credentialType);
    }

    public static Integer getNameResource(String credentialType) {
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(credentialType);
        if (credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get()) {
                case DEMO_ID_CREDENTIAL:
                    return R.string.credential_government_name;
                case DEMO_DEGREE_CREDENTIAL:
                    return R.string.credential_degree_name;
                case DEMO_EMPLOYMENT_CREDENTIAL:
                    return R.string.credential_employment_name;
                case DEMO_INSURANCE_CREDENTIAL:
                    return R.string.credential_insurance_name;
                case GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT:
                    return R.string.credential_georgia_educational_degree_transcript_name;
                case GEORGIA_EDUCATIONAL_DEGREE:
                    return R.string.credential_georgia_educational_degree_name;
                case GEORGIA_NATIONAL_ID:
                    return R.string.credential_georgia_national_id_name;
                default:
                    return 0;
            }
        }
        return 0;
    }

    public static Drawable getLogo(String credentialType, Context context) {
        switch (CredentialType.getByValue(credentialType).get()) {
            case DEMO_ID_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_government, null);
            case DEMO_DEGREE_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_university, null);
            case DEMO_EMPLOYMENT_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_proof, null);
            case DEMO_INSURANCE_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_insurance, null);
            case GEORGIA_EDUCATIONAL_DEGREE:
                return context.getResources().getDrawable(R.mipmap.ic_educational_degree, null);
            case GEORGIA_NATIONAL_ID:
                return context.getResources().getDrawable(R.mipmap.ic_national_id, null);
            case GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT:
                return context.getResources().getDrawable(R.mipmap.ic_educational_degree_transcript, null);
            default:
                return null;
        }
    }

    public static String getHtml(Credential credential) {
        try {
            if (isADemoCredential(credential)) {
                return getHtmlDemoSupport(credential);
            }
            return Html.fromHtml(credential.htmlView, Html.FROM_HTML_MODE_COMPACT).toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static String getHtmlDemoSupport(Credential credential) throws JSONException {
        JSONObject jsonObject = new JSONObject(credential.credentialDocument);
        JSONObject viewObj = jsonObject.getJSONObject("view");
        return viewObj.getString("html");
    }

    public static Boolean isADemoCredential(Credential credential) {
        CredentialType credentialType = CredentialType.getByValue(credential.credentialType).get();
        return credentialType.equals(CredentialType.DEMO_EMPLOYMENT_CREDENTIAL)
                || credentialType.equals(CredentialType.DEMO_DEGREE_CREDENTIAL)
                || credentialType.equals(CredentialType.DEMO_ID_CREDENTIAL)
                || credentialType.equals(CredentialType.DEMO_INSURANCE_CREDENTIAL);
    }
}