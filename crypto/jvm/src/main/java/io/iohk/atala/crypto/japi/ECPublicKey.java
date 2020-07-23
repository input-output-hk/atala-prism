package io.iohk.atala.crypto.japi;

public interface ECPublicKey {
    byte[] getEncoded();
    String getHexEncoded();
}
