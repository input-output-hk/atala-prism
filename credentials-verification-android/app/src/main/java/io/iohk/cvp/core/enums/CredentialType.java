package io.iohk.cvp.core.enums;

public enum CredentialType {

    REDLAND_CREDENTIAL(1L, "VerifiableCredential/RedlandIdCredential"),

    DEGREE_CREDENTIAL(2L, "VerifiableCredential/AirsideDegreeCredential"),

    EMPLOYMENT_CREDENTIAL(3L, "VerifiableCredential/CertificateOfEmployment"),

    INSURANCE_CREDENTIAL(4L, "VerifiableCredential/CertificateOfInsurance");

    private String value;

    private Long id;

    CredentialType(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public Long getId() {
        return id;
    }
}
