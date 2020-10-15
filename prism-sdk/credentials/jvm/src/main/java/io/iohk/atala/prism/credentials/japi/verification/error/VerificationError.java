package io.iohk.atala.prism.credentials.japi.verification.error;

public interface VerificationError {
    enum ErrorCode {
        Revoked,
        KeyWasNotValid,
        KeyWasRevoked,
        InvalidSignature
    }

    public ErrorCode getCode();
}
