package io.iohk.atala.prism.crypto.japi;

public interface ECKeyPair {
    ECPublicKey getPublic();
    ECPrivateKey getPrivate();
}
