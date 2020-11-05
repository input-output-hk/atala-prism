package io.iohk.atala.prism.app.core.enums;

import java.util.Optional;

public enum CredentialType {

    ID_CREDENTIAL(1, "VerifiableCredential/RedlandIdCredential"),
    DEGREE_CREDENTIAL(2, "VerifiableCredential/AirsideDegreeCredential"),
    EMPLOYMENT_CREDENTIAL(3, "VerifiableCredential/AtalaEmploymentCredential"),
    INSURANCE_CREDENTIAL(4, "VerifiableCredential/AtalaCertificateOfInsurance");

    private String value;

    private int id;

    CredentialType(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getId() {
        return id;
    }

    public static Optional<CredentialType> getByValue(String value) {
        for (CredentialType c : CredentialType.values()) {
            if (c.value.equalsIgnoreCase(value)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}