package io.iohk.atala.prism.app;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.google.common.primitives.Bytes;
import io.iohk.atala.prism.kotlin.crypto.EC;
import io.iohk.atala.prism.kotlin.crypto.derivation.*;
import io.iohk.atala.prism.kotlin.crypto.keys.*;
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import io.iohk.atala.prism.kotlin.identity.DID;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4ClassRunner.class)
public class CryptoTest {

    private String stringData;
    private byte[] randomBytes;
    private byte[] randomBytes2;


    @Before
    public void setUp() throws NoSuchAlgorithmException {
        stringData = "StringData";
        randomBytes = new byte[20];
        randomBytes2 = new byte[20];
        SecureRandom.getInstanceStrong().nextBytes(randomBytes);
    }

    @Test
    public void checkStringIntegrityCorrect() {
        ECKeyPair keyPair = EC.generateKeyPair();

        ECSignature signedEC = EC.sign(stringData, keyPair.getPrivateKey());

        boolean result = EC.verify(stringData, keyPair.getPublicKey(), signedEC);
        assertTrue(result);
    }

    @Test
    public void checkStringIntegrityWrong() {
        ECKeyPair keyPair = EC.generateKeyPair();

        ECSignature signedEC = EC.sign(stringData, keyPair.getPrivateKey());

        boolean result = EC.verify("OtherStringData", keyPair.getPublicKey(), signedEC);
        assertFalse(result);
    }

    @Test
    public void checkDataIntegrityCorrectPublicKey() {
        ECKeyPair keyPair = EC.generateKeyPair();

        ECSignature signedEC = EC.sign(Bytes.asList(randomBytes), keyPair.getPrivateKey());

        boolean result = EC.verify(Bytes.asList(randomBytes), keyPair.getPublicKey(), signedEC);
        assertTrue(result);
    }

    @Test
    public void checkDataIntegrityWrongPublicKey() {
        ECKeyPair keyPair = EC.generateKeyPair();

        ECSignature signedEC = EC.sign(Bytes.asList(randomBytes), keyPair.getPrivateKey());
        ECKeyPair keyPair2 = EC.generateKeyPair();

        boolean result = EC.verify(Bytes.asList(randomBytes), keyPair2.getPublicKey(), signedEC);
        assertFalse(result);
    }

    @Test
    public void checkDataIntegrityWrongData() throws Exception {
        SecureRandom.getInstanceStrong().nextBytes(randomBytes2);

        ECKeyPair keyPair = EC.generateKeyPair();
        ECSignature signedEC = EC.sign(Bytes.asList(randomBytes), keyPair.getPrivateKey());

        boolean result = EC.verify(Bytes.asList(randomBytes2), keyPair.getPublicKey(), signedEC);
        assertFalse(result);
    }

    @Test
    public void checkDid() {
        ECKeyPair keyPair = EC.generateKeyPair();
        DID did = DID.createUnpublishedDID(keyPair.getPublicKey());

        assertTrue(did.isLongForm());
        assertFalse(did.isCanonicalForm());
        did.getSuffix();
        did.getCanonicalSuffix();
        assertEquals("did:prism:" + did.stripPrismPrefix(), did.getValue());
    }

    @Test
    public void checkMnemonics() throws MnemonicException {
        JvmKeyDerivation keyDerivation = JvmKeyDerivation.INSTANCE;
        DerivationPath derivationPath = DerivationPath.fromPath("m/0'/1'/0'");
        MnemonicCode mnemonicCode = keyDerivation.randomMnemonicCode();
        List<Byte> seed = keyDerivation.binarySeed(mnemonicCode, "");
        keyDerivation.deriveKey(seed, derivationPath).keyPair();
    }

    @Test
    public void checkKeyEncoding() {
        ECKeyPair keyPair = EC.generateKeyPair();
        ECPublicKey publicKey = keyPair.getPublicKey();

        List<Byte> encodedPublicKey = publicKey.getEncoded();
        ECPublicKey decodedPublicKey = EC.toPublicKey(encodedPublicKey);
        assertEquals(publicKey.getHexEncoded(), decodedPublicKey.getHexEncoded());
    }
}

