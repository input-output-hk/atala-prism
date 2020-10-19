package io.iohk.atala;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.SecureRandom;

import io.iohk.atala.prism.crypto.japi.CryptoProvider;
import io.iohk.atala.prism.crypto.japi.EC;
import io.iohk.atala.prism.crypto.japi.ECKeyPair;
import io.iohk.atala.prism.crypto.japi.ECSignature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class CryptoTest {

  private String stringData ;
  private byte[] randomBytes;
  private byte[] randomBytes2;
  private EC ec;


  @Before
  public void setUp(){
    stringData = "StringData";
    randomBytes = new byte[20];
    randomBytes2 = new byte[20];
    ec = EC.getInstance(CryptoProvider.Android);
  }

  @Test
  public void checkStringIntegrityCorrect() {
    ECKeyPair keyPair = ec.generateKeyPair();

    ECSignature signedEC = ec.sign(stringData, keyPair.getPrivate());

    boolean result = ec.verify(stringData, keyPair.getPublic(), signedEC);
    assertTrue(result);
  }

  @Test
  public void checkStringIntegrityWrong() {
    ECKeyPair keyPair = ec.generateKeyPair();

    ECSignature signedEC = ec.sign(stringData, keyPair.getPrivate());

    boolean result = ec.verify("OtherStringData", keyPair.getPublic(), signedEC);
    assertFalse(result);
  }

  @Test
  public void checkDataIntegrityCorrectPublicKey() throws Exception {

    SecureRandom.getInstanceStrong().nextBytes(randomBytes);

    ECKeyPair keyPair = ec.generateKeyPair();

    ECSignature signedEC = ec.sign(randomBytes, keyPair.getPrivate());

    boolean result = ec.verify(randomBytes, keyPair.getPublic(), signedEC);
    assertTrue(result);
  }

  @Test
  public void checkDataIntegrityWrongPublicKey() throws Exception {

    SecureRandom.getInstanceStrong().nextBytes(randomBytes);

    ECKeyPair keyPair = ec.generateKeyPair();

    ECSignature signedEC = ec.sign(randomBytes, keyPair.getPrivate());
    ECKeyPair keyPair2 = ec.generateKeyPair();

    boolean result = ec.verify(randomBytes, keyPair2.getPublic(), signedEC);
    assertFalse(result);
  }

  @Test
  public void checkDataIntegrityWrongData() throws Exception {

    SecureRandom.getInstanceStrong().nextBytes(randomBytes);

    SecureRandom.getInstanceStrong().nextBytes(randomBytes2);

    ECKeyPair keyPair = ec.generateKeyPair();
    ECSignature signedEC = ec.sign(randomBytes, keyPair.getPrivate());

    boolean result = ec.verify(randomBytes2, keyPair.getPublic(), signedEC);
    assertFalse(result);
  }
}

