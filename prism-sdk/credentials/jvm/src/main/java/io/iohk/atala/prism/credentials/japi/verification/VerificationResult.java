package io.iohk.atala.prism.credentials.japi.verification;

import io.iohk.atala.prism.credentials.japi.verification.error.VerificationError;

import java.util.List;

public class VerificationResult {
    private final boolean isValid;
    private final List<VerificationError> errors;

    public VerificationResult(List<VerificationError> errors) {
        this.isValid = errors.isEmpty();
        this.errors = errors;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<VerificationError> getErrors() {
        return errors;
    }
}
