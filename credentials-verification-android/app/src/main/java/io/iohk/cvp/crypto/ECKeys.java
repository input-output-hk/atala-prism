package io.iohk.cvp.crypto;

import static io.iohk.cvp.core.exception.ErrorCode.CRYPTO_UNKNOWN_PRIVATE_KEY_TYPE;
import static io.iohk.cvp.core.exception.ErrorCode.CRYPTO_UNKNOWN_PUBLIC_KEY_TYPE;

import com.crashlytics.android.Crashlytics;
import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.io.wallet.BigInteger;
import io.iohk.cvp.io.wallet.ECPrivateKey;
import io.iohk.cvp.io.wallet.ECPublicKey;
import io.iohk.cvp.io.wallet.KeyPair;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveSpec;

public class ECKeys {

  private static final String CURVE_NAME = "secp256k1";
  private static KeyFactory keyFactory = null;

  static {
    Security.addProvider(new BouncyCastleProvider());
    try {
      keyFactory = KeyFactory.getInstance("ECDSA");
    } catch (NoSuchAlgorithmException e) {
      Crashlytics.logException(
          new CryptoException("Couldn't initialize key factory. " + e.getMessage(),
              ErrorCode.CRYPTO_KEY_FACTORY_NOT_INITIALIZED));
    }
  }

  private ECNamedCurveParameterSpec ecParameterSpec = ECNamedCurveTable
      .getParameterSpec(CURVE_NAME);

  private ECNamedCurveSpec ecNamedCurveSpec = new ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
  );

  public KeyPair getKeyPair(byte[] seed) throws InvalidKeySpecException, CryptoException {
    ECPrivateKey pk = toPrivateKey(seed);
    ECPublicKey pubK = getPublicKey(toPublicKey(pk.getD()));

    return KeyPair.newBuilder().setPublicKey(pubK).setPrivateKey(pk).build();
  }

  public ECPrivateKey toPrivateKey(byte[] d) {
    return ECPrivateKey.newBuilder().setD(BigInteger.newBuilder().setValue(toBigInt(d).toString()))
        .build();
  }

  private PrivateKey toPrivateKey(java.math.BigInteger d) throws InvalidKeySpecException {
    KeySpec spec = toPrivateKeySpec(d);
    return keyFactory.generatePrivate
        (spec);
  }

  public PublicKey toPublicKey(byte[] x, byte[] y) throws InvalidKeySpecException {
    BigInteger walletX = BigInteger.newBuilder().setValue(toBigInt(x).toString()).build();
    BigInteger walletY = BigInteger.newBuilder().setValue(toBigInt(y).toString()).build();
    KeySpec spec = toPublicKeySpec(walletX, walletY);
    return keyFactory.generatePublic(spec);
  }

  public PublicKey toPublicKey(BigInteger x, BigInteger y) throws InvalidKeySpecException {
    KeySpec spec = toPublicKeySpec(x, y);
    return keyFactory.generatePublic(spec);
  }

  public PublicKey toPublicKey(byte[] d) throws InvalidKeySpecException {
    BigInteger walletD = BigInteger.newBuilder().setValue(toBigInt(d).toString()).build();
    return toPublicKey(walletD);
  }

  private PublicKey toPublicKey(BigInteger d) throws InvalidKeySpecException {
    org.spongycastle.math.ec.ECPoint Q = ecParameterSpec.getG()
        .multiply(new java.math.BigInteger(d.getValue()));
    org.spongycastle.jce.spec.ECPublicKeySpec pubSpec = new org.spongycastle.jce.spec.ECPublicKeySpec(
        Q, ecParameterSpec);
    return keyFactory.generatePublic(pubSpec);
  }

  private ECPoint getECPoint(PublicKey publicKey) throws CryptoException {
    if (publicKey instanceof BCECPublicKey) {
      return ((BCECPublicKey) publicKey).getW();
    }
    throw new CryptoException("Unknown public key type", CRYPTO_UNKNOWN_PUBLIC_KEY_TYPE);
  }

  private BigInteger getD(PrivateKey privateKey) throws CryptoException {
    if (privateKey instanceof BCECPrivateKey) {
      return BigInteger.newBuilder().setValue(((BCECPrivateKey) privateKey).getD().toString())
          .build();
    }
    throw new CryptoException("Unknown private key type", CRYPTO_UNKNOWN_PRIVATE_KEY_TYPE);
  }

  /**
   * It looks like the coordinates on secp256k1 are always possitive and keys are encoded without
   * the byte sign that Java uses for encoding/decoding a big integer.
   */
  private java.math.BigInteger toBigInt(byte[] bytes) {
    return new java.math.BigInteger(1, bytes);
  }

  private ECPrivateKeySpec toPrivateKeySpec(java.math.BigInteger d) {
    return new ECPrivateKeySpec(d, ecNamedCurveSpec);
  }

  private java.security.spec.ECPublicKeySpec toPublicKeySpec(BigInteger x,
      BigInteger y) {
    ECPoint ecPoint = new ECPoint(new java.math.BigInteger(x.getValue()),
        new java.math.BigInteger(y.getValue()));

    return new java.security.spec.ECPublicKeySpec(ecPoint, ecNamedCurveSpec);
  }

  private ECPrivateKey getPrivateKey(PrivateKey pk) throws CryptoException {
    return ECPrivateKey.newBuilder().setD(this.getD(pk)).build();
  }

  public ECPublicKey getPublicKey(PublicKey pubKey) throws CryptoException {
    ECPoint ecPoint = this.getECPoint(pubKey);
    return ECPublicKey.newBuilder()
        .setX(BigInteger.newBuilder().setValue(ecPoint.getAffineX().toString()))
        .setY(BigInteger.newBuilder().setValue(ecPoint.getAffineY().toString())).build();
  }

}
