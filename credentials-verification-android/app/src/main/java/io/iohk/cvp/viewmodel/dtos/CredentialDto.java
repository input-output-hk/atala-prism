package io.iohk.cvp.viewmodel.dtos;

import java.util.List;

import io.iohk.cvp.R;
import io.iohk.cvp.core.enums.CredentialType;
import lombok.Data;

@Data
public class CredentialDto {

    private String id;
    private String credentialType;
    private List<String> type;
    private CredentialSubject credentialSubject;
    private String expiryDate;
    private String issuanceDate;
    private CredentialIssuer issuer;

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public CredentialSubject getCredentialSubject() {
        return credentialSubject;
    }

    public void setCredentialSubject(CredentialSubject credentialSubject) {
        this.credentialSubject = credentialSubject;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getIssuanceDate() {
        return issuanceDate;
    }

    public void setIssuanceDate(String issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    public CredentialIssuer getIssuer() {
        return issuer;
    }

    public void setIssuer(CredentialIssuer issuer) {
        this.issuer = issuer;
    }

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
