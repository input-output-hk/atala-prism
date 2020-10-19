package io.iohk.atala.prism.app.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

public class KeyStoreUtils {

  public static final String KEY_ALIAS_PRIVATE_KEY = "pk_encryption";

  private static final String AndroidKeyStore = "AndroidKeyStore";
  private static final String AES_MODE = "AES/GCM/NoPadding";
  private static final byte[] IV = new byte[]{-14, 24, 13, -41, 53, 1, 8, 10, -25, 10, 15, -30};

  private KeyStore keyStore;


  public KeyStoreUtils() {
    try {
      keyStore = KeyStore.getInstance(AndroidKeyStore);
      keyStore.load(null);
    } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public void generateKey() {
    try {
      if (!keyStore.containsAlias(KEY_ALIAS_PRIVATE_KEY)) {
        KeyGenerator keyGenerator = KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
        keyGenerator.init(getKeySpec());
        keyGenerator.generateKey();
      }
    } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException | InvalidAlgorithmParameterException e) {
      e.printStackTrace();

    }
  }

  public String encryptData(byte[] key) {
    String encryptedBase64Encoded = null;
    try {
      Cipher c = Cipher.getInstance(AES_MODE);
      c.init(Cipher.ENCRYPT_MODE, getSecretKey(),
          new GCMParameterSpec(128, IV));
      byte[] encodedBytes = c.doFinal(key);
      encryptedBase64Encoded = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    } catch (InvalidAlgorithmParameterException | InvalidKeyException |
        UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
        | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
      e.printStackTrace();
    }

    return encryptedBase64Encoded;

  }

  public byte[] decryptData(byte[] encrypted) {
    byte[] decodedBytes = null;
    try {
      Cipher c = Cipher.getInstance(AES_MODE);
      c.init(Cipher.DECRYPT_MODE, getSecretKey(),
          new GCMParameterSpec(128, IV));
      decodedBytes = c.doFinal(encrypted);

    } catch (InvalidAlgorithmParameterException | InvalidKeyException | UnrecoverableKeyException
        | NoSuchAlgorithmException | KeyStoreException | NoSuchPaddingException
        | BadPaddingException | IllegalBlockSizeException e) {
      e.printStackTrace();
    }
    return decodedBytes;
  }

  private Key getSecretKey()
      throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
    return keyStore.getKey(KEY_ALIAS_PRIVATE_KEY, null);
  }

  private KeyGenParameterSpec getKeySpec() {
    return new KeyGenParameterSpec.Builder(KEY_ALIAS_PRIVATE_KEY,
        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(false)
        .build();
  }

}
