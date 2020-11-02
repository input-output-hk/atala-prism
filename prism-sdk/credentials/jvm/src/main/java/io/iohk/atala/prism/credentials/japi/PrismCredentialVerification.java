package io.iohk.atala.prism.credentials.japi;

import io.iohk.atala.prism.credentials.japi.verification.VerificationResult;
import io.iohk.atala.prism.crypto.japi.EC;

public class PrismCredentialVerification {
    public static VerificationResult verify(KeyData keyData, CredentialData credentialData, Credential credential, EC ec) {
        return PrismCredentialVerificationFacade$.MODULE$.verify(keyData, credentialData, credential, ec);
    }
}
