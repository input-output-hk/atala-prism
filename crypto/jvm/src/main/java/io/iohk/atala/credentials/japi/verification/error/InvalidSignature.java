package io.iohk.atala.credentials.japi.verification.error;

import io.iohk.atala.crypto.ECPublicKey;

public class InvalidSignature implements VerificationError {

    @Override
    public ErrorCode getCode() {
        return ErrorCode.InvalidSignature;
    }
}
