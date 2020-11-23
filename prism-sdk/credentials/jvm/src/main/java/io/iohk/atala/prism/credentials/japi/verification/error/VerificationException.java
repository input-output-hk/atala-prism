package io.iohk.atala.prism.credentials.japi.verification.error;

public abstract class VerificationException extends Exception {
    public enum ErrorCode {
        Revoked,
        KeyWasNotValid,
        KeyWasRevoked,
        InvalidSignature,
        BatchWasRevoked,
        InvalidMerkleProof
    }

    public abstract ErrorCode getCode();
}
