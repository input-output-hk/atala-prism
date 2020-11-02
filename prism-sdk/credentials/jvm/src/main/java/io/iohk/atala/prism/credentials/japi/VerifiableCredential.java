package io.iohk.atala.prism.credentials.japi;

import io.iohk.atala.prism.crypto.japi.EC;
import io.iohk.atala.prism.crypto.japi.ECPrivateKey;
import io.iohk.atala.prism.crypto.japi.ECPublicKey;

public interface VerifiableCredential extends Credential {
    String getCanonicalForm();

    boolean isSigned();
    boolean isUnverifiable();

    byte[] getHash();
    CredentialSignature getSignature();

    Credential sign(ECPrivateKey privateKey, EC ec);
    boolean verifySignature(ECPublicKey publicKey, EC ec);
}
