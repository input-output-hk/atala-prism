package io.iohk.atala.prism.crypto.japi;

public interface ECSignature {
    String getHexEncoded();

    byte[] getData();
}
