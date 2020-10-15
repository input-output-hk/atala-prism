package io.iohk.atala.prism.credentials.japi.verification.error;

public class InvalidSignature implements VerificationError {

    @Override
    public ErrorCode getCode() {
        return ErrorCode.InvalidSignature;
    }
}
