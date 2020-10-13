package io.iohk.atala.credentials.japi;

public class Base64URLCredential {
    private final String value;

    public Base64URLCredential(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
