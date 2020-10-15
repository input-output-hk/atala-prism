package io.iohk.atala.prism.crypto.japi;

public interface ECPublicKey {
    ECPoint getCurvePoint();
    byte[] getEncoded();
    String getHexEncoded();
}
