package io.iohk.atala.crypto.japi;

public interface ECPublicKey {
    ECPoint getCurvePoint();
    byte[] getEncoded();
    String getHexEncoded();
}
