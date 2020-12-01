package io.iohk.atala.prism.app;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import io.iohk.atala.prism.crypto.japi.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import io.iohk.atala.prism.crypto.MnemonicException;
import io.iohk.atala.prism.identity.japi.DID;
import io.iohk.atala.prism.identity.japi.DIDFactory;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4ClassRunner.class)
public class CryptoTest {

    private String stringData;
    private byte[] randomBytes;
    private byte[] randomBytes2;
    private EC ec;


    @Before
    public void setUp() throws NoSuchAlgorithmException {
        stringData = "StringData";
        randomBytes = new byte[20];
        randomBytes2 = new byte[20];
        ec = EC.getInstance(CryptoProvider.Android);
        SecureRandom.getInstanceStrong().nextBytes(randomBytes);
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
    public void checkDataIntegrityCorrectPublicKey() {
        ECKeyPair keyPair = ec.generateKeyPair();

        ECSignature signedEC = ec.sign(randomBytes, keyPair.getPrivate());

        boolean result = ec.verify(randomBytes, keyPair.getPublic(), signedEC);
        assertTrue(result);
    }

    @Test
    public void checkDataIntegrityWrongPublicKey() {
        ECKeyPair keyPair = ec.generateKeyPair();

        ECSignature signedEC = ec.sign(randomBytes, keyPair.getPrivate());
        ECKeyPair keyPair2 = ec.generateKeyPair();

        boolean result = ec.verify(randomBytes, keyPair2.getPublic(), signedEC);
        assertFalse(result);
    }

    @Test
    public void checkDataIntegrityWrongData() throws Exception {
        SecureRandom.getInstanceStrong().nextBytes(randomBytes2);

        ECKeyPair keyPair = ec.generateKeyPair();
        ECSignature signedEC = ec.sign(randomBytes, keyPair.getPrivate());

        boolean result = ec.verify(randomBytes2, keyPair.getPublic(), signedEC);
        assertFalse(result);
    }

    @Test
    public void checkDid() {
        ECKeyPair keyPair = ec.generateKeyPair();
        DID did = DIDFactory.createUnpublishedDID(keyPair.getPublic());

        assertTrue(did.isLongForm());
        assertFalse(did.isCanonicalForm());
        assertTrue(did.getSuffix().isPresent());
        assertTrue(did.getCanonicalSuffix().isPresent());
        assertEquals("did:prism:" + did.stripPrismPrefix(), did.getValue());
    }

    @Test
    public void checkMnemonics() throws MnemonicException {
        KeyDerivation keyDerivation = KeyDerivation.getInstance(CryptoProvider.Android);
        DerivationPath derivationPath = DerivationPath.parse("m/0'/1'/0'");
        MnemonicCode mnemonicCode = keyDerivation.randomMnemonicCode();
        byte[] seed = keyDerivation.binarySeed(mnemonicCode, "");
        keyDerivation.deriveKey(seed, derivationPath).getKeyPair();
    }

    @Test
    public void checkKeyEncoding() {
        ECKeyPair keyPair = ec.generateKeyPair();
        ECPublicKey publicKey = keyPair.getPublic();

        byte[] encodedPublicKey = publicKey.getEncoded();
        ECPublicKey decodedPublicKey = ec.toPublicKey(encodedPublicKey);
        assertEquals(publicKey.getHexEncoded(), decodedPublicKey.getHexEncoded());
    }
}

