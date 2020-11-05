package io.iohk.atala.prism.app.views.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;

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
                case ID_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_government_type_title);
                case DEGREE_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_degree_type_title);
                case EMPLOYMENT_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_employed_type_title);
                case INSURANCE_CREDENTIAL:
                    return context.getResources().getString(R.string.credential_insurance_type_title);
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
                case ID_CREDENTIAL:
                    return R.string.credential_government_name;
                case DEGREE_CREDENTIAL:
                    return R.string.credential_degree_name;
                case EMPLOYMENT_CREDENTIAL:
                    return R.string.credential_employment_name;
                case INSURANCE_CREDENTIAL:
                    return R.string.credential_insurance_name;
                default:
                    return 0;
            }
        }
        return 0;
    }

    public static Drawable getLogo(String credentialType, Context context) {
        switch (CredentialType.getByValue(credentialType).get()) {
            case ID_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_government, null);
            case DEGREE_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_university, null);
            case EMPLOYMENT_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_proof, null);
            case INSURANCE_CREDENTIAL:
                return context.getResources().getDrawable(R.drawable.ic_id_insurance, null);
            default:
                return null;
        }
    }

}