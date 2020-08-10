package io.iohk.atala.crypto.japi;

import java.math.BigInteger;

public interface ECPoint {
    BigInteger getX();
    BigInteger getY();
}
