package io.iohk.atala.prism.crypto.japi;

import java.math.BigInteger;

public interface ECPoint {
    BigInteger getX();
    BigInteger getY();
}
