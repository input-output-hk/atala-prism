package io.iohk.atala.prism.credentials.japi;

public class SignedCredential {
    private final Base64URLCredential credential;
    private final Base64URLSignature signature;

    public SignedCredential(Base64URLCredential credential, Base64URLSignature signature) {
        this.credential = credential;
        this.signature = signature;
    }

    public Base64URLCredential getCredential() {
        return credential;
    }

    public Base64URLSignature getSignature() {
        return signature;
    }
}
