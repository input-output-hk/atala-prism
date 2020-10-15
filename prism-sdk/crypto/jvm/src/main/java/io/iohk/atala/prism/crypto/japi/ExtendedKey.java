package io.iohk.atala.prism.crypto.japi;

public interface ExtendedKey {
    /**
     * Derivation path used to obtain such key */
    DerivationPath getPath();

    /**
     * Public key for this extended key */
    ECPublicKey getPublic();

    /**
     * Private key for this extended key */
    ECPrivateKey getPrivate();

    /**
     * KeyPair for this extended key */
    ECKeyPair getKeyPair();

    /**
     * Generates child extended key for given index */
    ExtendedKey derive(DerivationAxis axis);
}
