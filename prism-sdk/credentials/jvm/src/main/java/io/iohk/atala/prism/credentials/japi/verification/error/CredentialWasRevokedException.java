package io.iohk.atala.prism.credentials.japi.verification.error;


import io.iohk.atala.prism.credentials.japi.TimestampInfo;

public class CredentialWasRevokedException extends VerificationException {

    private final TimestampInfo revokedOn;

    public CredentialWasRevokedException(TimestampInfo revokedOn) {
        this.revokedOn = revokedOn;
    }

    public TimestampInfo getRevokedOn() {
        return revokedOn;
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.Revoked;
    }
}
