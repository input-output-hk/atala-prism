package io.iohk.atala.crypto.japi;

public interface ECKeyPair {
    ECPublicKey getPublic();
    ECPrivateKey getPrivate();
}
