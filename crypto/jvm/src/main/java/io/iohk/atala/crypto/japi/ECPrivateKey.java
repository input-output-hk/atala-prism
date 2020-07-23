package io.iohk.atala.crypto.japi;

import java.math.BigInteger;

public interface ECPrivateKey {
    byte[] getEncoded();
    String getHexEncoded();
    BigInteger getD();
}
