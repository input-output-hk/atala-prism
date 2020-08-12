package io.iohk.cvp.views.fragments;

import android.content.Context;

import java.util.Optional;

import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import io.iohk.cvp.data.local.db.model.Credential;

public class CredentialUtil {
    public static String getType(Credential acceptedCredential, Context context) {
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(acceptedCredential.credentialType);
        if(credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get().getId()) {
                case 1:
                    return context.getResources().getString(R.string.credential_government_type_title);
                case 2:
                    return context.getResources().getString(R.string.credential_degree_type_title);
                case 3:
                    return context.getResources().getString(R.string.credential_employed_type_title);
                case 4:
                    return context.getResources().getString(R.string.credential_insurance_type_title);
                default:
                    return "";
            }
        }
        return "";
    }

    public static String getTitle(Credential credential, Context context){
        Optional<CredentialType> credentialTypeOptional = CredentialType.getByValue(credential.credentialType);
        if(credentialTypeOptional.isPresent()) {
            switch (credentialTypeOptional.get().getId()) {
                case 1:
                    return context.getResources().getString(R.string.credential_detail_government_title);
                case 2:
                    return context.getResources().getString(R.string.credential_detail_degree_title);
                case 3:
                    return context.getResources().getString(R.string.credential_detail_employed_title);
                case 4:
                    return context.getResources().getString(R.string.credential_detail_insurance_title);
                default:
                    return "";
            }
        }
        return "";
    }

}
