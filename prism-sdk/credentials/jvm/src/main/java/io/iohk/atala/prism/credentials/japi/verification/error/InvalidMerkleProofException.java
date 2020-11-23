package io.iohk.atala.prism.credentials.japi.verification.error;

public class InvalidMerkleProofException extends VerificationException {
    @Override
    public ErrorCode getCode() {
        return ErrorCode.InvalidMerkleProof;
    }
}
