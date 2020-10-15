package io.iohk.atala.prism.crypto.japi;

import java.math.BigInteger;

public interface ECPrivateKey {
    byte[] getEncoded();
    String getHexEncoded();
    BigInteger getD();
}
