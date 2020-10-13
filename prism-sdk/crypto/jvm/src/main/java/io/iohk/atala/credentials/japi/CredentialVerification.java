package io.iohk.atala.credentials.japi;

import io.iohk.atala.credentials.japi.verification.VerificationResult;
import io.iohk.atala.crypto.AndroidEC$;
import io.iohk.atala.crypto.EC$;

import java.util.Arrays;

public interface CredentialVerification {
  enum Provider {
    JVM,
    ANDROID
  }

  static CredentialVerification getInstance(Provider provider) {
    switch (provider) {
      case JVM:
        return new CredentialVerificationFacade(EC$.MODULE$);
      case ANDROID:
        return new CredentialVerificationFacade(AndroidEC$.MODULE$);
      default:
        throw new IllegalArgumentException(
            String.format("Unexpected provider %s, available types are %s",
                provider, Arrays.toString(Provider.values())));
    }
  }

  VerificationResult verifyCredential(KeyData keyData, CredentialData credentialData, SignedCredential signedCredential);
}
