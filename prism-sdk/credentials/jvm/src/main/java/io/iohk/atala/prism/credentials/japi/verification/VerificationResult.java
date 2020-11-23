package io.iohk.atala.prism.credentials.japi.verification;

import io.iohk.atala.prism.credentials.japi.verification.error.VerificationException;

import java.util.List;

public class VerificationResult {
    private final boolean isValid;
    private final List<VerificationException> verificationExceptions;

    public VerificationResult(List<VerificationException> verificationExceptions) {
        this.isValid = verificationExceptions.isEmpty();
        this.verificationExceptions = verificationExceptions;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<VerificationException> getVerificationExceptions() {
        return verificationExceptions;
    }
}
