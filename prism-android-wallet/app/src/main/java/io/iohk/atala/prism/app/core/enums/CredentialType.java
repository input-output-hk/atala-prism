package io.iohk.atala.prism.app.core.enums;

import java.util.Optional;

public enum CredentialType {

    DEMO_ID_CREDENTIAL(1, "VerifiableCredential/RedlandIdCredential"),
    DEMO_DEGREE_CREDENTIAL(2, "VerifiableCredential/AirsideDegreeCredential"),
    DEMO_EMPLOYMENT_CREDENTIAL(3, "VerifiableCredential/AtalaEmploymentCredential"),
    DEMO_INSURANCE_CREDENTIAL(4, "VerifiableCredential/AtalaCertificateOfInsurance"),
    GEORGIA_EDUCATIONAL_DEGREE(5, "GeorgiaEducationalDegree"),
    GEORGIA_EDUCATIONAL_DEGREE_TRANSCRIPT(6, "GeorgiaEducationalDegreeTranscript"),
    GEORGIA_NATIONAL_ID(7, "GeorgiaNationalID"),
    ETHIOPIA_EDUCATIONAL_DEGREE(8, "EthiopiaEducationalDegree"),
    ETHIOPIA_EDUCATIONAL_DEGREE_TRANSCRIPT(9, "EthiopiaEducationalDegreeTranscript"),
    ETHIOPIA_NATIONAL_ID(10, "EthiopiaNationalID");

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