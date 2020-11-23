package io.iohk.atala.prism.credentials.japi.verification.error;

public class InvalidSignatureException extends VerificationException {

    @Override
    public ErrorCode getCode() {
        return ErrorCode.InvalidSignature;
    }
}
