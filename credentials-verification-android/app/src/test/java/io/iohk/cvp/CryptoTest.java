package io.iohk.cvp;

import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import io.iohk.atala.prism.crypto.EC;
import io.iohk.atala.prism.crypto.ECKeyPair;
import io.iohk.atala.prism.crypto.ECSignature;
import io.iohk.atala.prism.crypto.MnemonicChecksumException;
import io.iohk.atala.prism.crypto.MnemonicLengthException;
import io.iohk.atala.prism.crypto.MnemonicWordException;
import io.iohk.cvp.utils.CryptoUtils;

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

  @Test
  public void isValidMnemonic() throws Exception {
    List<String> phrases =  CryptoUtils.Companion.generateMnemonicList();

    assertTrue(CryptoUtils.Companion.isValidMnemonicList(phrases));
  }

  @Test(expected = MnemonicWordException.class)
  public void isInvalidWordMnemonic() throws Exception {
    List<String> phrases =  Arrays.asList("already", "ankle", "announce", "annual", "another",
            "answer", "antenna", "antique", "anxiety", "any", "apart", "badword");

    CryptoUtils.Companion.isValidMnemonicList(phrases);
  }

  @Test(expected = MnemonicLengthException.class)
  public void isInvalidLength() throws Exception {
    List<String> phrases =  Arrays.asList("abandon", "announce");
    CryptoUtils.Companion.isValidMnemonicList(phrases);
  }

  @Test(expected = MnemonicWordException.class)
  public void hasInvalidWord() throws Exception {

    List<String> phrases =  Arrays.asList("hocus", "pocus", "mnemo", "codus", "annual", "another",
            "answer", "antenna", "antique", "anxiety", "any", "apart");
    CryptoUtils.Companion.isValidMnemonicList(phrases);
  }

  @Test(expected = MnemonicChecksumException.class)
  public void isInvalidChecksum() throws Exception {
    List<String> phrases =  Arrays.asList("already", "ankle", "announce", "annual", "another",
            "answer", "antenna", "antique", "anxiety", "any", "apart", "apology");
    CryptoUtils.Companion.isValidMnemonicList(phrases);
  }

}
