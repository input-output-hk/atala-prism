package io.iohk.cvp;

import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;

import io.iohk.atala.crypto.EC;
import io.iohk.atala.crypto.ECKeyPair;
import io.iohk.atala.crypto.ECSignature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CryptoTest {

  private String stringData = "StringData";
  private String otherStringData = "OtherStringData";
  private byte[] randomBytes = new byte[20];
  private byte[] randomBytes2 = new byte[20];

  @Before
  public void init() {
    SecureRandom random = new SecureRandom();
    random.nextBytes(randomBytes);
    random.nextBytes(randomBytes2);
  }

  @Test
  public void checkStringIntegrityCorrect() {
    ECKeyPair keyPair = EC.generateKeyPair();

    ECSignature signedEC = EC.sign(stringData, keyPair.privateKey());

    boolean result = EC.verify(stringData, keyPair.publicKey(), signedEC);
    assertTrue(result);
  }

  @Test
  public void checkStringIntegrityWrong() {
    ECKeyPair keyPair = EC.generateKeyPair();

    ECSignature signedEC = EC.sign(stringData, keyPair.privateKey());

    boolean result = EC.verify(otherStringData, keyPair.publicKey(), signedEC);
    assertFalse(result);
  }

  @Test
  public void checkDataIntegrityCorrectPublicKey() throws Exception {

    ECKeyPair keyPair = EC.generateKeyPair();

    ECSignature signedEC = EC.sign(randomBytes, keyPair.privateKey());

    boolean result = EC.verify(randomBytes, keyPair.publicKey(), signedEC);
    assertTrue(result);
  }

  @Test
  public void checkDataIntegrityWrongPublicKey() throws Exception {

    ECKeyPair keyPair = EC.generateKeyPair();

    ECSignature signedEC = EC.sign(randomBytes, keyPair.privateKey());
    ECKeyPair keyPair2 = EC.generateKeyPair();

    boolean result = EC.verify(randomBytes, keyPair2.publicKey(), signedEC);
    assertFalse(result);
  }

  @Test
  public void checkDataIntegrityWrongData() throws Exception {

    ECKeyPair keyPair = EC.generateKeyPair();
    ECSignature signedEC = EC.sign(randomBytes, keyPair.privateKey());

    boolean result = EC.verify(randomBytes2, keyPair.publicKey(), signedEC);
    assertFalse(result);
  }
}
