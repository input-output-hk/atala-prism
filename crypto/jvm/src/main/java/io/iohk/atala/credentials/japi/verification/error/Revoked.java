package io.iohk.atala.credentials.japi.verification.error;


import io.iohk.atala.credentials.japi.TimestampInfo;

public class Revoked implements VerificationError {

    private final TimestampInfo revokedOn;

    public Revoked(TimestampInfo revokedOn) {
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
