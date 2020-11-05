package io.iohk.atala.prism.app.viewmodel.dtos;

import java.util.List;

import io.iohk.cvp.R;
import io.iohk.atala.prism.app.core.enums.CredentialType;
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
    private CredentialView view;

    @Getter
    private CredentialIssuer issuer;
}