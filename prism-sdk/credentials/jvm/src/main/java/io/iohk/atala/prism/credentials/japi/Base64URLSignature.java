package io.iohk.atala.prism.credentials.japi;

public class Base64URLSignature {
    private final String value;

    public Base64URLSignature(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
