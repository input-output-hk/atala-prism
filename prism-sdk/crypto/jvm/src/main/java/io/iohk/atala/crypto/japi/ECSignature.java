package io.iohk.atala.crypto.japi;

public interface ECSignature {
    String getHexEncoded();

    byte[] getData();
}
