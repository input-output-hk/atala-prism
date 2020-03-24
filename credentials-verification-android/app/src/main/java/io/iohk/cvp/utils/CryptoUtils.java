package io.iohk.cvp.utils;

import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.crypto.ECKeys;
import io.iohk.cvp.io.wallet.ECPrivateKey;
import io.iohk.cvp.io.wallet.ECPublicKey;
import io.iohk.cvp.views.Preferences;
import io.iohk.prism.protos.ConnectorPublicKey;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class CryptoUtils {

  public static ConnectorPublicKey getPublicKey(Preferences prefs)
      throws InvalidKeySpecException, CryptoException, SharedPrefencesDataNotFoundException {
    ECKeys crypto = new ECKeys();
    byte[] pkBytes = prefs.getPrivateKey();
    ECPrivateKey pk = crypto.toPrivateKey(pkBytes);
    PublicKey publicKey = crypto.toPublicKey(pk.getD().toByteArray());
    ECPublicKey pubKey = crypto.getPublicKey(publicKey);

    return ConnectorPublicKey.newBuilder().setX("1234")
            .setY("1234").build();

   /* return io.iohk.cvp.io.connector.PublicKey.newBuilder().setX(pubKey.getX().getValue())
        .setY(pubKey.getY().getValue()).build();*/
  }
}
