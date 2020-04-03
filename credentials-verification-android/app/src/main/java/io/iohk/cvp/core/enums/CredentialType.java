package io.iohk.cvp.core.enums;

public enum CredentialType {

    REDLAND_CREDENTIAL(1, "VerifiableCredential/RedlandIdCredential"),
    DEGREE_CREDENTIAL(2, "VerifiableCredential/AirsideDegreeCredential"),
    EMPLOYMENT_CREDENTIAL(3, "VerifiableCredential/CertificateOfEmployment"),
    INSURANCE_CREDENTIAL(4, "VerifiableCredential/CertificateOfInsurance");

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
}
