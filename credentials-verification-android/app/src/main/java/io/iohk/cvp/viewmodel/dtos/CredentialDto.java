package io.iohk.cvp.viewmodel.dtos;

import java.util.List;

import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import lombok.Data;
import lombok.Getter;

@Data
public class CredentialDto {

    @Getter
    private String id;

    @Getter
    private String credentialType;

    @Getter
    private List<String> type;

    @Getter
    private CredentialSubject credentialSubject;

    @Getter
    private String expiryDate;

    @Getter
    private String employmentStatus;

    @Getter
    private String issuanceDate;

    @Getter
    private String productClass;

    @Getter
    private String policyNumber;

    @Getter
    private CredentialIssuer issuer;

    public int getTitle() {
        if (credentialType.equals(CredentialType.REDLAND_CREDENTIAL.getValue())) {
            return R.string.credential_detail_government_title;
        } else if (credentialType.equals(CredentialType.DEGREE_CREDENTIAL.getValue())) {
            return R.string.credential_detail_degree_title;
        } else if (credentialType.equals(CredentialType.EMPLOYMENT_CREDENTIAL.getValue())) {
            return R.string.credential_detail_employed_title;
        } else {
            return R.string.credential_detail_insurance_title;
        }
    }
}
