package io.iohk.atala.prism.credentials.japi.verification.error;

import io.iohk.atala.prism.credentials.japi.TimestampInfo;

public class KeyWasNotValid implements VerificationError {

    private final TimestampInfo keyAddedOn;
    private final TimestampInfo credentialIssuedOn;

    public KeyWasNotValid(TimestampInfo keyAddedOn, TimestampInfo credentialIssuedOn) {
        this.keyAddedOn = keyAddedOn;
        this.credentialIssuedOn = credentialIssuedOn;
    }

    public TimestampInfo getKeyAddedOn() {
        return this.keyAddedOn;
    }

    public TimestampInfo getCredentialIssuedOn() {
        return this.credentialIssuedOn;
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.KeyWasNotValid;
    }
}
