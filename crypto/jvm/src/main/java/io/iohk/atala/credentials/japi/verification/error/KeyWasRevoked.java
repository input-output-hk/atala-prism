package io.iohk.atala.credentials.japi.verification.error;

import io.iohk.atala.credentials.japi.TimestampInfo;

public class KeyWasRevoked implements VerificationError {

    private final TimestampInfo credentialIssuedOn;
    private final TimestampInfo keyRevokedOn;

    public KeyWasRevoked(TimestampInfo credentialIssuedOn, TimestampInfo keyRevokedOn) {
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
