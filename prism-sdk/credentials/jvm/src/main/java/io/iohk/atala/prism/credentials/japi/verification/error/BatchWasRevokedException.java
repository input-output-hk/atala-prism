package io.iohk.atala.prism.credentials.japi.verification.error;

import io.iohk.atala.prism.credentials.japi.TimestampInfo;

public class BatchWasRevokedException extends VerificationException {

    private final TimestampInfo batchRevokedOn;

    public BatchWasRevokedException(TimestampInfo batchRevokedOn) {
        this.batchRevokedOn = batchRevokedOn;
    }

    public TimestampInfo getBatchRevokedOn() {
        return batchRevokedOn;
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.BatchWasRevoked;
    }
}
