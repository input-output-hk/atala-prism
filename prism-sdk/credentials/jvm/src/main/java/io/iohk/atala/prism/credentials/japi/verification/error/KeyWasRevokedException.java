package io.iohk.atala.prism.credentials.japi.verification.error;

import io.iohk.atala.prism.credentials.japi.TimestampInfo;

public class KeyWasRevokedException extends VerificationException {

    private final TimestampInfo credentialIssuedOn;
    private final TimestampInfo keyRevokedOn;

    public KeyWasRevokedException(TimestampInfo credentialIssuedOn, TimestampInfo keyRevokedOn) {
        this.credentialIssuedOn = credentialIssuedOn;
        this.keyRevokedOn = keyRevokedOn;
    }

    public TimestampInfo getCredentialIssuedOn() {
        return credentialIssuedOn;
    }

    public TimestampInfo getKeyRevokedOn() {
        return keyRevokedOn;
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.KeyWasRevoked;
    }
}
